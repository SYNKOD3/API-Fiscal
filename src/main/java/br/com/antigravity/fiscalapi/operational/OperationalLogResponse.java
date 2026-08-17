package br.com.antigravity.fiscalapi.operational;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OperationalLogResponse(
    UUID id,
    String requestId,
    OperationalLogLevel level,
    String eventType,
    String httpMethod,
    String path,
    Integer statusCode,
    Long durationMs,
    UUID companyId,
    UUID documentId,
    String externalReference,
    String message,
    String details,
    OffsetDateTime createdAt
) {
    public static OperationalLogResponse from(OperationalLogEvent event) {
        return new OperationalLogResponse(
            event.getId(),
            event.getRequestId(),
            event.getLevel(),
            event.getEventType(),
            event.getHttpMethod(),
            event.getPath(),
            event.getStatusCode(),
            event.getDurationMs(),
            event.getCompanyId(),
            event.getDocumentId(),
            event.getExternalReference(),
            event.getMessage(),
            event.getDetails(),
            event.getCreatedAt()
        );
    }
}
