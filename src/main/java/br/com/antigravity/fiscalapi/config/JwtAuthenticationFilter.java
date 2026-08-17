package br.com.antigravity.fiscalapi.config;

import br.com.antigravity.fiscalapi.operational.OperationalRequestContext;
import br.com.antigravity.fiscalapi.security.JwtPrincipal;
import br.com.antigravity.fiscalapi.security.JwtTokenService;
import br.com.antigravity.fiscalapi.security.JwtValidationException;
import br.com.antigravity.fiscalapi.shared.ForbiddenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AppProperties properties;
    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(AppProperties properties, JwtTokenService jwtTokenService) {
        this.properties = properties;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !properties.getSecurity().getJwt().isEnabled()
            || path.startsWith("/actuator/health")
            || (properties.getDevConsole().isEnabled() && path.startsWith("/dev"))
            || (properties.getOpenApi().isPublicAccess()
                && (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            JwtPrincipal principal = jwtTokenService.validate(token(request));
            requireScope(request, principal);
            var authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.scopes().stream().map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope)).toList()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtValidationException ex) {
            unauthorized(request, response, ex.getMessage());
        } catch (ForbiddenException ex) {
            forbidden(request, response, ex.getMessage());
        }
    }

    private String token(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new JwtValidationException("JWT ausente no header Authorization");
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    private void requireScope(HttpServletRequest request, JwtPrincipal principal) {
        String requiredScope = requiredScope(request);
        if (requiredScope != null && !principal.hasScope(requiredScope)) {
            throw new ForbiddenException("JWT sem escopo necessario: " + requiredScope);
        }
    }

    private String requiredScope(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/companies") && path.contains("/certificates")) {
            return "POST".equals(method) ? "fiscal:certificates:write" : "fiscal:certificates:read";
        }
        if (path.startsWith("/api/v1/companies")) {
            return "POST".equals(method) ? "fiscal:companies:write" : "fiscal:companies:read";
        }
        if (path.startsWith("/api/v1/documents")) {
            if ("POST".equals(method) && path.endsWith("/retry")) {
                return "fiscal:documents:retry";
            }
            return "POST".equals(method) ? "fiscal:documents:issue" : "fiscal:documents:read";
        }
        if (path.startsWith("/api/v1/audit")) {
            return "fiscal:audit:read";
        }
        if (path.startsWith("/api/v1/operational-logs")) {
            return "fiscal:logs:read";
        }
        if (path.startsWith("/api/v1/sefaz")) {
            return "fiscal:sefaz:read";
        }
        return null;
    }

    private void unauthorized(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        OperationalRequestContext.attachError(request, "unauthorized", message);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("""
            {"error":{"code":"unauthorized","message":"%s","requestId":"%s"}}"""
            .formatted(escapeJson(message), OperationalRequestContext.requestId(request)));
    }

    private void forbidden(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        OperationalRequestContext.attachError(request, "forbidden", message);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write("""
            {"error":{"code":"forbidden","message":"%s","requestId":"%s"}}"""
            .formatted(escapeJson(message), OperationalRequestContext.requestId(request)));
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
