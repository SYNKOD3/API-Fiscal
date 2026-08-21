package br.com.antigravity.fiscalapi.document;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Justificativa do cancelamento.
 *
 * A SEFAZ exige de 15 a 255 caracteres. Conferir aqui devolve o motivo em
 * portugues antes de gastar uma ida a SEFAZ para ouvir a mesma coisa em codigo.
 */
public record CancelDocumentRequest(
    @Schema(
        description = "Justificativa do cancelamento, de 15 a 255 caracteres, como a SEFAZ exige.",
        example = "Cliente desistiu da compra apos a emissao"
    )
    @NotBlank(message = "Informe a justificativa do cancelamento.")
    @Size(min = 15, max = 255, message = "A justificativa precisa ter de 15 a 255 caracteres.")
    String reason
) {
}
