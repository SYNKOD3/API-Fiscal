package br.com.antigravity.fiscalapi;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.antigravity.fiscalapi.audit.FiscalAuditEventResponse;
import br.com.antigravity.fiscalapi.audit.FiscalAuditService;
import br.com.antigravity.fiscalapi.company.CreateCompanyRequest;
import br.com.antigravity.fiscalapi.company.FiscalEnvironment;
import br.com.antigravity.fiscalapi.company.CompanyService;
import br.com.antigravity.fiscalapi.company.TaxRegime;
import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.antigravity.fiscalapi.document.FiscalItemRequest;
import br.com.antigravity.fiscalapi.document.IssueDocumentRequest;
import br.com.antigravity.fiscalapi.document.FiscalDocumentService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FiscalApiApplicationTests {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private FiscalDocumentService fiscalDocumentService;

    @Autowired
    private FiscalAuditService fiscalAuditService;

    @Test
    void contextLoads() {
        assertThat(companyService).isNotNull();
        assertThat(fiscalDocumentService).isNotNull();
        assertThat(fiscalAuditService).isNotNull();
    }

    @Test
    void allocatesIndependentNumberSequencesByDocumentModel() {
        var company = companyService.create(new CreateCompanyRequest(
            "Empresa Fiscal Teste LTDA",
            "12345678000199",
            "123456789",
            "BA",
            "Empresa Teste",
            "Rua Fiscal",
            "100",
            null,
            "Centro",
            "2927408",
            "Salvador",
            "40000000",
            "7133334444",
            TaxRegime.SIMPLES_NACIONAL,
            FiscalEnvironment.HOMOLOGATION,
            null,
            null,
            "000001",
            "token-csc",
            3,
            10L,
            7,
            200L
        ));

        var firstNfce = fiscalDocumentService.issue(new IssueDocumentRequest(
            company.id(),
            DocumentModel.NFCE,
            "PEDIDO-1",
            "Cliente Teste",
            BigDecimal.valueOf(100),
            List.of(item("A", "Produto A", BigDecimal.valueOf(100)))
        ));

        var secondNfce = fiscalDocumentService.issue(new IssueDocumentRequest(
            company.id(),
            DocumentModel.NFCE,
            "PEDIDO-2",
            "Cliente Teste",
            BigDecimal.valueOf(150),
            List.of(item("B", "Produto B", BigDecimal.valueOf(150)))
        ));

        var firstNfe = fiscalDocumentService.issue(new IssueDocumentRequest(
            company.id(),
            DocumentModel.NFE,
            "PEDIDO-3",
            "Cliente Empresa",
            BigDecimal.valueOf(500),
            List.of(item("C", "Produto C", BigDecimal.valueOf(500)))
        ));

        assertThat(firstNfce.seriesNumber()).isEqualTo(7);
        assertThat(firstNfce.invoiceNumber()).isEqualTo(200);
        assertThat(fiscalDocumentService.getFiscalXml(firstNfce.id())).contains("<chNFe>" + firstNfce.accessKey() + "</chNFe>");
        assertThat(fiscalDocumentService.getReceipt(firstNfce.id())).contains("Chave de acesso: " + firstNfce.accessKey());
        assertThat(secondNfce.seriesNumber()).isEqualTo(7);
        assertThat(secondNfce.invoiceNumber()).isEqualTo(201);
        assertThat(firstNfe.seriesNumber()).isEqualTo(3);
        assertThat(firstNfe.invoiceNumber()).isEqualTo(10);
        assertThat(firstNfce.accessKey()).hasSize(44);
        assertThat(firstNfce.receiptContent()).contains("Chave de acesso: " + firstNfce.accessKey());
        assertThat(fiscalDocumentService.getPrintableDocument(firstNfce.id()))
            .contains(firstNfce.accessKey(), "Documento Fiscal", "PEDIDO-1");

        assertThat(fiscalAuditService.listByCompany(company.id()).stream().map(FiscalAuditEventResponse::eventType))
            .contains("COMPANY_CREATED", "DOCUMENT_RECEIVED", "ACCESS_KEY_CREATED", "DOCUMENT_AUTHORIZED");
        assertThat(fiscalAuditService.listByDocument(firstNfce.id()).stream().map(FiscalAuditEventResponse::eventType))
            .contains("DOCUMENT_RECEIVED", "ACCESS_KEY_CREATED", "DOCUMENT_AUTHORIZED");
    }

    private FiscalItemRequest item(String sku, String description, BigDecimal totalAmount) {
        return new FiscalItemRequest(
            sku,
            description,
            "01012100",
            null,
            "SEM GTIN",
            "5102",
            "UN",
            BigDecimal.ONE,
            totalAmount,
            totalAmount,
            "0",
            "102",
            "49",
            "49",
            BigDecimal.ZERO
        );
    }
}
