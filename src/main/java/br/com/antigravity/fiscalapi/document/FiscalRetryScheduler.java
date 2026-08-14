package br.com.antigravity.fiscalapi.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FiscalRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(FiscalRetryScheduler.class);
    private final FiscalDocumentService documentService;

    public FiscalRetryScheduler(FiscalDocumentService documentService) {
        this.documentService = documentService;
    }

    @Scheduled(fixedDelayString = "${app.retry.fixed-delay-ms:30000}")
    public void retryPendingDocuments() {
        int processed = documentService.retryPendingDocuments();
        if (processed > 0) {
            log.info("Reprocessados {} documentos em contingencia", processed);
        }
    }
}
