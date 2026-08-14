package br.com.antigravity.fiscalapi.fiscal;

public interface FiscalGateway {
    boolean isAvailable(FiscalSubmission submission);

    FiscalSubmissionResult submit(FiscalSubmission submission);
}
