package br.com.antigravity.fiscalapi.certificate;

import br.com.antigravity.fiscalapi.company.Company;
import br.com.antigravity.fiscalapi.config.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_certificates")
public class CompanyCertificate {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false, length = 1024)
    private String storagePath;

    @Column(nullable = false)
    private String originalFileName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 2048)
    private String certificatePassword;

    @Column(length = 14)
    private String certificateTaxId;

    @Column(length = 128)
    private String serialNumber;

    @Column(length = 1000)
    private String subjectDn;

    private OffsetDateTime validFrom;
    private OffsetDateTime validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CertificateStatus status;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime activatedAt;

    private OffsetDateTime replacedAt;

    public static CompanyCertificate active(Company company,
                                            String storagePath,
                                            String originalFileName,
                                            String certificatePassword,
                                            CertificateMetadata metadata) {
        CompanyCertificate certificate = new CompanyCertificate();
        certificate.id = UUID.randomUUID();
        certificate.company = company;
        certificate.storagePath = storagePath;
        certificate.originalFileName = originalFileName;
        certificate.certificatePassword = certificatePassword;
        certificate.certificateTaxId = metadata.taxId();
        certificate.serialNumber = metadata.serialNumber();
        certificate.subjectDn = metadata.subjectDn();
        certificate.validFrom = metadata.validFrom();
        certificate.validUntil = metadata.validUntil();
        certificate.status = CertificateStatus.ACTIVE;
        certificate.createdAt = OffsetDateTime.now();
        certificate.activatedAt = certificate.createdAt;
        return certificate;
    }

    public void replace() {
        this.status = CertificateStatus.REPLACED;
        this.replacedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getCertificatePassword() {
        return certificatePassword;
    }

    public String getCertificateTaxId() {
        return certificateTaxId;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getSubjectDn() {
        return subjectDn;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public OffsetDateTime getValidUntil() {
        return validUntil;
    }

    public CertificateStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getActivatedAt() {
        return activatedAt;
    }

    public OffsetDateTime getReplacedAt() {
        return replacedAt;
    }
}
