package br.com.antigravity.fiscalapi.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fiscal_audit_events")
public class FiscalAuditEvent {

    @Id
    private UUID id;

    private UUID companyId;
    private UUID documentId;

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 1000)
    private String message;

    @Lob
    private String details;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public static FiscalAuditEvent create(UUID companyId,
                                          UUID documentId,
                                          String eventType,
                                          String message,
                                          String details) {
        FiscalAuditEvent event = new FiscalAuditEvent();
        event.id = UUID.randomUUID();
        event.companyId = companyId;
        event.documentId = documentId;
        event.eventType = eventType;
        event.message = message;
        event.details = details;
        event.createdAt = OffsetDateTime.now();
        return event;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
