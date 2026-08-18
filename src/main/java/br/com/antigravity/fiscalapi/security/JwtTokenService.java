package br.com.antigravity.fiscalapi.security;

import br.com.antigravity.fiscalapi.config.AppProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public JwtTokenService(AppProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, Clock.systemUTC());
    }

    JwtTokenService(AppProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public JwtPrincipal validate(String token) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            throw new JwtValidationException("JWT malformado");
        }

        Map<String, Object> header = decodeJson(parts[0]);
        if (!"HS256".equals(header.get("alg"))) {
            throw new JwtValidationException("Algoritmo JWT nao permitido");
        }

        String signedContent = parts[0] + "." + parts[1];
        verifySignature(signedContent, parts[2]);

        Map<String, Object> claims = decodeJson(parts[1]);
        validateIssuer(claims);
        validateAudience(claims);
        validateTimeClaims(claims);

        return new JwtPrincipal(
            stringClaim(claims, "sub"),
            stringClaim(claims, "iss"),
            firstAudience(claims),
            stringClaim(claims, "jti"),
            stringClaim(claims, "tenantId"),
            stringClaim(claims, "merchantId"),
            scopes(claims),
            instantClaim(claims, "exp")
        );
    }

    public String issue(String subject,
                        String tenantId,
                        String merchantId,
                        Collection<String> scopes,
                        long expiresInSeconds) {
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", properties.getSecurity().getJwt().getIssuer());
        claims.put("aud", properties.getSecurity().getJwt().getAudience());
        claims.put("sub", hasText(subject) ? subject : "integration-client");
        claims.put("jti", UUID.randomUUID().toString());
        if (hasText(tenantId)) {
            claims.put("tenantId", tenantId);
        }
        if (hasText(merchantId)) {
            claims.put("merchantId", merchantId);
        }
        claims.put("scopes", scopes == null || scopes.isEmpty() ? List.of("fiscal:admin") : List.copyOf(scopes));
        claims.put("exp", Instant.now(clock).plusSeconds(expiresInSeconds).getEpochSecond());

        String signedContent = encodeJson(header) + "." + encodeJson(claims);
        return signedContent + "." + BASE64_URL_ENCODER.encodeToString(sign(signedContent));
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao gerar JWT", ex);
        }
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            return objectMapper.readValue(BASE64_URL_DECODER.decode(value), new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new JwtValidationException("JWT invalido");
        }
    }

    private void verifySignature(String signedContent, String signature) {
        byte[] expected = sign(signedContent);
        byte[] actual;
        try {
            actual = BASE64_URL_DECODER.decode(signature);
        } catch (IllegalArgumentException ex) {
            throw new JwtValidationException("Assinatura JWT invalida");
        }

        if (!MessageDigest.isEqual(expected, actual)) {
            throw new JwtValidationException("Assinatura JWT invalida");
        }
    }

    private byte[] sign(String signedContent) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao validar assinatura JWT", ex);
        }
    }

    private String secret() {
        String secret = properties.getSecurity().getJwt().getSecret();
        if (secret == null || secret.isBlank()) {
            throw new JwtValidationException("JWT secret nao configurado");
        }
        return secret;
    }

    private void validateIssuer(Map<String, Object> claims) {
        String expectedIssuer = properties.getSecurity().getJwt().getIssuer();
        if (expectedIssuer != null && !expectedIssuer.isBlank()
            && !expectedIssuer.equals(stringClaim(claims, "iss"))) {
            throw new JwtValidationException("Emissor JWT invalido");
        }
    }

    private void validateAudience(Map<String, Object> claims) {
        String expectedAudience = properties.getSecurity().getJwt().getAudience();
        if (expectedAudience == null || expectedAudience.isBlank()) {
            return;
        }
        List<String> audiences = audiences(claims);
        if (!audiences.contains(expectedAudience)) {
            throw new JwtValidationException("Audiencia JWT invalida");
        }
    }

    private void validateTimeClaims(Map<String, Object> claims) {
        Instant now = Instant.now(clock);
        Instant expiresAt = instantClaim(claims, "exp");
        if (expiresAt == null || !expiresAt.isAfter(now)) {
            throw new JwtValidationException("JWT expirado");
        }

        Instant notBefore = instantClaim(claims, "nbf");
        if (notBefore != null && notBefore.isAfter(now)) {
            throw new JwtValidationException("JWT ainda nao e valido");
        }
    }

    private String firstAudience(Map<String, Object> claims) {
        List<String> audiences = audiences(claims);
        return audiences.isEmpty() ? null : audiences.getFirst();
    }

    private List<String> audiences(Map<String, Object> claims) {
        Object audience = claims.get("aud");
        if (audience instanceof String text) {
            return List.of(text);
        }
        if (audience instanceof Collection<?> values) {
            return values.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    private Set<String> scopes(Map<String, Object> claims) {
        Set<String> result = new LinkedHashSet<>();
        Object scope = claims.get("scope");
        if (scope instanceof String text && !text.isBlank()) {
            result.addAll(Arrays.asList(text.split("\\s+")));
        }

        Object scopes = claims.get("scopes");
        if (scopes instanceof String text && !text.isBlank()) {
            result.addAll(Arrays.asList(text.split("\\s+")));
        } else if (scopes instanceof Collection<?> values) {
            values.stream().map(Object::toString).forEach(result::add);
        }
        return result;
    }

    private String stringClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        return value == null ? null : value.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Instant instantClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value instanceof Number number) {
            return Instant.ofEpochSecond(number.longValue());
        }
        if (value instanceof String text && !text.isBlank()) {
            return Instant.ofEpochSecond(Long.parseLong(text));
        }
        return null;
    }
}
