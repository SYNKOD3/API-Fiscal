package br.com.antigravity.fiscalapi.fiscal;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.antigravity.fiscalapi.document.FiscalItemRequest;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FiscalXmlBuilderTests {

    private final FiscalXmlBuilder builder = new FiscalXmlBuilder();
    private final FiscalXmlPreviewRenderer renderer = new FiscalXmlPreviewRenderer();

    @Test
    void buildsNfceDraftWithModel65AndItems() {
        FiscalSubmission submission = new FiscalSubmission(
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
            null,
            null,
            "2",
            OffsetDateTime.parse("2026-08-13T18:00:00-03:00"),
            7,
            200L,
            "PEDIDO-1",
            BigDecimal.valueOf(199.90),
            "Cliente Teste",
            List.of(item())
        );

        FiscalXmlDraft draft = builder.build(submission);
        String xml = renderer.render(draft);

        assertThat(draft.invoiceId()).startsWith("NFe");
        assertThat(draft.accessKey()).hasSize(44);
        assertThat(draft.accessKey()).contains("12345678000199650070000002001");
        assertThat(xml).contains("<mod>65</mod>");
        assertThat(xml).contains("<tpAmb>2</tpAmb>");
        assertThat(xml).contains("<chNFe>" + draft.accessKey() + "</chNFe>");
        assertThat(xml).contains("<NCM>01012100</NCM>");
        assertThat(xml).contains("<vNF>199.90</vNF>");
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
            null,
            null,
            BigDecimal.ZERO
        );
    }
}
