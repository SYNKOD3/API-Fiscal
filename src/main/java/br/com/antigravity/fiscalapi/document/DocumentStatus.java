package br.com.antigravity.fiscalapi.document;

public enum DocumentStatus {
    RECEIVED,
    AUTHORIZED,
    CONTINGENCY_PENDING,
    REJECTED,
    RETRY_SCHEDULED,
    /// Nota que existiu e foi cancelada na SEFAZ pelo evento 110111.
    CANCELLED
}
