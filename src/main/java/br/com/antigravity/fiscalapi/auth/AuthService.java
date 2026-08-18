package br.com.antigravity.fiscalapi.auth;

import br.com.antigravity.fiscalapi.config.AppProperties;
import br.com.antigravity.fiscalapi.security.JwtTokenService;
import br.com.antigravity.fiscalapi.shared.BadRequestException;
import br.com.antigravity.fiscalapi.shared.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String BASIC_PREFIX = "Basic ";

    private final AppProperties properties;
    private final JwtTokenService jwtTokenService;

    public AuthService(AppProperties properties, JwtTokenService jwtTokenService) {
        this.properties = properties;
        this.jwtTokenService = jwtTokenService;
    }

    public TokenResponse issueToken(TokenRequest request, String authorizationHeader) {
        if (!properties.getSecurity().getJwt().isEnabled()) {
            throw new BadRequestException("JWT_AUTH_ENABLED=false; use Login e senha no Swagger");
        }

        TokenRequest safeRequest = request == null ? TokenRequest.empty() : request;
        BasicCredentials basicCredentials = basicCredentials(authorizationHeader);

        String username = firstText(safeRequest.username(), safeRequest.clientId(), basicCredentials.username());
        String password = firstText(safeRequest.password(), safeRequest.clientSecret(), basicCredentials.password());

        if (!hasText(username) || !hasText(password)) {
            throw new UnauthorizedException("Credenciais ausentes");
        }
        if (!matches(username, properties.getSecurity().getIntegrationClient().getUsername())
            || !matches(password, properties.getSecurity().getIntegrationClient().getPassword())) {
            throw new UnauthorizedException("Credenciais inválidas");
        }

        Set<String> scopes = scopes(safeRequest.scopes());
        int expiresInMinutes = expiresInMinutes(safeRequest.expiresInMinutes());
        String token = jwtTokenService.issue(
            safeRequest.subject(),
            safeRequest.tenantId(),
            safeRequest.merchantId(),
            scopes,
            expiresInMinutes * 60L
        );
        return new TokenResponse(token, "Bearer", expiresInMinutes * 60L, "Authorization", "Bearer " + token);
    }

    private Set<String> scopes(List<String> requestedScopes) {
        Set<String> defaultScopes = properties.getSecurity().getIntegrationClient().getDefaultScopes();
        if (requestedScopes == null || requestedScopes.isEmpty()) {
            return defaultScopes;
        }
        if (defaultScopes.contains("fiscal:admin") || defaultScopes.containsAll(requestedScopes)) {
            return Set.copyOf(requestedScopes);
        }
        throw new BadRequestException("Escopo solicitado não permitido para esta integração");
    }

    private int expiresInMinutes(Integer requestedExpiresInMinutes) {
        int configuredTtl = properties.getSecurity().getIntegrationClient().getTokenTtlMinutes();
        int requestedTtl = requestedExpiresInMinutes == null ? configuredTtl : requestedExpiresInMinutes;
        return Math.max(1, Math.min(requestedTtl, configuredTtl));
    }

    private BasicCredentials basicCredentials(String authorizationHeader) {
        if (!hasText(authorizationHeader) || !authorizationHeader.startsWith(BASIC_PREFIX)) {
            return BasicCredentials.empty();
        }

        try {
            String decoded = new String(
                Base64.getDecoder().decode(authorizationHeader.substring(BASIC_PREFIX.length()).trim()),
                StandardCharsets.UTF_8
            );
            int separator = decoded.indexOf(':');
            if (separator < 0) {
                return BasicCredentials.empty();
            }
            return new BasicCredentials(decoded.substring(0, separator), decoded.substring(separator + 1));
        } catch (IllegalArgumentException ex) {
            return BasicCredentials.empty();
        }
    }

    private boolean matches(String provided, String expected) {
        if (provided == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(
            provided.getBytes(StandardCharsets.UTF_8),
            expected.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record BasicCredentials(String username, String password) {
        static BasicCredentials empty() {
            return new BasicCredentials(null, null);
        }
    }
}
