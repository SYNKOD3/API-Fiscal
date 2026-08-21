package br.com.antigravity.fiscalapi.fiscal;

public interface FiscalGateway {
    boolean isAvailable(FiscalSubmission submission);

    FiscalSubmissionResult submit(FiscalSubmission submission);

    /**
     * Cancela um documento ja autorizado, pelo evento 110111.
     *
     * O prazo e da SEFAZ, nao nosso: 30 minutos da autorizacao para a NFC-e,
     * por forca do Ajuste SINIEF 19/2016. Passado isso ela recusa, e o
     * caminho vira o cancelamento extemporaneo, que e processo manual.
     */
    FiscalCancellationResult cancel(java.util.UUID companyId, FiscalCancellation cancellation);
}
