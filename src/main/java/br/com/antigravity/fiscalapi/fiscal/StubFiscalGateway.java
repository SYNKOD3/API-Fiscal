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

        return new FiscalSubmissionResult(auth, key, receipt);
    }
}
