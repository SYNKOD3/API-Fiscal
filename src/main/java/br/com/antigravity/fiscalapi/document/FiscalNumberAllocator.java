package br.com.antigravity.fiscalapi.document;

import br.com.antigravity.fiscalapi.company.Company;
import br.com.antigravity.fiscalapi.company.CompanyRepository;
import br.com.antigravity.fiscalapi.company.FiscalNumber;
import br.com.antigravity.fiscalapi.shared.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FiscalNumberAllocator {

    private final CompanyRepository companyRepository;

    public FiscalNumberAllocator(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public FiscalNumberAllocation allocate(UUID companyId, DocumentModel model) {
        Company company = companyRepository.findByIdForUpdate(companyId)
            .orElseThrow(() -> new NotFoundException("Empresa nao encontrada"));

        FiscalNumber fiscalNumber = model == DocumentModel.NFE
            ? company.allocateNfeNumber()
            : company.allocateNfceNumber();

        return new FiscalNumberAllocation(company, fiscalNumber);
    }
}
