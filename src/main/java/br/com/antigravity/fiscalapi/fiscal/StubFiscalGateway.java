package br.com.antigravity.fiscalapi.fiscal;

import br.com.antigravity.fiscalapi.config.AppProperties;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class StubFiscalGateway implements FiscalGateway {

    private final AppProperties properties;
    private final FiscalXmlBuilder fiscalXmlBuilder;

    public StubFiscalGateway(AppProperties properties, FiscalXmlBuilder fiscalXmlBuilder) {
        this.properties = properties;
        this.fiscalXmlBuilder = fiscalXmlBuilder;
    }

    @Override
    public boolean isAvailable(FiscalSubmission submission) {
        return properties.getFiscal().isStubOnline()
            && properties.getFiscal().getUnavailableStates().stream()
            .map(state -> state.trim().toUpperCase())
            .noneMatch(submission.companyStateCode()::equals);
    }

    /**
     * O stub aceita o cancelamento sem falar com a SEFAZ.
     *
     * Ele existe para o fluxo inteiro ser exercitado sem certificado, e o
     * cancelamento faz parte do fluxo. O protocolo devolvido e sintetico e a
     * mensagem diz isso: um cancelamento simulado passando por real seria pior
     * do que nao ter.
     *
     * O prazo de 30 minutos nao e conferido aqui de proposito — quem o impoe e
     * a SEFAZ, e reproduzir a regra no stub criaria uma segunda fonte da
     * verdade que envelheceria sozinha.
     */
    @Override
    public FiscalCancellationResult cancel(java.util.UUID companyId, FiscalCancellation cancellation) {
        return new FiscalCancellationResult(
            "135" + System.currentTimeMillis(),
            "Cancelamento simulado, sem valor fiscal: o provedor de teste nao fala com a SEFAZ."
        );
    }

    @Override
    public FiscalSubmissionResult submit(FiscalSubmission submission) {
        if (!isAvailable(submission)) {
            throw new FiscalGatewayException("Stub fiscal configurado como offline", true);
        }

        FiscalXmlDraft draft = fiscalXmlBuilder.build(submission);
        String auth = "AUT-" + submission.externalReference();
        String key = draft.accessKey();
        String receipt = """
            CUPOM AUTORIZADO
            Modelo: %s
            Chave de acesso: %s
            Referencia: %s
            Serie/Numero: %s/%s
            Cliente: %s
            Itens: %s
            Valor total: %s
            Autorizado em: %s
            """.formatted(
            submission.model(),
            key,
            submission.externalReference(),
            submission.seriesNumber(),
            submission.invoiceNumber(),
            submission.customerName(),
            submission.items().size(),
            submission.totalAmount(),
            OffsetDateTime.now()
        );

        // O stub nao fala com a SEFAZ e nao monta QR Code: nulo aqui e a
        // verdade, e a tela mostra que o documento esta sem.
        return new FiscalSubmissionResult(auth, key, receipt, null);
    }
}
