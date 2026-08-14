package br.com.antigravity.fiscalapi.document;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentResponse(
    UUID id,
    UUID companyId,
    String externalReference,
    DocumentModel model,
    int seriesNumber,
    long invoiceNumber,
    DocumentStatus status,
    BigDecimal totalAmount,
    String customerName,
    String authorizationNumber,
    String accessKey,
    String receiptContent,
    String lastError,
    int retryCount,
    OffsetDateTime createdAt,
    OffsetDateTime authorizedAt
) {
    public static DocumentResponse from(FiscalDocument document) {
        return new DocumentResponse(
            document.getId(),
            document.getCompany().getId(),
            document.getExternalReference(),
            document.getModel(),
            document.getSeriesNumber(),
            document.getInvoiceNumber(),
            document.getStatus(),
            document.getTotalAmount(),
            document.getCustomerName(),
            document.getAuthorizationNumber(),
            document.getAccessKey(),
            document.getReceiptContent(),
            document.getLastError(),
            document.getRetryCount(),
            document.getCreatedAt(),
            document.getAuthorizedAt()
        );
    }
}
