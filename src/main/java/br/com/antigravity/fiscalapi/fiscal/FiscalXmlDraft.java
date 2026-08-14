package br.com.antigravity.fiscalapi.fiscal;

import br.com.antigravity.fiscalapi.document.DocumentModel;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record FiscalXmlDraft(
    DocumentModel model,
    String version,
    String invoiceId,
    String accessKey,
    String numericCode,
    String checkDigit,
    String emissionType,
    String fiscalEnvironmentCode,
    OffsetDateTime issuedAt,
    FiscalIssuerDraft issuer,
    int seriesNumber,
    long invoiceNumber,
    BigDecimal totalAmount,
    List<FiscalXmlItemDraft> items
) {
}
