package br.com.antigravity.fiscalapi.fiscal;

import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.antigravity.fiscalapi.document.FiscalItemRequest;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record FiscalSubmission(
    UUID companyId,
    DocumentModel model,
    String companyLegalName,
    String companyTradeName,
    String companyTaxId,
    String companyStateCode,
    String stateRegistration,
    String companyStreet,
    String companyAddressNumber,
    String companyAddressComplement,
    String companyDistrict,
    String companyCityCode,
    String companyCityName,
    String companyZipCode,
    String companyPhone,
    String companyTaxRegimeCode,
    String fiscalEnvironmentCode,
    OffsetDateTime issuedAt,
    int seriesNumber,
    long invoiceNumber,
    String externalReference,
    BigDecimal totalAmount,
    String customerName,
    List<FiscalItemRequest> items
) {
}
