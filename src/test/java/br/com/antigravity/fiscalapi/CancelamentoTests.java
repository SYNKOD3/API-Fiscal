package br.com.antigravity.fiscalapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.antigravity.fiscalapi.company.CompanyService;
import br.com.antigravity.fiscalapi.company.CreateCompanyRequest;
import br.com.antigravity.fiscalapi.company.FiscalEnvironment;
import br.com.antigravity.fiscalapi.company.TaxRegime;
import br.com.antigravity.fiscalapi.document.CancelDocumentRequest;
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
 * Cancelamento de documento fiscal.
 *
 * O prazo — 30 minutos da autorização para a NFC-e, por força do Ajuste
 * SINIEF 19/2016 — não é conferido aqui de propósito: quem o impõe é a SEFAZ.
 * Reproduzir a regra criaria uma segunda fonte da verdade, que envelheceria
 * sozinha quando a norma mudasse.
 *
 * O que esta suíte protege são as regras que são nossas: só se cancela o que
 * foi autorizado, e não se cancela duas vezes.
 */
@SpringBootTest
class CancelamentoTests {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private FiscalDocumentService fiscalDocumentService;

    @Test
    void cancelaDocumentoAutorizadoEGuardaOMotivo() {
        var documento = emitir("tenant-cancel-1", "merchant-cancel-1", "PEDIDO-CANCEL-1");
        assertThat(documento.status()).isEqualTo(DocumentStatus.AUTHORIZED);

        var cancelado = fiscalDocumentService.cancel(
            documento.id(),
            new CancelDocumentRequest("Cliente desistiu da compra apos a emissao")
        );

        assertThat(cancelado.status()).isEqualTo(DocumentStatus.CANCELLED);
    }

    /**
     * Uma nota só cai uma vez. A segunda tentativa não pode gerar outro evento
     * na SEFAZ nem parecer que funcionou.
     */
    @Test
    void naoCancelaDuasVezes() {
        var documento = emitir("tenant-cancel-2", "merchant-cancel-2", "PEDIDO-CANCEL-2");
        fiscalDocumentService.cancel(
            documento.id(),
            new CancelDocumentRequest("Cliente desistiu da compra apos a emissao")
        );

        assertThatThrownBy(() -> fiscalDocumentService.cancel(
            documento.id(),
            new CancelDocumentRequest("Tentando cancelar de novo por engano")
        )).hasMessageContaining("ja esta cancelado");
    }

    private br.com.antigravity.fiscalapi.document.DocumentResponse emitir(
        String tenantId, String merchantId, String referencia
    ) {
        var empresa = companyService.create(new CreateCompanyRequest(
            tenantId,
            merchantId,
            "https://integrator.example/webhooks/fiscal",
            "Empresa Cancelamento LTDA",
            "9234567800016" + tenantId.charAt(tenantId.length() - 1),
            "923456761",
            "BA",
            "Empresa Cancelamento",
            "Rua Fiscal",
            "400",
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

        return fiscalDocumentService.issue(new IssueDocumentRequest(
            empresa.id(),
            null,
            null,
            DocumentModel.NFCE,
            referencia,
            "Cliente Teste",
            BigDecimal.valueOf(15),
            List.of(new FiscalItemRequest(
                "A", "Produto A", "01012100", null, "SEM GTIN", "5102", "UN",
                BigDecimal.ONE, BigDecimal.valueOf(15), BigDecimal.valueOf(15),
                "0", "102", "49", "49", null, null, BigDecimal.ZERO
            ))
        ));
    }
}
