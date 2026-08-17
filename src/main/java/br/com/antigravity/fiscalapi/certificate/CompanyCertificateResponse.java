package br.com.antigravity.fiscalapi.certificate;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CompanyCertificateResponse(
    UUID id,
    UUID companyId,
    String originalFileName,
    String certificateTaxId,
    String serialNumber,
    String subjectDn,
    OffsetDateTime validFrom,
    OffsetDateTime validUntil,
    CertificateStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime activatedAt,
    OffsetDateTime replacedAt
) {
    public static CompanyCertificateResponse from(CompanyCertificate certificate) {
        return new CompanyCertificateResponse(
            certificate.getId(),
            certificate.getCompany().getId(),
            certificate.getOriginalFileName(),
            certificate.getCertificateTaxId(),
            certificate.getSerialNumber(),
            certificate.getSubjectDn(),
            certificate.getValidFrom(),
            certificate.getValidUntil(),
            certificate.getStatus(),
            certificate.getCreatedAt(),
            certificate.getActivatedAt(),
            certificate.getReplacedAt()
        );
    }
}
