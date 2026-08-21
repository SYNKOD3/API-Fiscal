package br.com.antigravity.fiscalapi.fiscal;

import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.dom.enuns.DocumentoEnum;
import br.com.swconsultoria.nfe.schemas.TEnviNFe;
import br.com.swconsultoria.nfe.schemas.TNFe;
import br.com.swconsultoria.nfe.schemas.TTribNFe;
import br.com.swconsultoria.nfe.util.IbsCbsUtil;
import java.util.Optional;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Grupo de IBS/CBS de cada item, montado pela java-nfe a partir da tabela da
 * SEFAZ.
 *
 * Informar só CST e classificação tributária não basta: a SEFAZ recusa com
 * "Grupo IBS/CBS não informado" porque falta o subgrupo de valores — base,
 * alíquotas estadual e municipal, CBS e os totais. O subgrupo é opcional no
 * XSD, então nenhuma validação local o cobra; só o retorno da SEFAZ.
 *
 * Quem monta é a biblioteca, de propósito. Ela lê a tabela oficial, deriva o
 * CST a partir da classificação tributária e escolhe o ramo certo — normal,
 * monofásico, transferência de crédito, ajuste de competência —, além de
 * aplicar redução e crédito presumido quando o código pede. Reproduzir isso
 * aqui seria manter uma cópia de regra tributária que muda por norma.
 *
 * Sem tabela ou sem classificação cadastrada, o grupo não vai e a nota é
 * recusada nomeando o que falta. É o desfecho visível; inventar valores
 * produziria nota autorizada com tributo errado.
 */
@Component
public class IbsCbsFactory {

    private static final String NFE_NAMESPACE = "http://www.portalfiscal.inf.br/nfe";
    private static final Logger log = LoggerFactory.getLogger(IbsCbsFactory.class);

    private final IbsCbsTabela tabela;

    public IbsCbsFactory(IbsCbsTabela tabela) {
        this.tabela = tabela;
    }

    public void aplicar(TEnviNFe enviNFe, FiscalXmlDraft draft, ConfiguracoesNfe config) {
        if (draft.items().stream().allMatch(item -> vazio(item.ibsCbsClassTrib()))) {
            log.warn(
                "Nenhum item tem classificacao tributaria de IBS/CBS: emissao seguira sem o grupo."
            );
            return;
        }

        Optional<String> json = tabela.json(config);
        if (json.isEmpty()) {
            return;
        }

        try {
            IbsCbsUtil util = new IbsCbsUtil(json.get(), documento(draft.model()));
            TNFe.InfNFe infNFe = enviNFe.getNFe().get(0).getInfNFe();

            for (int indice = 0; indice < infNFe.getDet().size(); indice++) {
                String classTrib = draft.items().get(indice).ibsCbsClassTrib();
                if (vazio(classTrib)) {
                    continue;
                }
                TNFe.InfNFe.Det det = infNFe.getDet().get(indice);
                det.getImposto().getContent().add(
                    elemento("IBSCBS", util.montaImpostosDet(classTrib, det))
                );
            }

            infNFe.getTotal().setIBSCBSTot(util.preencheTotaisIbsCsb());
            // O IBS e a CBS entram por fora do preço, então o total da nota
            // muda. Deixar o vNF antigo faria a soma dos grupos não fechar com
            // o total — que é conferência que a SEFAZ faz.
            infNFe.getTotal().getICMSTot().setVNF(
                util.calculaVnfTot(infNFe.getTotal().getICMSTot().getVNF()).toPlainString()
            );
        } catch (Exception ex) {
            // Mesma regra da tabela: falhar aqui recusa a nota com mensagem, e
            // isso é melhor do que derrubar a venda ou emitir valor inventado.
            log.warn("Falha ao montar o grupo de IBS/CBS: {}. A emissao segue sem ele.",
                ex.getMessage());
        }
    }

    private DocumentoEnum documento(DocumentModel model) {
        return model == DocumentModel.NFE ? DocumentoEnum.NFE : DocumentoEnum.NFCE;
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    private JAXBElement<TTribNFe> elemento(String nome, TTribNFe valor) {
        return new JAXBElement<>(new QName(NFE_NAMESPACE, nome), TTribNFe.class, valor);
    }
}
