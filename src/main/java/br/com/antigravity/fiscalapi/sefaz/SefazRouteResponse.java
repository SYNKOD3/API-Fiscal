package br.com.antigravity.fiscalapi.sefaz;

import br.com.antigravity.fiscalapi.company.FiscalEnvironment;
import br.com.antigravity.fiscalapi.document.DocumentModel;
import java.util.UUID;

public record SefazRouteResponse(
    UUID companyId,
    String bivaroTenantId,
    String bivaroMerchantId,
    String companyTaxId,
    String stateCode,
    DocumentModel model,
    FiscalEnvironment environment,
    String authorizerStrategy,
    SefazContingencyStrategy contingencyStrategy,
    boolean available,
    String message
) {
}
