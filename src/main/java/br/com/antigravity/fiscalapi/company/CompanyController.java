package br.com.antigravity.fiscalapi.company;

import br.com.antigravity.fiscalapi.operational.OperationalRequestContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies")
@Tag(name = "Empresas")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Cadastrar empresa emissora",
        description = "Cria uma empresa com dados fiscais, numeracao, ambiente, certificado e CSC para emissao de NF-e/NFC-e."
    )
    public ApiEnvelope<CompanyResponse> create(@Valid @RequestBody CreateCompanyRequest request,
                                               HttpServletRequest servletRequest) {
        CompanyResponse response = companyService.create(request);
        OperationalRequestContext.attachCompany(servletRequest, response.id());
        return ApiEnvelope.of(response);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Atualizar empresa emissora",
        description = "Atualiza dados fiscais, numeração, ambiente, CSC e callback de certificado de uma empresa emissora ja cadastrada."
    )
    public ApiEnvelope<CompanyResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody CreateCompanyRequest request,
                                               HttpServletRequest servletRequest) {
        CompanyResponse response = companyService.update(id, request);
        OperationalRequestContext.attachCompany(servletRequest, response.id());
        return ApiEnvelope.of(response);
    }

    @PatchMapping("/{id}/ibs-cbs")
    @Operation(
        summary = "Informar CST e classificação tributária do IBS/CBS",
        description = "Grava apenas os dois códigos do IBS/CBS, sem exigir o cadastro completo da empresa. "
            + "Eles são definidos pelo contador e determinam o tratamento tributário da venda: sem eles a "
            + "SEFAZ recusa a nota com 1115, e com o código errado ela autoriza um documento incorreto."
    )
    public ApiEnvelope<CompanyResponse> updateIbsCbs(@PathVariable UUID id,
                                                     @Valid @RequestBody UpdateIbsCbsRequest request,
                                                     HttpServletRequest servletRequest) {
        CompanyResponse response = companyService.updateIbsCbs(id, request);
        OperationalRequestContext.attachCompany(servletRequest, response.id());
        return ApiEnvelope.of(response);
    }

    @GetMapping
    @Operation(
        summary = "Listar empresas emissoras",
        description = "Retorna todas as empresas cadastradas para emissao fiscal multiempresa."
    )
    public ApiEnvelope<List<CompanyResponse>> list(@RequestParam(required = false) String tenantId) {
        return ApiEnvelope.of(companyService.list(tenantId));
    }

    public record ApiEnvelope<T>(T data) {
        public static <T> ApiEnvelope<T> of(T data) {
            return new ApiEnvelope<>(data);
        }
    }
}
