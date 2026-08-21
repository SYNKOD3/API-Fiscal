package br.com.antigravity.fiscalapi.fiscal;

import br.com.swconsultoria.nfe.ConsultaTributacao;
import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Tabela oficial de CST e classificação tributária do IBS/CBS.
 *
 * A SEFAZ publica quais códigos existem e como cada um se comporta — se reduz
 * base, se gera crédito presumido, se é monofásico. É a partir dela que a
 * java-nfe monta o grupo de valores, e é por isso que buscá-la é melhor do que
 * fixar alíquotas no código: quando a transição avançar, a tabela muda sozinha
 * e a emissão acompanha.
 *
 * Guardada em memória depois da primeira busca. Ela muda em ritmo de norma, não
 * de venda, e buscá-la a cada nota colocaria a SEFAZ no caminho crítico do
 * balcão — uma indisponibilidade dela pararia a loja.
 *
 * Falha não derruba a emissão: devolve vazio e quem chama segue sem o grupo. A
 * nota é recusada com um código que nomeia o que falta, o que é visível e
 * corrigível — ao contrário de emitir com um número inventado.
 */
@Component
public class IbsCbsTabela {

    private static final Logger log = LoggerFactory.getLogger(IbsCbsTabela.class);

    private final AtomicReference<String> emMemoria = new AtomicReference<>();

    public Optional<String> json(ConfiguracoesNfe config) {
        String guardada = emMemoria.get();
        if (guardada != null) {
            return Optional.of(guardada);
        }

        try {
            String json = ConsultaTributacao.getJson(config);
            if (json == null || json.isBlank()) {
                log.warn("Tabela de IBS/CBS voltou vazia: emissao seguira sem o grupo de valores.");
                return Optional.empty();
            }
            emMemoria.set(json);
            log.info("Tabela de IBS/CBS carregada da SEFAZ.");
            return Optional.of(json);
        } catch (Exception ex) {
            // Exception larga de proposito: a busca passa por rede, certificado
            // e parsing, e nenhuma dessas falhas justifica derrubar uma venda.
            log.warn("Falha ao buscar a tabela de IBS/CBS: {}. A emissao segue sem o grupo de valores.",
                ex.getMessage());
            return Optional.empty();
        }
    }

    /// Descarta o que está em memória, para a próxima emissão buscar de novo.
    public void esquecer() {
        emMemoria.set(null);
    }
}
