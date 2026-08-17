package br.com.antigravity.fiscalapi.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CreateCompanyRequest(
    String tenantId,
    String merchantId,
    String callbackUrl,
    @NotBlank String legalName,
    @NotBlank @Pattern(regexp = "\\d{14}") String taxId,
    @NotBlank String stateRegistration,
    @NotBlank @Pattern(regexp = "[A-Z]{2}") String stateCode,
    String tradeName,
    @NotBlank String street,
    @NotBlank String addressNumber,
    String addressComplement,
    @NotBlank String district,
    @NotBlank @Pattern(regexp = "\\d{7}") String cityCode,
    @NotBlank String cityName,
    @NotBlank @Pattern(regexp = "\\d{8}") String zipCode,
    String phone,
    TaxRegime taxRegime,
    FiscalEnvironment fiscalEnvironment,
    String certificatePath,
    String certificatePassword,
    String cscId,
    String cscToken,
    @Positive Integer nfeSeriesNumber,
    @Positive Long nextNfeNumber,
    @Positive Integer nfceSeriesNumber,
    @Positive Long nextNfceNumber
) {
}
