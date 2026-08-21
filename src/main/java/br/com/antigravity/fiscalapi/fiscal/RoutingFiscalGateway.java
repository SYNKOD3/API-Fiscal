package br.com.antigravity.fiscalapi.fiscal;

import br.com.antigravity.fiscalapi.config.AppProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class RoutingFiscalGateway implements FiscalGateway {

    private final AppProperties properties;
    private final StubFiscalGateway stubFiscalGateway;
    private final LibraryFiscalGateway libraryFiscalGateway;

    public RoutingFiscalGateway(AppProperties properties,
                                StubFiscalGateway stubFiscalGateway,
                                LibraryFiscalGateway libraryFiscalGateway) {
        this.properties = properties;
        this.stubFiscalGateway = stubFiscalGateway;
        this.libraryFiscalGateway = libraryFiscalGateway;
    }

    @Override
    public boolean isAvailable(FiscalSubmission submission) {
        return currentGateway().isAvailable(submission);
    }

    @Override
    public FiscalSubmissionResult submit(FiscalSubmission submission) {
        return currentGateway().submit(submission);
    }

    @Override
    public FiscalCancellationResult cancel(java.util.UUID companyId, FiscalCancellation cancellation) {
        return currentGateway().cancel(companyId, cancellation);
    }

    private FiscalGateway currentGateway() {
        return properties.getFiscal().getProvider() == AppProperties.Provider.LIBRARY
            ? libraryFiscalGateway
            : stubFiscalGateway;
    }
}
