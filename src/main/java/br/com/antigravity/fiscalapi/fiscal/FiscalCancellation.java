package br.com.antigravity.fiscalapi.fiscal;

/**
 * Pedido de cancelamento de um documento ja autorizado.
 *
 * A justificativa e exigida pela SEFAZ com no minimo 15 caracteres — ela vai
 * para o evento e fica no historico da nota, entao "erro" nao serve.
 */
public record FiscalCancellation(
    String accessKey,
    String authorizationNumber,
    String taxId,
    String reason
) {
}
