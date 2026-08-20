package br.com.antigravity.fiscalapi.company;

import br.com.antigravity.fiscalapi.audit.FiscalAuditService;
import br.com.antigravity.fiscalapi.certificate.CertificateStatus;
import br.com.antigravity.fiscalapi.certificate.CompanyCertificateRepository;
import br.com.antigravity.fiscalapi.security.JwtSecurityContext;
import br.com.antigravity.fiscalapi.shared.ConflictException;
import br.com.antigravity.fiscalapi.shared.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyCertificateRepository certificateRepository;
    private final FiscalAuditService auditService;
    private final JwtSecurityContext jwtSecurityContext;

    public CompanyService(CompanyRepository companyRepository,
                          CompanyCertificateRepository certificateRepository,
                          FiscalAuditService auditService,
                          JwtSecurityContext jwtSecurityContext) {
        this.companyRepository = companyRepository;
        this.certificateRepository = certificateRepository;
        this.auditService = auditService;
        this.jwtSecurityContext = jwtSecurityContext;
    }

    @Transactional
    public CompanyResponse create(CreateCompanyRequest request) {
        jwtSecurityContext.requireTenantAccess(request.tenantId(), request.merchantId());
        companyRepository.findByTaxId(request.taxId()).ifPresent(existing -> {
            throw new ConflictException("Empresa ja cadastrada para este CNPJ");
        });

        if (hasText(request.tenantId()) && hasText(request.merchantId())) {
            companyRepository.findByTenantIdAndMerchantId(
                request.tenantId(),
                request.merchantId()
            ).ifPresent(existing -> {
                throw new ConflictException("Merchant já vinculado a outra empresa emissora");
            });
        }

        Company company = Company.create(
            request.legalName(),
            request.tenantId(),
            request.merchantId(),
            request.callbackUrl(),
            request.taxId(),
            request.stateRegistration(),
            request.stateCode(),
            request.tradeName(),
            request.street(),
            request.addressNumber(),
            request.addressComplement(),
            request.district(),
            request.cityCode(),
            request.cityName(),
            request.zipCode(),
            request.phone(),
            request.taxRegime(),
            request.fiscalEnvironment(),
            request.certificatePath(),
            request.certificatePassword(),
            request.cscId(),
            request.cscToken(),
            request.ibsCbsCst(),
            request.ibsCbsClassTrib(),
            request.nfeSeriesNumber(),
            request.nextNfeNumber(),
            request.nfceSeriesNumber(),
            request.nextNfceNumber()
        );
        Company saved = companyRepository.save(company);
        auditService.record(saved.getId(), null, "COMPANY_CREATED", "Empresa emissora cadastrada", saved.getTaxId());
        return response(saved);
    }

    @Transactional
    public CompanyResponse update(UUID id, CreateCompanyRequest request) {
        Company company = companyRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new NotFoundException("Empresa nao encontrada"));
        jwtSecurityContext.requireCompanyAccess(company);
        jwtSecurityContext.requireTenantAccess(request.tenantId(), request.merchantId());

        companyRepository.findByTaxId(request.taxId())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new ConflictException("Empresa ja cadastrada para este CNPJ");
            });

        if (hasText(request.tenantId()) && hasText(request.merchantId())) {
            companyRepository.findByTenantIdAndMerchantId(
                request.tenantId(),
                request.merchantId()
            ).filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Merchant já vinculado a outra empresa emissora");
                });
        }

        company.updateFiscalData(
            request.legalName(),
            request.tenantId(),
            request.merchantId(),
            request.callbackUrl(),
            request.taxId(),
            request.stateRegistration(),
            request.stateCode(),
            request.tradeName(),
            request.street(),
            request.addressNumber(),
            request.addressComplement(),
            request.district(),
            request.cityCode(),
            request.cityName(),
            request.zipCode(),
            request.phone(),
            request.taxRegime(),
            request.fiscalEnvironment(),
            request.cscId(),
            request.cscToken(),
            request.ibsCbsCst(),
            request.ibsCbsClassTrib(),
            request.nfeSeriesNumber(),
            request.nextNfeNumber(),
            request.nfceSeriesNumber(),
            request.nextNfceNumber()
        );
        Company saved = companyRepository.save(company);
        auditService.record(saved.getId(), null, "COMPANY_UPDATED", "Empresa emissora atualizada", saved.getTaxId());
        return response(saved);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> list(String tenantId) {
        String constrainedTenant = jwtSecurityContext.constrainedTenant(tenantId);
        return hasText(constrainedTenant)
            ? companyRepository.findByTenantIdOrderByCreatedAtDesc(constrainedTenant).stream().map(this::response).toList()
            : companyRepository.findAll().stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public Company getById(UUID id) {
        Company company = companyRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Empresa nao encontrada"));
        jwtSecurityContext.requireCompanyAccess(company);
        return company;
    }

    @Transactional(readOnly = true)
    public Company getByMerchant(String tenantId, String merchantId) {
        jwtSecurityContext.requireTenantAccess(tenantId, merchantId);
        Company company = companyRepository.findByTenantIdAndMerchantId(tenantId, merchantId)
            .orElseThrow(() -> new NotFoundException("Empresa emissora não encontrada para o merchant informado"));
        jwtSecurityContext.requireCompanyAccess(company);
        return company;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private CompanyResponse response(Company company) {
        boolean managedCertificateConfigured = certificateRepository.existsByCompany_IdAndStatus(
            company.getId(),
            CertificateStatus.ACTIVE
        );
        boolean legacyCertificateConfigured = hasText(company.getCertificatePath());
        return CompanyResponse.from(company, managedCertificateConfigured || legacyCertificateConfigured);
    }
}
