package br.com.antigravity.fiscalapi.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalAuditEventRepository extends JpaRepository<FiscalAuditEvent, UUID> {
    List<FiscalAuditEvent> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    List<FiscalAuditEvent> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}
