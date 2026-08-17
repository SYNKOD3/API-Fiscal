package br.com.antigravity.fiscalapi.document;

import br.com.antigravity.fiscalapi.audit.FiscalAuditService;
import br.com.antigravity.fiscalapi.company.Company;
import br.com.antigravity.fiscalapi.company.CompanyRepository;
import br.com.antigravity.fiscalapi.company.FiscalEnvironment;
import br.com.antigravity.fiscalapi.fiscal.FiscalGateway;
import br.com.antigravity.fiscalapi.fiscal.FiscalGatewayException;
import br.com.antigravity.fiscalapi.fiscal.FiscalSubmission;
import br.com.antigravity.fiscalapi.fiscal.FiscalXmlBuilder;
import br.com.antigravity.fiscalapi.fiscal.FiscalXmlDraft;
import br.com.antigravity.fiscalapi.fiscal.FiscalXmlPreviewRenderer;
import br.com.antigravity.fiscalapi.security.JwtSecurityContext;
import br.com.antigravity.fiscalapi.shared.BadRequestException;
import br.com.antigravity.fiscalapi.shared.ConflictException;
import br.com.antigravity.fiscalapi.shared.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FiscalDocumentService {

    private final FiscalDocumentRepository documentRepository;
    private final CompanyRepository companyRepository;
    private final FiscalNumberAllocator fiscalNumberAllocator;
    private final FiscalGateway fiscalGateway;
    private final FiscalXmlBuilder fiscalXmlBuilder;
    private final FiscalXmlPreviewRenderer fiscalXmlPreviewRenderer;
    private final FiscalAuditService auditService;
    private final ObjectMapper objectMapper;
    private final JwtSecurityContext jwtSecurityContext;

    public FiscalDocumentService(FiscalDocumentRepository documentRepository,
                                 CompanyRepository companyRepository,
                                 FiscalNumberAllocator fiscalNumberAllocator,
                                 FiscalGateway fiscalGateway,
                                 FiscalXmlBuilder fiscalXmlBuilder,
                                 FiscalXmlPreviewRenderer fiscalXmlPreviewRenderer,
                                 FiscalAuditService auditService,
                                 ObjectMapper objectMapper,
                                 JwtSecurityContext jwtSecurityContext) {
        this.documentRepository = documentRepository;
        this.companyRepository = companyRepository;
        this.fiscalNumberAllocator = fiscalNumberAllocator;
        this.fiscalGateway = fiscalGateway;
        this.fiscalXmlBuilder = fiscalXmlBuilder;
        this.fiscalXmlPreviewRenderer = fiscalXmlPreviewRenderer;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.jwtSecurityContext = jwtSecurityContext;
    }

    @Transactional
    public DocumentResponse issue(IssueDocumentRequest request) {
        validateTotals(request);

        FiscalNumberAllocation allocation = allocateFiscalNumber(request);
        Company company = allocation.company();
        jwtSecurityContext.requireCompanyAccess(company);

        documentRepository.findByCompany_IdAndExternalReference(company.getId(), request.externalReference())
            .ifPresent(existing -> {
                throw new ConflictException("Ja existe documento para a referencia externa informada");
            });

        FiscalDocument document = FiscalDocument.create(
            company,
            request.externalReference(),
            request.model(),
            allocation.fiscalNumber().series(),
            allocation.fiscalNumber().number(),
            request.totalAmount(),
            request.customerName(),
            toPayload(request)
        );

        documentRepository.save(document);
        auditService.record(company.getId(), document.getId(), "DOCUMENT_RECEIVED", "Documento fiscal recebido para emissao", request.externalReference());
        process(document, request.items());
        return DocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getById(UUID id) {
        FiscalDocument document = loadDocument(id);
        jwtSecurityContext.requireCompanyAccess(document.getCompany());
        return DocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(UUID companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new NotFoundException("Empresa nao encontrada"));
        jwtSecurityContext.requireCompanyAccess(company);
        return documentRepository.findByCompany_Id(companyId).stream().map(DocumentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public String getReceipt(UUID id) {
        FiscalDocument document = loadDocument(id);
        jwtSecurityContext.requireCompanyAccess(document.getCompany());
        if (document.getReceiptContent() == null || document.getReceiptContent().isBlank()) {
            throw new NotFoundException("Comprovante fiscal ainda nao disponivel");
        }
        return document.getReceiptContent();
    }

    @Transactional(readOnly = true)
    public String getFiscalXml(UUID id) {
        FiscalDocument document = loadDocument(id);
        jwtSecurityContext.requireCompanyAccess(document.getCompany());
        if (document.getFiscalXml() == null || document.getFiscalXml().isBlank()) {
            throw new NotFoundException("XML fiscal ainda nao disponivel");
        }
        return document.getFiscalXml();
    }

    @Transactional(readOnly = true)
    public String getPrintableDocument(UUID id) {
        FiscalDocument document = loadDocument(id);
        jwtSecurityContext.requireCompanyAccess(document.getCompany());
        return """
            <!doctype html>
            <html lang="pt-BR">
            <head>
              <meta charset="utf-8">
              <title>Documento Fiscal %s/%s</title>
              <style>
                body { font-family: Arial, sans-serif; margin: 32px; color: #1f261f; }
                .paper { max-width: 760px; border: 1px solid #ccc; padding: 24px; }
                h1 { margin: 0 0 8px; }
                .muted { color: #666; }
                .row { display: flex; justify-content: space-between; border-bottom: 1px solid #eee; padding: 8px 0; gap: 16px; }
                pre { white-space: pre-wrap; background: #f7f7f7; padding: 12px; }
                @media print { body { margin: 0; } .paper { border: 0; } }
              </style>
            </head>
            <body>
              <main class="paper">
                <h1>%s %s/%s</h1>
                <p class="muted">%s</p>
                <div class="row"><strong>Status</strong><span>%s</span></div>
                <div class="row"><strong>Empresa</strong><span>%s</span></div>
                <div class="row"><strong>Cliente</strong><span>%s</span></div>
                <div class="row"><strong>Referencia</strong><span>%s</span></div>
                <div class="row"><strong>Valor total</strong><span>%s</span></div>
                <div class="row"><strong>Autorizacao</strong><span>%s</span></div>
                <h2>Comprovante</h2>
                <pre>%s</pre>
              </main>
            </body>
            </html>
            """.formatted(
            document.getSeriesNumber(),
            document.getInvoiceNumber(),
            document.getModel(),
            document.getSeriesNumber(),
            document.getInvoiceNumber(),
            escapeHtml(nullToDash(document.getAccessKey())),
            document.getStatus(),
            escapeHtml(document.getCompany().getLegalName()),
            escapeHtml(document.getCustomerName()),
            escapeHtml(document.getExternalReference()),
            document.getTotalAmount(),
            escapeHtml(nullToDash(document.getAuthorizationNumber())),
            escapeHtml(nullToDash(document.getReceiptContent()))
        );
    }

    @Transactional
    public DocumentResponse retry(UUID id) {
        FiscalDocument document = loadDocument(id);
        jwtSecurityContext.requireCompanyAccess(document.getCompany());
        process(document, extractItems(document));
        return DocumentResponse.from(document);
    }

    @Transactional
    public int retryPendingDocuments() {
        List<FiscalDocument> documents = documentRepository.findByStatusInAndNextRetryAtBeforeOrderByCreatedAtAsc(
            EnumSet.of(DocumentStatus.CONTINGENCY_PENDING, DocumentStatus.RETRY_SCHEDULED),
            OffsetDateTime.now().plusSeconds(1),
            PageRequest.of(0, 50)
        );

        documents.forEach(document -> process(document, extractItems(document)));
        return documents.size();
    }

    private FiscalDocument loadDocument(UUID id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Documento fiscal nao encontrado"));
    }

    private void process(FiscalDocument document, List<FiscalItemRequest> items) {
        OffsetDateTime issuedAt = OffsetDateTime.now();
        FiscalSubmission submission = new FiscalSubmission(
            document.getCompany().getId(),
            document.getModel(),
            document.getCompany().getLegalName(),
            document.getCompany().getTradeName(),
            document.getCompany().getTaxId(),
            document.getCompany().getStateCode(),
            document.getCompany().getStateRegistration(),
            document.getCompany().getStreet(),
            document.getCompany().getAddressNumber(),
            document.getCompany().getAddressComplement(),
            document.getCompany().getDistrict(),
            document.getCompany().getCityCode(),
            document.getCompany().getCityName(),
            document.getCompany().getZipCode(),
            document.getCompany().getPhone(),
            document.getCompany().getTaxRegime().getNfeCode(),
            environmentCode(document.getCompany().getFiscalEnvironment()),
            issuedAt,
            document.getSeriesNumber(),
            document.getInvoiceNumber(),
            document.getExternalReference(),
            document.getTotalAmount(),
            document.getCustomerName(),
            items
        );
        FiscalXmlDraft draft = fiscalXmlBuilder.build(submission);
        document.registerAccessKey(draft.accessKey());
        document.registerFiscalXml(fiscalXmlPreviewRenderer.render(draft));
        auditService.record(
            document.getCompany().getId(),
            document.getId(),
            "ACCESS_KEY_CREATED",
            "Chave de acesso fiscal gerada",
            draft.accessKey()
        );

        if (!fiscalGateway.isAvailable(submission)) {
            document.moveToContingency("SEFAZ indisponivel, documento em contingencia", buildContingencyReceipt(document, draft));
            auditService.record(document.getCompany().getId(), document.getId(), "CONTINGENCY_CREATED", "Documento colocado em contingencia", document.getLastError());
            documentRepository.saveAndFlush(document);
            return;
        }

        try {
            var result = fiscalGateway.submit(submission);
            document.authorize(result.authorizationNumber(), result.accessKey(), result.receiptContent());
            auditService.record(document.getCompany().getId(), document.getId(), "DOCUMENT_AUTHORIZED", "Documento fiscal autorizado", result.authorizationNumber());
        } catch (FiscalGatewayException ex) {
            if (ex.isTemporary()) {
                document.moveToContingency(ex.getMessage(), buildContingencyReceipt(document, draft));
                auditService.record(document.getCompany().getId(), document.getId(), "CONTINGENCY_CREATED", "Falha temporaria; documento colocado em contingencia", ex.getMessage());
                documentRepository.saveAndFlush(document);
                return;
            }
            document.reject(ex.getMessage());
            auditService.record(document.getCompany().getId(), document.getId(), "DOCUMENT_REJECTED", "Documento fiscal rejeitado", ex.getMessage());
        }
        documentRepository.saveAndFlush(document);
    }

    private String environmentCode(FiscalEnvironment fiscalEnvironment) {
        return fiscalEnvironment == FiscalEnvironment.PRODUCTION ? "1" : "2";
    }

    private String toPayload(IssueDocumentRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar payload fiscal", ex);
        }
    }

    private String buildContingencyReceipt(FiscalDocument document, FiscalXmlDraft draft) {
        return """
            CUPOM EM CONTINGENCIA
            Modelo: %s
            Chave de acesso: %s
            Documento: %s
            Serie/Numero: %s/%s
            Empresa: %s
            Cliente: %s
            Valor: %s
            Emitido localmente em: %s
            """.formatted(
            document.getModel(),
            draft.accessKey(),
            document.getExternalReference(),
            document.getSeriesNumber(),
            document.getInvoiceNumber(),
            document.getCompany().getLegalName(),
            document.getCustomerName(),
            document.getTotalAmount(),
            OffsetDateTime.now()
        );
    }

    private List<FiscalItemRequest> extractItems(FiscalDocument document) {
        try {
            var payload = objectMapper.readValue(document.getPayloadJson(), IssueDocumentRequest.class);
            return payload.items();
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao reconstituir payload fiscal", ex);
        }
    }

    private void validateTotals(IssueDocumentRequest request) {
        BigDecimal itemsTotal = request.items().stream()
            .map(FiscalItemRequest::totalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (itemsTotal.compareTo(request.totalAmount()) != 0) {
            throw new ConflictException("Total dos itens nao confere com o total do documento");
        }
    }

    private FiscalNumberAllocation allocateFiscalNumber(IssueDocumentRequest request) {
        if (request.companyId() != null) {
            return fiscalNumberAllocator.allocate(request.companyId(), request.model());
        }

        if (hasText(request.bivaroTenantId()) && hasText(request.bivaroMerchantId())) {
            return fiscalNumberAllocator.allocateByBivaroMerchant(
                request.bivaroTenantId(),
                request.bivaroMerchantId(),
                request.model()
            );
        }

        throw new BadRequestException("Informe companyId ou bivaroTenantId + bivaroMerchantId para emitir.");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
