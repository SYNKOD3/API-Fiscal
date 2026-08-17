package br.com.antigravity.fiscalapi.document;

import br.com.antigravity.fiscalapi.company.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "fiscal_documents",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_fiscal_documents_company_external_reference",
        columnNames = {"company_id", "external_reference"}
    )
)
public class FiscalDocument {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "external_reference", nullable = false)
    private String externalReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentModel model;

    @Column(nullable = false)
    private int seriesNumber;

    @Column(nullable = false)
    private long invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false, columnDefinition = "text")
    private String payloadJson;

    @Column(length = 4096)
    private String receiptContent;

    @Column(columnDefinition = "text")
    private String fiscalXml;

    private String authorizationNumber;
    private String accessKey;

    @Column(length = 1000)
    private String lastError;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    private OffsetDateTime authorizedAt;
    private OffsetDateTime nextRetryAt;

    public static FiscalDocument create(Company company,
                                        String externalReference,
                                        DocumentModel model,
                                        int seriesNumber,
                                        long invoiceNumber,
                                        BigDecimal totalAmount,
                                        String customerName,
                                        String payloadJson) {
        FiscalDocument document = new FiscalDocument();
        document.id = UUID.randomUUID();
        document.company = company;
        document.externalReference = externalReference;
        document.model = model;
        document.seriesNumber = seriesNumber;
        document.invoiceNumber = invoiceNumber;
        document.totalAmount = totalAmount;
        document.customerName = customerName;
        document.payloadJson = payloadJson;
        document.status = DocumentStatus.RECEIVED;
        document.retryCount = 0;
        document.createdAt = OffsetDateTime.now();
        document.updatedAt = document.createdAt;
        return document;
    }

    public void authorize(String authorizationNumber, String accessKey, String receiptContent) {
        this.status = DocumentStatus.AUTHORIZED;
        this.authorizationNumber = authorizationNumber;
        this.accessKey = accessKey;
        this.receiptContent = receiptContent;
        this.lastError = null;
        this.authorizedAt = OffsetDateTime.now();
        this.nextRetryAt = null;
        this.updatedAt = OffsetDateTime.now();
    }

    public void registerAccessKey(String accessKey) {
        if (this.accessKey == null || this.accessKey.isBlank()) {
            this.accessKey = accessKey;
            this.updatedAt = OffsetDateTime.now();
        }
    }

    public void registerFiscalXml(String fiscalXml) {
        this.fiscalXml = fiscalXml;
        this.updatedAt = OffsetDateTime.now();
    }

    public void moveToContingency(String reason, String receiptContent) {
        this.status = DocumentStatus.CONTINGENCY_PENDING;
        this.lastError = reason;
        this.receiptContent = receiptContent;
        this.retryCount += 1;
        this.nextRetryAt = OffsetDateTime.now().plusMinutes(1);
        this.updatedAt = OffsetDateTime.now();
    }

    public void scheduleRetry(String reason) {
        this.status = DocumentStatus.RETRY_SCHEDULED;
        this.lastError = reason;
        this.retryCount += 1;
        this.nextRetryAt = OffsetDateTime.now().plusMinutes(1);
        this.updatedAt = OffsetDateTime.now();
    }

    public void reject(String reason) {
        this.status = DocumentStatus.REJECTED;
        this.lastError = reason;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public DocumentModel getModel() {
        return model;
    }

    public int getSeriesNumber() {
        return seriesNumber;
    }

    public long getInvoiceNumber() {
        return invoiceNumber;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getReceiptContent() {
        return receiptContent;
    }

    public String getFiscalXml() {
        return fiscalXml;
    }

    public String getAuthorizationNumber() {
        return authorizationNumber;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getLastError() {
        return lastError;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getAuthorizedAt() {
        return authorizedAt;
    }

    public OffsetDateTime getNextRetryAt() {
        return nextRetryAt;
    }
}
