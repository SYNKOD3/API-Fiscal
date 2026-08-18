package br.com.antigravity.fiscalapi.auth;

import java.util.List;

public record TokenRequest(
    String username,
    String password,
    String clientId,
    String clientSecret,
    String subject,
    String tenantId,
    String merchantId,
    List<String> scopes,
    Integer expiresInMinutes
) {
    static TokenRequest empty() {
        return new TokenRequest(null, null, null, null, null, null, null, null, null);
    }
}
