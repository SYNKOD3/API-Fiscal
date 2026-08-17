package br.com.antigravity.fiscalapi.operational;

import java.util.UUID;

public record OperationalLogRecord(
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
    String details
) {
}
