package br.com.antigravity.fiscalapi.operational;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operational-logs")
@Tag(name = "Logs operacionais")
public class OperationalLogController {

    private final OperationalLogService operationalLogService;

    public OperationalLogController(OperationalLogService operationalLogService) {
        this.operationalLogService = operationalLogService;
    }

    @GetMapping
    @Operation(
        summary = "Listar logs operacionais",
        description = "Consulta requisicoes, erros e tratativas tecnicas registradas pela API. Nao armazena API key, CSC, senha de certificado ou payload fiscal bruto."
    )
    public ApiEnvelope<List<OperationalLogResponse>> list(@RequestParam(required = false) Integer limit,
                                                          @RequestParam(required = false) OperationalLogLevel level,
                                                          @RequestParam(required = false) UUID companyId,
                                                          @RequestParam(required = false) UUID documentId) {
        return ApiEnvelope.of(operationalLogService.list(limit, level, companyId, documentId));
    }

    @GetMapping("/requests/{requestId}")
    @Operation(
        summary = "Consultar log por request id",
        description = "Busca o log operacional gerado para uma requisicao especifica."
    )
    public ApiEnvelope<OperationalLogResponse> getByRequestId(@PathVariable String requestId) {
        return ApiEnvelope.of(operationalLogService.getByRequestId(requestId));
    }

    public record ApiEnvelope<T>(T data) {
        public static <T> ApiEnvelope<T> of(T data) {
            return new ApiEnvelope<>(data);
        }
    }
}
