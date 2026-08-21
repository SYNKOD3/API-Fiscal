package br.com.antigravity.fiscalapi.fiscal;

public record FiscalSubmissionResult(
    String authorizationNumber,
    String accessKey,
    String receiptContent,
    /// Conteudo do QR Code aplicado a NFC-e; nulo em NF-e.
    String qrCode
) {
}
