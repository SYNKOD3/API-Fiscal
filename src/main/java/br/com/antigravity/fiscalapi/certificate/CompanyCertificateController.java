package br.com.antigravity.fiscalapi.certificate;

import br.com.antigravity.fiscalapi.operational.OperationalRequestContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/certificates")
@Tag(name = "Certificados digitais")
public class CompanyCertificateController {

    private final CompanyCertificateService certificateService;

    public CompanyCertificateController(CompanyCertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Enviar certificado A1 da empresa",
        description = "Recebe um certificado .pfx/.p12, valida senha/validade/CNPJ quando possivel, salva em storage privado e ativa para emissao fiscal da empresa."
    )
    public ApiEnvelope<CompanyCertificateResponse> upload(@PathVariable UUID companyId,
                                                          @RequestParam("file") MultipartFile file,
                                                          @RequestParam("password") String password,
                                                          HttpServletRequest servletRequest) {
        CompanyCertificateResponse response = certificateService.upload(companyId, file, password);
        OperationalRequestContext.attachCompany(servletRequest, companyId);
        return ApiEnvelope.of(response);
    }

    @GetMapping
    @Operation(
        summary = "Listar certificados da empresa",
        description = "Retorna o historico de certificados enviados para uma empresa, sem expor senha nem caminho interno do arquivo."
    )
    public ApiEnvelope<List<CompanyCertificateResponse>> list(@PathVariable UUID companyId,
                                                              HttpServletRequest servletRequest) {
        OperationalRequestContext.attachCompany(servletRequest, companyId);
        return ApiEnvelope.of(certificateService.list(companyId));
    }

    public record ApiEnvelope<T>(T data) {
        public static <T> ApiEnvelope<T> of(T data) {
            return new ApiEnvelope<>(data);
        }
    }
}
