package br.com.antigravity.fiscalapi.document;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record IssueDocumentRequest(
    UUID companyId,
    String tenantId,
    String merchantId,
    @NotNull DocumentModel model,
    @NotBlank String externalReference,
    @NotBlank String customerName,
    @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
    @NotEmpty List<@Valid FiscalItemRequest> items
) {
}
