package br.com.antigravity.fiscalapi.company;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Os dois codigos do IBS/CBS, e mais nada.
 *
 * Existe porque atualizar a empresa inteira so para informa-los obriga a
 * reenviar o cadastro completo — e esquecer um campo no caminho apaga dado
 * de quem esta so tentando cadastrar um codigo tributario.
 *
 * Aqui os dois sao obrigatorios: quem chama esta rota esta informando os
 * codigos, e um par pela metade nao serve para nada — o grupo IBSCBS precisa
 * dos dois para ir ao XML.
 */
public record UpdateIbsCbsRequest(
    @Schema(description = "CST do IBS/CBS, 3 dígitos. Definido pelo contador da empresa.", example = "000")
    @NotBlank(message = "Informe o CST do IBS/CBS.")
    @Pattern(regexp = "\\d{3}", message = "O CST do IBS/CBS tem 3 dígitos.")
    String ibsCbsCst,

    @Schema(description = "Classificação tributária do IBS/CBS, 6 dígitos.", example = "000001")
    @NotBlank(message = "Informe a classificação tributária do IBS/CBS.")
    @Pattern(regexp = "\\d{6}", message = "A classificação tributária do IBS/CBS tem 6 dígitos.")
    String ibsCbsClassTrib
) {
}
