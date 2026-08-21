package br.com.antigravity.fiscalapi.fiscal;

/// Retorno do evento de cancelamento: o protocolo que a SEFAZ registrou e o
/// que ela respondeu, para ficar no historico da nota.
public record FiscalCancellationResult(
    String protocol,
    String message
) {
}
