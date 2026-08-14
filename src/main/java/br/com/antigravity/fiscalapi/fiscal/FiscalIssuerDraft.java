package br.com.antigravity.fiscalapi.fiscal;

public record FiscalIssuerDraft(
    String legalName,
    String tradeName,
    String taxId,
    String stateRegistration,
    String stateCode,
    String street,
    String number,
    String complement,
    String district,
    String cityCode,
    String cityName,
    String zipCode,
    String phone,
    String taxRegimeCode
) {
}
