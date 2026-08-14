package br.com.antigravity.fiscalapi.fiscal;

public record FiscalSubmissionResult(
    String authorizationNumber,
    String accessKey,
    String receiptContent
) {
}
