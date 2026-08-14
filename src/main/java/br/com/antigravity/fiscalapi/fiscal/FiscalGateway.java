package br.com.antigravity.fiscalapi.fiscal;

public interface FiscalGateway {
    boolean isAvailable(String companyTaxId);

    FiscalSubmissionResult submit(FiscalSubmission submission);
}
