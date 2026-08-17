package br.com.antigravity.fiscalapi.operational;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationalLogRepository extends JpaRepository<OperationalLogEvent, UUID> {
    Optional<OperationalLogEvent> findByRequestId(String requestId);

    List<OperationalLogEvent> findByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);

    List<OperationalLogEvent> findByDocumentIdOrderByCreatedAtDesc(UUID documentId, Pageable pageable);

    List<OperationalLogEvent> findByLevelOrderByCreatedAtDesc(OperationalLogLevel level, Pageable pageable);

    List<OperationalLogEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
