package br.com.antigravity.fiscalapi.operational;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "operational_logs")
public class OperationalLogEvent {

    @Id
    private UUID id;

    @Column(nullable = false, length = 64)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OperationalLogLevel level;

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column(length = 16)
    private String httpMethod;

    @Column(length = 512)
    private String path;

    private Integer statusCode;
    private Long durationMs;
    private UUID companyId;
    private UUID documentId;

    @Column(length = 255)
    private String externalReference;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(columnDefinition = "text")
    private String details;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public static OperationalLogEvent create(OperationalLogRecord record) {
        OperationalLogEvent event = new OperationalLogEvent();
        event.id = UUID.randomUUID();
        event.requestId = record.requestId();
        event.level = record.level();
        event.eventType = record.eventType();
        event.httpMethod = record.httpMethod();
        event.path = record.path();
        event.statusCode = record.statusCode();
        event.durationMs = record.durationMs();
        event.companyId = record.companyId();
        event.documentId = record.documentId();
        event.externalReference = record.externalReference();
        event.message = record.message();
        event.details = record.details();
        event.createdAt = OffsetDateTime.now();
        return event;
    }

    public UUID getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public OperationalLogLevel getLevel() {
        return level;
    }

    public String getEventType() {
        return eventType;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getExternalReference() {
        return externalReference;
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
