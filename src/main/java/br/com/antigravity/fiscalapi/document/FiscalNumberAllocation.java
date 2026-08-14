package br.com.antigravity.fiscalapi.document;

import br.com.antigravity.fiscalapi.company.Company;
import br.com.antigravity.fiscalapi.company.FiscalNumber;

public record FiscalNumberAllocation(
    Company company,
    FiscalNumber fiscalNumber
) {
}
