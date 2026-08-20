package br.com.antigravity.fiscalapi.fiscal;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.antigravity.fiscalapi.document.FiscalItemRequest;
import br.com.swconsultoria.nfe.schemas.TNFe;
import br.com.swconsultoria.nfe.schemas.TTribNFe;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * IBS e CBS, da reforma tributaria.
 *
 * O CST e a classificacao tributaria definem o tratamento da venda, e o schema
 * aceita qualquer numero no formato — nao ha padrao que sirva para toda
 * empresa. Por isso vem do cadastro, decididos pelo contador.
 *
 * A ausencia deles omite o grupo, de proposito. Inventar conteudo produziria
 * documento que a SEFAZ autoriza e que entra errado na escrituracao; sem o
 * grupo vem a rejeicao 1115, que e visivel e corrigivel.
 */
class IbsCbsTests {

    private final FiscalXmlBuilder builder = new FiscalXmlBuilder();
    private final JavaNfeMapper mapper = new JavaNfeMapper();

    @Test
    void comOsCodigosCadastradosOGrupoVaiNoXml() {
        var ibsCbs = ibsCbsDoPrimeiroItem("000", "000001");

        assertThat(ibsCbs).isPresent();
        assertThat(ibsCbs.get().getCST()).isEqualTo("000");
        assertThat(ibsCbs.get().getCClassTrib()).isEqualTo("000001");
    }

    @Test
    void semOsCodigosOGrupoNaoVai() {
        assertThat(ibsCbsDoPrimeiroItem(null, null)).isEmpty();
    }

    /**
     * Meio preenchido e o caso traicoeiro: o grupo iria ao ar com um dos dois
     * campos obrigatorios em branco. Melhor omitir e levar a 1115.
     */
    @Test
    void comApenasUmDosCodigosOGrupoNaoVai() {
        assertThat(ibsCbsDoPrimeiroItem("000", null)).isEmpty();
        assertThat(ibsCbsDoPrimeiroItem(null, "000001")).isEmpty();
        assertThat(ibsCbsDoPrimeiroItem("000", "  ")).isEmpty();
    }

    private Optional<TTribNFe> ibsCbsDoPrimeiroItem(String cst, String classTrib) {
        TNFe.InfNFe.Det.Imposto imposto = mapper.toEnviNFe(builder.build(new FiscalSubmission(
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
            cst,
            classTrib,
            "2",
            OffsetDateTime.now(),
            1,
            8L,
            "PEDIDO-IBSCBS-1",
            BigDecimal.valueOf(15.00),
            "Cliente Teste",
            List.of(new FiscalItemRequest(
                "PROD-001", "Produto A", "01012100", null, "SEM GTIN", "5102", "UN",
                BigDecimal.ONE, BigDecimal.valueOf(15.00), BigDecimal.valueOf(15.00),
                "0", "102", "49", "49", BigDecimal.ZERO
            ))
        ))).getNFe().get(0).getInfNFe().getDet().get(0).getImposto();

        return imposto.getContent().stream()
            .map(conteudo -> conteudo instanceof javax.xml.bind.JAXBElement<?> elemento
                ? elemento.getValue()
                : conteudo)
            .filter(TTribNFe.class::isInstance)
            .map(TTribNFe.class::cast)
            .findFirst();
    }
}
