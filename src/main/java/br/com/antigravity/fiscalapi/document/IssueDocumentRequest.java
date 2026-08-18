package br.com.antigravity.fiscalapi.document;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record IssueDocumentRequest(
    @Schema(description = "ID interno da empresa emissora. Pode ser omitido se tenantId e merchantId forem enviados.", example = "00000000-0000-0000-0000-000000000000")
    UUID companyId,
    @Schema(description = "Identificador do tenant na plataforma integradora.", example = "tenant-dev")
    String tenantId,
    @Schema(description = "Identificador do lojista/empresa na plataforma integradora.", example = "merchant-dev")
    String merchantId,
    @Schema(description = "Modelo fiscal: NFE ou NFCE.", example = "NFCE")
    @NotNull DocumentModel model,
    @Schema(description = "Referência única do pedido/venda no sistema integrador.", example = "PEDIDO-1001")
    @NotBlank String externalReference,
    @Schema(description = "Nome do consumidor ou destinatário.", example = "Cliente Teste")
    @NotBlank String customerName,
    @Schema(description = "Valor total do documento.", example = "199.90")
    @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
    @Schema(description = "Itens fiscais do documento.")
    @NotEmpty List<@Valid FiscalItemRequest> items
) {
}
