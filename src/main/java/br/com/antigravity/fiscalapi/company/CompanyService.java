package br.com.antigravity.fiscalapi.company;

import br.com.antigravity.fiscalapi.audit.FiscalAuditService;
import br.com.antigravity.fiscalapi.shared.ConflictException;
import br.com.antigravity.fiscalapi.shared.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final FiscalAuditService auditService;

    public CompanyService(CompanyRepository companyRepository, FiscalAuditService auditService) {
        this.companyRepository = companyRepository;
        this.auditService = auditService;
    }

    @Transactional
    public CompanyResponse create(CreateCompanyRequest request) {
        companyRepository.findByTaxId(request.taxId()).ifPresent(existing -> {
            throw new ConflictException("Empresa ja cadastrada para este CNPJ");
        });

        Company company = Company.create(
            request.legalName(),
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
            request.nfeSeriesNumber(),
            request.nextNfeNumber(),
            request.nfceSeriesNumber(),
            request.nextNfceNumber()
        );
        Company saved = companyRepository.save(company);
        auditService.record(saved.getId(), null, "COMPANY_CREATED", "Empresa emissora cadastrada", saved.getTaxId());
        return CompanyResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> list() {
        return companyRepository.findAll().stream().map(CompanyResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Company getById(UUID id) {
        return companyRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Empresa nao encontrada"));
    }
}
