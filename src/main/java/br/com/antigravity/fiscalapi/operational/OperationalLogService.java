package br.com.antigravity.fiscalapi.operational;

import jakarta.servlet.http.HttpServletRequest;
import br.com.antigravity.fiscalapi.shared.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationalLogService {

    private final OperationalLogRepository operationalLogRepository;

    public OperationalLogService(OperationalLogRepository operationalLogRepository) {
        this.operationalLogRepository = operationalLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRequest(HttpServletRequest request, int statusCode, long durationMs) {
        String errorMessage = attributeAsString(request, OperationalRequestContext.ERROR_MESSAGE_ATTRIBUTE);
        OperationalLogLevel level = level(statusCode);
        String eventType = statusCode >= 400 ? "API_REQUEST_FAILED" : "API_REQUEST_COMPLETED";
        String message = statusCode >= 400
            ? "Requisicao finalizada com erro: " + nullToDefault(errorMessage, "sem detalhe informado")
            : "Requisicao finalizada com sucesso";

        record(new OperationalLogRecord(
            OperationalRequestContext.requestId(request),
            level,
            eventType,
            request.getMethod(),
            request.getRequestURI(),
            statusCode,
            durationMs,
            attributeAsUuid(request, OperationalRequestContext.COMPANY_ID_ATTRIBUTE),
            attributeAsUuid(request, OperationalRequestContext.DOCUMENT_ID_ATTRIBUTE),
            attributeAsString(request, OperationalRequestContext.EXTERNAL_REFERENCE_ATTRIBUTE),
            message,
            attributeAsString(request, OperationalRequestContext.ERROR_CODE_ATTRIBUTE)
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(OperationalLogRecord record) {
        operationalLogRepository.save(OperationalLogEvent.create(record));
    }

    @Transactional(readOnly = true)
    public List<OperationalLogResponse> list(Integer limit,
                                             OperationalLogLevel level,
                                             UUID companyId,
                                             UUID documentId) {
        PageRequest page = PageRequest.of(0, sanitizeLimit(limit));
        List<OperationalLogEvent> events;
        if (documentId != null) {
            events = operationalLogRepository.findByDocumentIdOrderByCreatedAtDesc(documentId, page);
        } else if (companyId != null) {
            events = operationalLogRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, page);
        } else if (level != null) {
            events = operationalLogRepository.findByLevelOrderByCreatedAtDesc(level, page);
        } else {
            events = operationalLogRepository.findAllByOrderByCreatedAtDesc(page);
        }
        return events.stream().map(OperationalLogResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OperationalLogResponse getByRequestId(String requestId) {
        return operationalLogRepository.findByRequestId(requestId)
            .map(OperationalLogResponse::from)
            .orElseThrow(() -> new NotFoundException("Log operacional nao encontrado para o request id informado"));
    }

    private OperationalLogLevel level(int statusCode) {
        if (statusCode >= 500) {
            return OperationalLogLevel.ERROR;
        }
        if (statusCode >= 400) {
            return OperationalLogLevel.WARN;
        }
        return OperationalLogLevel.INFO;
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null) {
            return 100;
        }
        return Math.min(Math.max(limit, 1), 500);
    }

    private UUID attributeAsUuid(HttpServletRequest request, String attributeName) {
        Object value = request.getAttribute(attributeName);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String text && !text.isBlank()) {
            return UUID.fromString(text);
        }
        return null;
    }

    private String attributeAsString(HttpServletRequest request, String attributeName) {
        Object value = request.getAttribute(attributeName);
        return value == null ? null : value.toString();
    }

    private String nullToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
