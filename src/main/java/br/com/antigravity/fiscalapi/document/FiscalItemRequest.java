package br.com.antigravity.fiscalapi.document;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record FiscalItemRequest(
    @NotBlank String sku,
    @NotBlank String description,
    @NotBlank @Pattern(regexp = "\\d{8}") String ncm,
    String cest,
    String gtin,
    @NotBlank String cfop,
    @NotBlank String unit,
    @NotNull @DecimalMin("0.0001") BigDecimal quantity,
    @NotNull @DecimalMin("0.01") BigDecimal unitAmount,
    @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
    @NotBlank String origin,
    @NotBlank String icmsCode,
    @NotBlank String pisCode,
    @NotBlank String cofinsCode,
    @NotNull @DecimalMin("0.00") BigDecimal approximateTaxAmount
) {
}
