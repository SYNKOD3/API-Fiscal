package br.com.antigravity.fiscalapi.config;

import br.com.antigravity.fiscalapi.security.JwtTokenService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dev/jwt")
@ConditionalOnProperty(prefix = "app.dev-console", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DevJwtController {

    private static final int DEFAULT_EXPIRES_IN_MINUTES = 60;
    private static final int MAX_EXPIRES_IN_MINUTES = 240;

    private final JwtTokenService jwtTokenService;

    public DevJwtController(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping
    DevJwtResponse issue(@RequestBody(required = false) DevJwtRequest request) {
        DevJwtRequest safeRequest = request == null ? new DevJwtRequest(null, null, null, null, null) : request;
        int expiresInMinutes = expiresInMinutes(safeRequest.expiresInMinutes());
        String token = jwtTokenService.issue(
            safeRequest.subject(),
            safeRequest.tenantId(),
            safeRequest.merchantId(),
            safeRequest.scopes(),
            expiresInMinutes * 60L
        );
        return new DevJwtResponse(token, "Bearer", expiresInMinutes * 60L, "Authorization", "Bearer " + token);
    }

    private int expiresInMinutes(Integer value) {
        if (value == null) {
            return DEFAULT_EXPIRES_IN_MINUTES;
        }
        return Math.max(1, Math.min(value, MAX_EXPIRES_IN_MINUTES));
    }

    record DevJwtRequest(
        String subject,
        String tenantId,
        String merchantId,
        List<String> scopes,
        Integer expiresInMinutes
    ) {
    }

    record DevJwtResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String headerName,
        String headerValue
    ) {
    }
}
