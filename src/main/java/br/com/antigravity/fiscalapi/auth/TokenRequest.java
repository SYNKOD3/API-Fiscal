package br.com.antigravity.fiscalapi.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record TokenRequest(
    @Schema(description = "Usuário da integração configurado em AUTH_USERNAME.", example = "dev-client")
    String username,
    @Schema(description = "Senha da integração configurada em AUTH_PASSWORD.", example = "dev-password-change-me")
    String password,
    @Schema(description = "Alternativa ao campo username para integrações que preferem client credentials.", example = "dev-client")
    String clientId,
    @Schema(description = "Alternativa ao campo password para integrações que preferem client credentials.", example = "dev-password-change-me")
    String clientSecret,
    @Schema(description = "Identificador técnico de quem está solicitando o token.", example = "integrador-backend")
    String subject,
    @Schema(description = "Identificador do tenant na plataforma integradora.", example = "tenant-dev")
    String tenantId,
    @Schema(description = "Identificador do lojista/empresa na plataforma integradora.", example = "merchant-dev")
    String merchantId,
    @Schema(description = "Escopos solicitados para o token. Use fiscal:admin em testes locais.", example = "[\"fiscal:admin\"]")
    List<String> scopes,
    @Schema(description = "Validade desejada em minutos, limitada por AUTH_TOKEN_TTL_MINUTES.", example = "60")
    Integer expiresInMinutes
) {
    static TokenRequest empty() {
        return new TokenRequest(null, null, null, null, null, null, null, null, null);
    }
}
