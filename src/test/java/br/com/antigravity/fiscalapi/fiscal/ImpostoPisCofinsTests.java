package br.com.antigravity.fiscalapi.fiscal;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.antigravity.fiscalapi.document.FiscalItemRequest;
import br.com.swconsultoria.nfe.schemas.TNFe;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * O CST decide em qual grupo o PIS e a COFINS entram.
 *
 * PISNT/COFINSNT sao "nao tributado" e o schema so aceita 04 a 09 la dentro.
 * O 49 — que e o que o Simples Nacional usa, e o que a Bivaro manda — e
 * "outras operacoes", e mora em PISOutr/COFINSOutr, com base, aliquota e
 * valor. Mandar 49 dentro de NT fazia a SEFAZ recusar o lote inteiro com
 * "225 - Falha no Schema XML", que nao nomeia o campo.
 */
class ImpostoPisCofinsTests {

    private final FiscalXmlBuilder builder = new FiscalXmlBuilder();
    private final JavaNfeMapper mapper = new JavaNfeMapper();

    @Test
    void cst49VaiParaOGrupoDeOutrasOperacoes() {
        var imposto = impostoDoPrimeiroItem("49", "49");

        var pis = grupo(imposto, TNFe.InfNFe.Det.Imposto.PIS.class);
        assertThat(pis.getPISNT()).as("49 nao cabe em nao tributado").isNull();
        assertThat(pis.getPISOutr()).isNotNull();
        assertThat(pis.getPISOutr().getCST()).isEqualTo("49");
        assertThat(pis.getPISOutr().getVBC()).isEqualTo("0.00");
        assertThat(pis.getPISOutr().getPPIS()).isEqualTo("0.0000");
        assertThat(pis.getPISOutr().getVPIS()).isEqualTo("0.00");

        var cofins = grupo(imposto, TNFe.InfNFe.Det.Imposto.COFINS.class);
        assertThat(cofins.getCOFINSNT()).isNull();
        assertThat(cofins.getCOFINSOutr()).isNotNull();
        assertThat(cofins.getCOFINSOutr().getCST()).isEqualTo("49");
    }

    @Test
    void cstDeNaoTributadoContinuaNoGrupoNT() {
        var imposto = impostoDoPrimeiroItem("07", "07");

        var pis = grupo(imposto, TNFe.InfNFe.Det.Imposto.PIS.class);
        assertThat(pis.getPISOutr()).isNull();
        assertThat(pis.getPISNT()).isNotNull();
        assertThat(pis.getPISNT().getCST()).isEqualTo("07");

        var cofins = grupo(imposto, TNFe.InfNFe.Det.Imposto.COFINS.class);
        assertThat(cofins.getCOFINSOutr()).isNull();
        assertThat(cofins.getCOFINSNT()).isNotNull();
        assertThat(cofins.getCOFINSNT().getCST()).isEqualTo("07");
    }

    private <T> T grupo(TNFe.InfNFe.Det.Imposto imposto, Class<T> tipo) {
        return imposto.getContent().stream()
            .map(conteudo -> conteudo instanceof javax.xml.bind.JAXBElement<?> elemento
                ? elemento.getValue()
                : conteudo)
            .filter(tipo::isInstance)
            .map(tipo::cast)
            .findFirst()
            .orElseThrow(() -> new AssertionError("grupo ausente: " + tipo.getSimpleName()));
    }

    private TNFe.InfNFe.Det.Imposto impostoDoPrimeiroItem(String pisCode, String cofinsCode) {
        return mapper.toEnviNFe(builder.build(new FiscalSubmission(
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
            null,
            null,
            "2",
            OffsetDateTime.now(),
            1,
            8L,
            "PEDIDO-CST-1",
            BigDecimal.valueOf(15.00),
            "Cliente Teste",
            List.of(new FiscalItemRequest(
                "PROD-001", "Produto A", "01012100", null, "SEM GTIN", "5102", "UN",
                BigDecimal.ONE, BigDecimal.valueOf(15.00), BigDecimal.valueOf(15.00),
                "0", "102", pisCode, cofinsCode, BigDecimal.ZERO
            ))
        ))).getNFe().get(0).getInfNFe().getDet().get(0).getImposto();
    }
}
