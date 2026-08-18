package br.com.antigravity.fiscalapi.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/token")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Gerar token de acesso",
        description = "Recebe credenciais da integração e retorna um Bearer token emitido pela própria API Fiscal."
    )
    public ApiEnvelope<TokenResponse> token(@RequestBody(required = false) TokenRequest request,
                                            HttpServletRequest servletRequest) {
        return ApiEnvelope.of(authService.issueToken(request, servletRequest.getHeader("Authorization")));
    }

    public record ApiEnvelope<T>(T data) {
        public static <T> ApiEnvelope<T> of(T data) {
            return new ApiEnvelope<>(data);
        }
    }
}
