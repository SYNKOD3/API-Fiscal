package br.com.antigravity.fiscalapi.document;

import br.com.antigravity.fiscalapi.operational.OperationalRequestContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documentos fiscais")
public class FiscalDocumentController {

    private final FiscalDocumentService documentService;

    public FiscalDocumentController(FiscalDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Emitir documento fiscal",
        description = "Emite NF-e ou NFC-e para uma empresa. Se o provedor fiscal estiver indisponivel, gera contingencia local com chave, XML e comprovante."
    )
    public ApiEnvelope<DocumentResponse> issue(@Valid @RequestBody IssueDocumentRequest request,
                                               HttpServletRequest servletRequest) {
        DocumentResponse response = documentService.issue(request);
        OperationalRequestContext.attachFiscalDocument(
            servletRequest,
            response.companyId(),
            response.id(),
            response.externalReference()
        );
        return ApiEnvelope.of(response);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Consultar documento fiscal",
        description = "Busca os dados de uma NF-e/NFC-e pelo identificador interno."
    )
    public ApiEnvelope<DocumentResponse> getById(@PathVariable UUID id, HttpServletRequest servletRequest) {
        DocumentResponse response = documentService.getById(id);
        OperationalRequestContext.attachFiscalDocument(
            servletRequest,
            response.companyId(),
            response.id(),
            response.externalReference()
        );
        return ApiEnvelope.of(response);
    }

    @GetMapping
    @Operation(
        summary = "Listar documentos por empresa",
        description = "Lista os documentos fiscais emitidos ou pendentes de uma empresa."
    )
    public ApiEnvelope<List<DocumentResponse>> list(@RequestParam UUID companyId) {
        return ApiEnvelope.of(documentService.list(companyId));
    }

    @PostMapping("/{id}/retry")
    @Operation(
        summary = "Reprocessar documento fiscal",
        description = "Tenta reenviar para a SEFAZ um documento que ficou em contingencia ou com retry agendado."
    )
    public ApiEnvelope<DocumentResponse> retry(@PathVariable UUID id, HttpServletRequest servletRequest) {
        DocumentResponse response = documentService.retry(id);
        OperationalRequestContext.attachFiscalDocument(
            servletRequest,
            response.companyId(),
            response.id(),
            response.externalReference()
        );
        return ApiEnvelope.of(response);
    }

    @GetMapping(value = "/{id}/receipt", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
        summary = "Baixar comprovante",
        description = "Retorna o comprovante local ou autorizado em texto simples."
    )
    public ResponseEntity<String> receipt(@PathVariable UUID id) {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(documentService.getReceipt(id));
    }

    @GetMapping(value = "/{id}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(
        summary = "Baixar XML fiscal",
        description = "Retorna o XML fiscal armazenado para auditoria, contingencia ou integracao."
    )
    public ResponseEntity<String> xml(@PathVariable UUID id) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(documentService.getFiscalXml(id));
    }

    @GetMapping(value = "/{id}/print", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(
        summary = "Visualizar documento imprimivel",
        description = "Retorna uma visualizacao HTML simples para impressao ou conferencia interna do documento fiscal."
    )
    public ResponseEntity<String> print(@PathVariable UUID id) {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(documentService.getPrintableDocument(id));
    }

    public record ApiEnvelope<T>(T data) {
        public static <T> ApiEnvelope<T> of(T data) {
            return new ApiEnvelope<>(data);
        }
    }
}
