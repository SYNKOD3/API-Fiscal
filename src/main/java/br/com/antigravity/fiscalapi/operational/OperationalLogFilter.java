package br.com.antigravity.fiscalapi.operational;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OperationalLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OperationalLogFilter.class);
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private final OperationalLogService operationalLogService;

    public OperationalLogFilter(OperationalLogService operationalLogService) {
        this.operationalLogService = operationalLogService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = requestId(request);
        long startedAt = System.nanoTime();
        request.setAttribute(OperationalRequestContext.REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put("requestId", requestId);

        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            OperationalRequestContext.attachError(request, "unhandled_exception", ex.getMessage());
            throw ex;
        } finally {
            if (shouldLog(request)) {
                long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
                try {
                    operationalLogService.recordRequest(request, response.getStatus(), durationMs);
                } catch (RuntimeException logException) {
                    log.warn("Falha ao gravar log operacional do request {}", requestId, logException);
                }
            }
            MDC.remove("requestId");
        }
    }

    private String requestId(HttpServletRequest request) {
        String header = request.getHeader(REQUEST_ID_HEADER);
        if (header == null || header.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return header.length() > 64 ? header.substring(0, 64) : header;
    }

    private boolean shouldLog(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }
}
