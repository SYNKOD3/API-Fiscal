package br.com.antigravity.fiscalapi.sefaz;

import br.com.antigravity.fiscalapi.company.CompanyService;
import br.com.antigravity.fiscalapi.document.DocumentModel;
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
@RequestMapping("/api/v1/sefaz")
@Tag(name = "Roteamento SEFAZ")
public class SefazController {

    private final SefazRouter sefazRouter;
    private final CompanyService companyService;

    public SefazController(SefazRouter sefazRouter, CompanyService companyService) {
        this.sefazRouter = sefazRouter;
        this.companyService = companyService;
    }

    @GetMapping("/states")
    @Operation(
        summary = "Listar UFs fiscais suportadas",
        description = "Lista as UFs tratadas pela API Fiscal. O endpoint/autorizador real e resolvido pela Java-NFe conforme a UF da empresa emitente."
    )
    public ApiEnvelope<List<SefazStateResponse>> states() {
        return ApiEnvelope.of(sefazRouter.states());
    }

    @GetMapping("/companies/{companyId}/route")
    @Operation(
        summary = "Consultar rota SEFAZ por empresa",
        description = "Mostra qual estrategia sera usada para transmitir NF-e/NFC-e de uma empresa emissora."
    )
    public ApiEnvelope<SefazRouteResponse> routeByCompany(@PathVariable UUID companyId,
                                                          @RequestParam DocumentModel model) {
        return ApiEnvelope.of(sefazRouter.route(companyService.getById(companyId), model));
    }

    @GetMapping("/bivaro-route")
    @Operation(
        summary = "Consultar rota SEFAZ por lojista Bivaro",
        description = "Mostra a rota fiscal de um lojista usando os identificadores do Bivaro, sem exigir o UUID interno da API Fiscal."
    )
    public ApiEnvelope<SefazRouteResponse> routeByBivaroMerchant(@RequestParam String bivaroTenantId,
                                                                 @RequestParam String bivaroMerchantId,
                                                                 @RequestParam DocumentModel model) {
        return ApiEnvelope.of(sefazRouter.route(
            companyService.getByBivaroMerchant(bivaroTenantId, bivaroMerchantId),
            model
        ));
    }

    public record ApiEnvelope<T>(T data) {
        public static <T> ApiEnvelope<T> of(T data) {
            return new ApiEnvelope<>(data);
        }
    }
}
