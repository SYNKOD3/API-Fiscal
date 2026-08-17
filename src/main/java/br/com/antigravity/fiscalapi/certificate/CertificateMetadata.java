package br.com.antigravity.fiscalapi.certificate;

import java.time.OffsetDateTime;

public record CertificateMetadata(
    String taxId,
    String serialNumber,
    String subjectDn,
    OffsetDateTime validFrom,
    OffsetDateTime validUntil
) {
}
