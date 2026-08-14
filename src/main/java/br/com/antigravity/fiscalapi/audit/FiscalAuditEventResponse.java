package br.com.antigravity.fiscalapi.audit;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FiscalAuditEventResponse(
    UUID id,
    UUID companyId,
    UUID documentId,
    String eventType,
    String message,
    String details,
    OffsetDateTime createdAt
) {
    public static FiscalAuditEventResponse from(FiscalAuditEvent event) {
        return new FiscalAuditEventResponse(
            event.getId(),
            event.getCompanyId(),
            event.getDocumentId(),
            event.getEventType(),
            event.getMessage(),
            event.getDetails(),
            event.getCreatedAt()
        );
    }
}
