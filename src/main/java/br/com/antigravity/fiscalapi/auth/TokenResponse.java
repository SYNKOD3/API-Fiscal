package br.com.antigravity.fiscalapi.auth;

public record TokenResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    String headerName,
    String headerValue
) {
}
