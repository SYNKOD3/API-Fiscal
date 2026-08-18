package br.com.antigravity.fiscalapi.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
    @Schema(description = "JWT emitido pela API Fiscal para uso opcional.", example = "eyJhbGciOiJIUzI1NiJ9...")
    String accessToken,
    @Schema(description = "Tipo do token.", example = "Bearer")
    String tokenType,
    @Schema(description = "Tempo de validade em segundos.", example = "3600")
    long expiresInSeconds,
    @Schema(description = "Nome do header usado nas chamadas protegidas.", example = "Authorization")
    String headerName,
    @Schema(description = "Valor completo do header Authorization quando JWT estiver ativo.", example = "Bearer eyJhbGciOiJIUzI1NiJ9...")
    String headerValue
) {
}
