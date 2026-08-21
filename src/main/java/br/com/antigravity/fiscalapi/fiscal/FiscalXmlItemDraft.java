package br.com.antigravity.fiscalapi.fiscal;

import java.math.BigDecimal;

public record FiscalXmlItemDraft(
    int itemNumber,
    String sku,
    String description,
    String ncm,
    String cest,
    String gtin,
    String cfop,
    String unit,
    BigDecimal quantity,
    BigDecimal unitAmount,
    BigDecimal totalAmount,
    String origin,
    String icmsCode,
    String pisCode,
    String cofinsCode,
    /// CST e classificacao do IBS/CBS ja resolvidos: o do item quando ele tem
    /// tratamento proprio, o da empresa quando nao tem.
    String ibsCbsCst,
    String ibsCbsClassTrib,
    BigDecimal approximateTaxAmount
) {
}
