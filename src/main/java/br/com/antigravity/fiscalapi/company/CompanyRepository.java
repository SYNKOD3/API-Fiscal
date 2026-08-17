package br.com.antigravity.fiscalapi.company;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findByTaxId(String taxId);

    Optional<Company> findByTenantIdAndMerchantId(String tenantId, String merchantId);

    List<Company> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Company c where c.id = :id")
    Optional<Company> findByIdForUpdate(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Company c where c.tenantId = :tenantId and c.merchantId = :merchantId")
    Optional<Company> findByTenantIdAndMerchantIdForUpdate(String tenantId, String merchantId);
}
