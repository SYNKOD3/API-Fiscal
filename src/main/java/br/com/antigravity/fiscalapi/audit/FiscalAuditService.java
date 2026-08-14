package br.com.antigravity.fiscalapi.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FiscalAuditService {

    private final FiscalAuditEventRepository auditEventRepository;

    public FiscalAuditService(FiscalAuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID companyId, UUID documentId, String eventType, String message, String details) {
        auditEventRepository.save(FiscalAuditEvent.create(companyId, documentId, eventType, message, details));
    }

    @Transactional(readOnly = true)
    public List<FiscalAuditEventResponse> listByCompany(UUID companyId) {
        return auditEventRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
            .map(FiscalAuditEventResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<FiscalAuditEventResponse> listByDocument(UUID documentId) {
        return auditEventRepository.findByDocumentIdOrderByCreatedAtDesc(documentId).stream()
            .map(FiscalAuditEventResponse::from)
            .toList();
    }
}
