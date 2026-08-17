package br.com.antigravity.fiscalapi;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.antigravity.fiscalapi.audit.FiscalAuditEventResponse;
import br.com.antigravity.fiscalapi.audit.FiscalAuditService;
import br.com.antigravity.fiscalapi.company.CreateCompanyRequest;
import br.com.antigravity.fiscalapi.company.FiscalEnvironment;
import br.com.antigravity.fiscalapi.company.CompanyService;
import br.com.antigravity.fiscalapi.company.TaxRegime;
import br.com.antigravity.fiscalapi.config.AppProperties;
import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.antigravity.fiscalapi.document.FiscalItemRequest;
import br.com.antigravity.fiscalapi.document.IssueDocumentRequest;
import br.com.antigravity.fiscalapi.document.FiscalDocumentService;
import br.com.antigravity.fiscalapi.operational.OperationalLogLevel;
import br.com.antigravity.fiscalapi.operational.OperationalLogRecord;
import br.com.antigravity.fiscalapi.operational.OperationalLogService;
import br.com.antigravity.fiscalapi.security.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.antigravity.fiscalapi.sefaz.SefazRouter;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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

    @Autowired
    private SefazRouter sefazRouter;

    @Autowired
    private OperationalLogService operationalLogService;

    @Test
    void contextLoads() {
        assertThat(companyService).isNotNull();
        assertThat(fiscalDocumentService).isNotNull();
        assertThat(fiscalAuditService).isNotNull();
        assertThat(sefazRouter).isNotNull();
        assertThat(operationalLogService).isNotNull();
    }

    @Test
    void allocatesIndependentNumberSequencesByDocumentModel() {
        var company = companyService.create(new CreateCompanyRequest(
            "bivaro-test",
            "merchant-ba",
            "https://bivaro.example/webhooks/fiscal",
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
            null,
            null,
            DocumentModel.NFCE,
            "PEDIDO-1",
            "Cliente Teste",
            BigDecimal.valueOf(100),
            List.of(item("A", "Produto A", BigDecimal.valueOf(100)))
        ));

        var secondNfce = fiscalDocumentService.issue(new IssueDocumentRequest(
            company.id(),
            null,
            null,
            DocumentModel.NFCE,
            "PEDIDO-2",
            "Cliente Teste",
            BigDecimal.valueOf(150),
            List.of(item("B", "Produto B", BigDecimal.valueOf(150)))
        ));

        var firstNfe = fiscalDocumentService.issue(new IssueDocumentRequest(
            company.id(),
            null,
            null,
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

        var bivaroIssued = fiscalDocumentService.issue(new IssueDocumentRequest(
            null,
            "bivaro-test",
            "merchant-ba",
            DocumentModel.NFCE,
            "PEDIDO-BIVARO-1",
            "Cliente Bivaro",
            BigDecimal.valueOf(50),
            List.of(item("D", "Produto D", BigDecimal.valueOf(50)))
        ));
        assertThat(bivaroIssued.companyId()).isEqualTo(company.id());
        assertThat(bivaroIssued.invoiceNumber()).isEqualTo(202);

        var route = sefazRouter.route(companyService.getByBivaroMerchant("bivaro-test", "merchant-ba"), DocumentModel.NFCE);
        assertThat(route.stateCode()).isEqualTo("BA");
        assertThat(route.authorizerStrategy()).isEqualTo("JAVA_NFE_BY_EMITTER_UF");
        assertThat(route.available()).isTrue();
    }

    @Test
    void recordsOperationalLogsForSupportAndTroubleshooting() {
        operationalLogService.record(new OperationalLogRecord(
            "req-test-001",
            OperationalLogLevel.WARN,
            "API_REQUEST_FAILED",
            "POST",
            "/api/v1/documents",
            422,
            35L,
            null,
            null,
            "PEDIDO-ERRO",
            "Requisicao finalizada com erro: total invalido",
            "validation_error"
        ));

        assertThat(operationalLogService.list(10, OperationalLogLevel.WARN, null, null))
            .anySatisfy(log -> {
                assertThat(log.requestId()).isEqualTo("req-test-001");
                assertThat(log.eventType()).isEqualTo("API_REQUEST_FAILED");
                assertThat(log.externalReference()).isEqualTo("PEDIDO-ERRO");
            });

        assertThat(operationalLogService.getByRequestId("req-test-001").statusCode()).isEqualTo(422);
    }

    @Test
    void validatesBivaroJwtClaimsAndScopes() throws Exception {
        AppProperties properties = new AppProperties();
        properties.getSecurity().getJwt().setSecret("jwt-secret-for-tests-with-more-than-32-characters");
        properties.getSecurity().getJwt().setIssuer("bivaro");
        properties.getSecurity().getJwt().setAudience("fiscal-api");

        ObjectMapper mapper = new ObjectMapper();
        JwtTokenService tokenService = new JwtTokenService(properties, mapper);
        String token = jwt(
            mapper,
            properties.getSecurity().getJwt().getSecret(),
            """
                {
                  "iss": "bivaro",
                  "aud": "fiscal-api",
                  "sub": "bivaro-backend",
                  "jti": "jwt-test-001",
                  "bivaroTenantId": "tenant-001",
                  "bivaroMerchantId": "merchant-001",
                  "scopes": ["fiscal:documents:issue", "fiscal:certificates:write"],
                  "exp": %d
                }
                """.formatted(Instant.now().plusSeconds(300).getEpochSecond())
        );

        var principal = tokenService.validate(token);

        assertThat(principal.subject()).isEqualTo("bivaro-backend");
        assertThat(principal.bivaroTenantId()).isEqualTo("tenant-001");
        assertThat(principal.bivaroMerchantId()).isEqualTo("merchant-001");
        assertThat(principal.hasScope("fiscal:documents:issue")).isTrue();
        assertThat(principal.hasScope("fiscal:logs:read")).isFalse();
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

    private String jwt(ObjectMapper mapper, String secret, String payloadJson) throws Exception {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(mapper.writeValueAsBytes(java.util.Map.of("alg", "HS256", "typ", "JWT")));
        String payload = encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signedContent = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = encoder.encodeToString(mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8)));
        return signedContent + "." + signature;
    }
}
