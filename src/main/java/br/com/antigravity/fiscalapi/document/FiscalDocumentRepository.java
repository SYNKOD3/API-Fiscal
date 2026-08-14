package br.com.antigravity.fiscalapi.document;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalDocumentRepository extends JpaRepository<FiscalDocument, UUID> {
    Optional<FiscalDocument> findByCompany_IdAndExternalReference(UUID companyId, String externalReference);

    List<FiscalDocument> findByCompany_Id(UUID companyId);

    List<FiscalDocument> findByStatusInAndNextRetryAtBeforeOrderByCreatedAtAsc(
        Collection<DocumentStatus> statuses,
        OffsetDateTime nextRetryAt,
        Pageable pageable
    );
}
