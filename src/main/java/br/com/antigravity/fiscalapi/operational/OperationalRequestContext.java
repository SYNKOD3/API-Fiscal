package br.com.antigravity.fiscalapi.operational;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

public final class OperationalRequestContext {

    public static final String REQUEST_ID_ATTRIBUTE = "fiscalApi.requestId";
    public static final String COMPANY_ID_ATTRIBUTE = "fiscalApi.companyId";
    public static final String DOCUMENT_ID_ATTRIBUTE = "fiscalApi.documentId";
    public static final String EXTERNAL_REFERENCE_ATTRIBUTE = "fiscalApi.externalReference";
    public static final String ERROR_CODE_ATTRIBUTE = "fiscalApi.errorCode";
    public static final String ERROR_MESSAGE_ATTRIBUTE = "fiscalApi.errorMessage";

    private OperationalRequestContext() {
    }

    public static String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        return requestId == null ? null : requestId.toString();
    }

    public static void attachFiscalDocument(HttpServletRequest request,
                                            UUID companyId,
                                            UUID documentId,
                                            String externalReference) {
        request.setAttribute(COMPANY_ID_ATTRIBUTE, companyId);
        request.setAttribute(DOCUMENT_ID_ATTRIBUTE, documentId);
        request.setAttribute(EXTERNAL_REFERENCE_ATTRIBUTE, externalReference);
    }

    public static void attachCompany(HttpServletRequest request, UUID companyId) {
        request.setAttribute(COMPANY_ID_ATTRIBUTE, companyId);
    }

    public static void attachError(HttpServletRequest request, String code, String message) {
        request.setAttribute(ERROR_CODE_ATTRIBUTE, code);
        request.setAttribute(ERROR_MESSAGE_ATTRIBUTE, message);
    }
}
