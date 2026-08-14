package br.com.antigravity.fiscalapi.audit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Auditoria fiscal")
public class FiscalAuditController {

    private final FiscalAuditService auditService;

    public FiscalAuditController(FiscalAuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/companies/{companyId}")
    @Operation(
        summary = "Listar eventos de auditoria por empresa",
        description = "Retorna os eventos fiscais registrados para uma empresa emissora."
    )
    public ApiEnvelope<List<FiscalAuditEventResponse>> listByCompany(@PathVariable UUID companyId) {
        return ApiEnvelope.of(auditService.listByCompany(companyId));
    }

    @GetMapping("/documents/{documentId}")
    @Operation(
        summary = "Listar eventos de auditoria por documento",
        description = "Retorna os eventos fiscais registrados para uma NF-e/NFC-e."
    )
    public ApiEnvelope<List<FiscalAuditEventResponse>> listByDocument(@PathVariable UUID documentId) {
        return ApiEnvelope.of(auditService.listByDocument(documentId));
    }

    public record ApiEnvelope<T>(T data) {
        public static <T> ApiEnvelope<T> of(T data) {
            return new ApiEnvelope<>(data);
        }
    }
}
