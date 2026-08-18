package br.com.antigravity.fiscalapi.document;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record FiscalItemRequest(
    @Schema(description = "Código interno do produto.", example = "PROD-001")
    @NotBlank String sku,
    @Schema(description = "Descrição do produto.", example = "Produto de Teste")
    @NotBlank String description,
    @Schema(description = "NCM com 8 dígitos.", example = "01012100")
    @NotBlank @Pattern(regexp = "\\d{8}") String ncm,
    @Schema(description = "CEST quando aplicável.", example = "")
    String cest,
    @Schema(description = "GTIN/EAN ou SEM GTIN.", example = "SEM GTIN")
    String gtin,
    @Schema(description = "CFOP da operação.", example = "5102")
    @NotBlank String cfop,
    @Schema(description = "Unidade comercial.", example = "UN")
    @NotBlank String unit,
    @Schema(description = "Quantidade.", example = "1")
    @NotNull @DecimalMin("0.0001") BigDecimal quantity,
    @Schema(description = "Valor unitário.", example = "199.90")
    @NotNull @DecimalMin("0.01") BigDecimal unitAmount,
    @Schema(description = "Valor total do item.", example = "199.90")
    @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
    @Schema(description = "Origem da mercadoria.", example = "0")
    @NotBlank String origin,
    @Schema(description = "Código ICMS conforme regime tributário.", example = "102")
    @NotBlank String icmsCode,
    @Schema(description = "Código PIS.", example = "49")
    @NotBlank String pisCode,
    @Schema(description = "Código COFINS.", example = "49")
    @NotBlank String cofinsCode,
    @Schema(description = "Valor aproximado dos tributos.", example = "0")
    @NotNull @DecimalMin("0.00") BigDecimal approximateTaxAmount
) {
}
