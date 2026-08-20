package br.com.antigravity.fiscalapi;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.antigravity.fiscalapi.company.CompanyService;
import br.com.antigravity.fiscalapi.company.CreateCompanyRequest;
import br.com.antigravity.fiscalapi.company.FiscalEnvironment;
import br.com.antigravity.fiscalapi.company.TaxRegime;
import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.antigravity.fiscalapi.document.DocumentStatus;
import br.com.antigravity.fiscalapi.document.FiscalDocumentService;
import br.com.antigravity.fiscalapi.document.FiscalItemRequest;
import br.com.antigravity.fiscalapi.document.IssueDocumentRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Reemissao da mesma referencia externa sobre um documento que ainda nao foi
 * autorizado.
 *
 * A UF entra como indisponivel de proposito: e o unico jeito de o documento
 * parar em contingencia e o caminho de reemissao ser exercitado. Com a SEFAZ
 * respondendo, o stub autoriza na primeira tentativa e nao sobra o que retomar.
 */
@SpringBootTest(properties = "app.fiscal.unavailable-states=BA")
class DocumentReissueTests {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private FiscalDocumentService fiscalDocumentService;

    @Test
    void reissueWithNewDataReplacesTheContentStillPending() {
        var company = companyService.create(new CreateCompanyRequest(
            "tenant-substituicao",
            "merchant-substituicao",
            "https://integrator.example/webhooks/fiscal",
            "Empresa Substituicao LTDA",
            "92345678000180",
            "923456780",
            "BA",
            "Empresa Substituicao",
            "Rua Fiscal",
            "201",
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
            null,
            null,
            1,
            1L,
            1,
            1L
        ));

        var firstIssue = fiscalDocumentService.issue(new IssueDocumentRequest(
            company.id(),
            null,
            null,
            DocumentModel.NFCE,
            "PEDIDO-SUBSTITUIDO-1",
            "Cliente Antigo",
            BigDecimal.valueOf(90),
            List.of(item("A", "Produto A", BigDecimal.valueOf(90)))
        ));

        // A premissa do teste. Se isto virar AUTHORIZED, o resto nao prova nada.
        assertThat(firstIssue.status()).isNotEqualTo(DocumentStatus.AUTHORIZED);

        var secondIssue = fiscalDocumentService.issue(new IssueDocumentRequest(
            company.id(),
            null,
            null,
            DocumentModel.NFCE,
            "PEDIDO-SUBSTITUIDO-1",
            "Cliente Novo",
            BigDecimal.valueOf(150),
            List.of(item("B", "Produto B", BigDecimal.valueOf(150)))
        ));

        // Mesmo documento e mesma numeracao — a nota e uma so —, mas valendo o
        // conteudo que acabou de chegar, e nao o da primeira tentativa.
        assertThat(secondIssue.id()).isEqualTo(firstIssue.id());
        assertThat(secondIssue.seriesNumber()).isEqualTo(firstIssue.seriesNumber());
        assertThat(secondIssue.invoiceNumber()).isEqualTo(firstIssue.invoiceNumber());
        assertThat(secondIssue.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(150));
        assertThat(secondIssue.customerName()).isEqualTo("Cliente Novo");
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
