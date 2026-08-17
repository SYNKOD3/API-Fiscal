package br.com.antigravity.fiscalapi.company;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CompanyResponse(
    UUID id,
    String bivaroTenantId,
    String bivaroMerchantId,
    String callbackUrl,
    String legalName,
    String taxId,
    String stateRegistration,
    String stateCode,
    String tradeName,
    String street,
    String addressNumber,
    String addressComplement,
    String district,
    String cityCode,
    String cityName,
    String zipCode,
    String phone,
    TaxRegime taxRegime,
    FiscalEnvironment fiscalEnvironment,
    boolean certificateConfigured,
    boolean cscConfigured,
    int nfeSeriesNumber,
    long nextNfeNumber,
    int nfceSeriesNumber,
    long nextNfceNumber,
    boolean active,
    OffsetDateTime createdAt
) {
    public static CompanyResponse from(Company company) {
        boolean legacyCertificateConfigured = company.getCertificatePath() != null && !company.getCertificatePath().isBlank();
        return from(company, legacyCertificateConfigured);
    }

    public static CompanyResponse from(Company company, boolean certificateConfigured) {
        return new CompanyResponse(
            company.getId(),
            company.getBivaroTenantId(),
            company.getBivaroMerchantId(),
            company.getCallbackUrl(),
            company.getLegalName(),
            company.getTaxId(),
            company.getStateRegistration(),
            company.getStateCode(),
            company.getTradeName(),
            company.getStreet(),
            company.getAddressNumber(),
            company.getAddressComplement(),
            company.getDistrict(),
            company.getCityCode(),
            company.getCityName(),
            company.getZipCode(),
            company.getPhone(),
            company.getTaxRegime(),
            company.getFiscalEnvironment(),
            certificateConfigured,
            company.getCscId() != null && !company.getCscId().isBlank()
                && company.getCscToken() != null && !company.getCscToken().isBlank(),
            company.getNfeSeriesNumber(),
            company.getNextNfeNumber(),
            company.getNfceSeriesNumber(),
            company.getNextNfceNumber(),
            company.isActive(),
            company.getCreatedAt()
        );
    }
}
