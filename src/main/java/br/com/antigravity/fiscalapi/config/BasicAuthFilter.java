package br.com.antigravity.fiscalapi.config;

import br.com.antigravity.fiscalapi.operational.OperationalRequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BasicAuthFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BASIC_PREFIX = "Basic ";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AppProperties properties;

    public BasicAuthFilter(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !properties.getSecurity().isBasicAuthEnabled()
            || path.startsWith("/actuator/health")
            || path.equals("/api/v1/auth/token")
            || (properties.getDevConsole().isEnabled() && path.startsWith("/dev"))
            || (properties.getOpenApi().isPublicAccess()
                && (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (isBearerDelegatedToJwt(authorization)) {
            filterChain.doFilter(request, response);
            return;
        }

        Credentials credentials = credentials(authorization);
        if (credentials == null || !isValid(credentials)) {
            unauthorized(request, response);
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
            credentials.username(),
            null,
            List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private boolean isBearerDelegatedToJwt(String authorization) {
        return properties.getSecurity().getJwt().isEnabled()
            && authorization != null
            && authorization.startsWith(BEARER_PREFIX);
    }

    private Credentials credentials(String authorization) {
        if (authorization == null || !authorization.startsWith(BASIC_PREFIX)) {
            return null;
        }

        try {
            String decoded = new String(
                Base64.getDecoder().decode(authorization.substring(BASIC_PREFIX.length()).trim()),
                StandardCharsets.UTF_8
            );
            int separator = decoded.indexOf(':');
            if (separator < 0) {
                return null;
            }
            return new Credentials(decoded.substring(0, separator), decoded.substring(separator + 1));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isValid(Credentials credentials) {
        AppProperties.IntegrationClient client = properties.getSecurity().getIntegrationClient();
        return matches(credentials.username(), client.getUsername())
            && matches(credentials.password(), client.getPassword());
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

    private void unauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        OperationalRequestContext.attachError(request, "unauthorized", "Login ou senha invalidos");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader("WWW-Authenticate", "Basic realm=\"Fiscal API\"");
        response.setContentType("application/json");
        response.getWriter().write("""
            {"error":{"code":"unauthorized","message":"Login ou senha invalidos","requestId":"%s"}}"""
            .formatted(OperationalRequestContext.requestId(request)));
    }

    private record Credentials(String username, String password) {
    }
}
