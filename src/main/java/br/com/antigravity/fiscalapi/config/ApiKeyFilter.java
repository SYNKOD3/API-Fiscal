package br.com.antigravity.fiscalapi.config;

import br.com.antigravity.fiscalapi.operational.OperationalRequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-API-Key";
    private final AppProperties properties;

    public ApiKeyFilter(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !properties.getSecurity().isApiKeyEnabled()
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
        String apiKey = request.getHeader(HEADER_NAME);
        if (!properties.getSecurity().getApiKey().equals(apiKey)) {
            OperationalRequestContext.attachError(request, "unauthorized", "API key invalida");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                {"error":{"code":"unauthorized","message":"API key invalida","requestId":"%s"}}"""
                .formatted(OperationalRequestContext.requestId(request)));
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            var authentication = new UsernamePasswordAuthenticationToken("api-client", null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }
}
