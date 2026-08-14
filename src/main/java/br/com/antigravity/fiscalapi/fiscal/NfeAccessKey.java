package br.com.antigravity.fiscalapi.fiscal;

public record NfeAccessKey(
    String value,
    String numericCode,
    String checkDigit
) {
}
