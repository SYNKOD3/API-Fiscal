package br.com.antigravity.fiscalapi.security;

import java.time.Instant;
import java.util.Set;

public record JwtPrincipal(
    String subject,
    String issuer,
    String audience,
    String jwtId,
    String bivaroTenantId,
    String bivaroMerchantId,
    Set<String> scopes,
    Instant expiresAt
) {
    public boolean hasScope(String scope) {
        return scopes.contains("fiscal:admin") || scopes.contains(scope);
    }
}
