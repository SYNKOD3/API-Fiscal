package br.com.antigravity.fiscalapi.certificate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyCertificateRepository extends JpaRepository<CompanyCertificate, UUID> {
    Optional<CompanyCertificate> findFirstByCompany_IdAndStatusOrderByActivatedAtDesc(UUID companyId, CertificateStatus status);

    List<CompanyCertificate> findByCompany_IdOrderByCreatedAtDesc(UUID companyId);

    boolean existsByCompany_IdAndStatus(UUID companyId, CertificateStatus status);
}
