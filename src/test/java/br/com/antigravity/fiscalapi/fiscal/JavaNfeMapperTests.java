package br.com.antigravity.fiscalapi.fiscal;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.antigravity.fiscalapi.document.FiscalItemRequest;
import br.com.swconsultoria.nfe.schemas.TEnviNFe;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JavaNfeMapperTests {

    private final FiscalXmlBuilder builder = new FiscalXmlBuilder();
    private final JavaNfeMapper mapper = new JavaNfeMapper();

    @Test
    void mapsDraftToJavaNfeEnvelope() {
        FiscalXmlDraft draft = builder.build(new FiscalSubmission(
            UUID.randomUUID(),
            DocumentModel.NFCE,
            "Empresa Fiscal Teste LTDA",
            "Empresa Teste",
            "12345678000199",
            "BA",
            "123456789",
            "Rua Fiscal",
            "100",
            null,
            "Centro",
            "2927408",
            "Salvador",
            "40000000",
            "7133334444",
            "1",
            "2",
            OffsetDateTime.parse("2026-08-13T18:00:00-03:00"),
            7,
            200L,
            "PEDIDO-1",
            BigDecimal.valueOf(199.90),
            "Cliente Teste",
            List.of(item())
        ));

        TEnviNFe enviNFe = mapper.toEnviNFe(draft);

        assertThat(enviNFe.getVersao()).isEqualTo("4.00");
        assertThat(enviNFe.getNFe()).hasSize(1);
        var infNFe = enviNFe.getNFe().get(0).getInfNFe();
        assertThat(infNFe.getIde().getMod()).isEqualTo("65");
        assertThat(infNFe.getIde().getCUF()).isEqualTo("29");
        assertThat(infNFe.getId()).isEqualTo("NFe" + draft.accessKey());
        assertThat(infNFe.getIde().getCNF()).isEqualTo(draft.numericCode());
        assertThat(infNFe.getIde().getTpAmb()).isEqualTo("2");
        assertThat(infNFe.getEmit().getXNome()).isEqualTo("Empresa Fiscal Teste LTDA");
        assertThat(infNFe.getEmit().getEnderEmit().getCMun()).isEqualTo("2927408");
        assertThat(infNFe.getEmit().getCRT()).isEqualTo("1");
        assertThat(infNFe.getDet()).hasSize(1);
        assertThat(infNFe.getDet().get(0).getProd().getNCM()).isEqualTo("01012100");
        assertThat(infNFe.getTotal().getICMSTot().getVNF()).isEqualTo("199.90");
    }

    private FiscalItemRequest item() {
        return new FiscalItemRequest(
            "PROD-001",
            "Produto A",
            "01012100",
            null,
            "SEM GTIN",
            "5102",
            "UN",
            BigDecimal.ONE,
            BigDecimal.valueOf(199.90),
            BigDecimal.valueOf(199.90),
            "0",
            "102",
            "49",
            "49",
            BigDecimal.ZERO
        );
    }
}
