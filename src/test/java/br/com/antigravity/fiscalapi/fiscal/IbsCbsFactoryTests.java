package br.com.antigravity.fiscalapi.fiscal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.antigravity.fiscalapi.document.FiscalItemRequest;
import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.schemas.TEnviNFe;
import br.com.swconsultoria.nfe.schemas.TTribNFe;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Montagem do grupo de IBS/CBS.
 *
 * O que estes testes protegem não é o cálculo — quem calcula é a java-nfe, a
 * partir da tabela oficial. É o que acontece quando a tabela **não** vem: uma
 * consulta à SEFAZ no caminho da emissão não pode derrubar a venda do balcão.
 * A nota sai sem o grupo e é recusada nomeando o que falta, que é visível e
 * corrigível; uma exceção subindo pararia a loja.
 *
 * Aqui a tabela nunca vem, de propósito: sem certificado a consulta falha, e é
 * exatamente o cenário que interessa exercitar sem depender de rede.
 */
class IbsCbsFactoryTests {

    private final FiscalXmlBuilder builder = new FiscalXmlBuilder();
    private final JavaNfeMapper mapper = new JavaNfeMapper();
    private final IbsCbsFactory factory = new IbsCbsFactory(new IbsCbsTabela());

    @Test
    void semTabelaAEmissaoSegueSemGrupoEmVezDeQuebrar() {
        var enviNFe = comIbsCbs("000001");

        assertThatCode(() -> factory.aplicar(enviNFe, draft("000001"), new ConfiguracoesNfe()))
            .as("a consulta à SEFAZ não pode derrubar a venda")
            .doesNotThrowAnyException();

        assertThat(grupoIbsCbs(enviNFe))
            .as("sem tabela o grupo não vai, e a SEFAZ recusa nomeando o que falta")
            .isNull();
    }

    @Test
    void semClassificacaoNemTentaConsultarATabela() {
        var enviNFe = comIbsCbs(null);

        assertThatCode(() -> factory.aplicar(enviNFe, draft(null), new ConfiguracoesNfe()))
            .doesNotThrowAnyException();

        assertThat(grupoIbsCbs(enviNFe)).isNull();
    }

    @Test
    void oTotalDaNotaFicaIntactoQuandoOGrupoNaoEMontado() {
        var enviNFe = comIbsCbs("000001");
        String antes = enviNFe.getNFe().get(0).getInfNFe().getTotal().getICMSTot().getVNF();

        factory.aplicar(enviNFe, draft("000001"), new ConfiguracoesNfe());

        assertThat(enviNFe.getNFe().get(0).getInfNFe().getTotal().getICMSTot().getVNF())
            .as("sem grupo não há tributo a somar; mexer no total aqui desencontraria a nota")
            .isEqualTo(antes);
    }

    private TTribNFe grupoIbsCbs(TEnviNFe enviNFe) {
        return enviNFe.getNFe().get(0).getInfNFe().getDet().get(0).getImposto().getContent().stream()
            .map(conteudo -> conteudo instanceof javax.xml.bind.JAXBElement<?> elemento
                ? elemento.getValue()
                : conteudo)
            .filter(TTribNFe.class::isInstance)
            .map(TTribNFe.class::cast)
            .findFirst()
            .orElse(null);
    }

    private TEnviNFe comIbsCbs(String classTrib) {
        return mapper.toEnviNFe(draft(classTrib));
    }

    private FiscalXmlDraft draft(String classTrib) {
        return builder.build(new FiscalSubmission(
            UUID.randomUUID(),
            DocumentModel.NFCE,
            "Empresa Fiscal Teste LTDA",
            "Empresa Teste",
            "55092719000191",
            "SP",
            "123456789",
            "Rua Fiscal",
            "100",
            null,
            "Centro",
            "3550308",
            "Sao Paulo",
            "01000000",
            "1133334444",
            "1",
            classTrib == null ? null : "000",
            classTrib,
            "2",
            OffsetDateTime.now(),
            1,
            8L,
            "PEDIDO-IBSCBS-FACTORY",
            BigDecimal.valueOf(15.00),
            "Cliente Teste",
            List.of(new FiscalItemRequest(
                "PROD-001", "Produto A", "01012100", null, "SEM GTIN", "5102", "UN",
                BigDecimal.ONE, BigDecimal.valueOf(15.00), BigDecimal.valueOf(15.00),
                "0", "102", "49", "49", null, null, BigDecimal.ZERO
            ))
        ));
    }
}
