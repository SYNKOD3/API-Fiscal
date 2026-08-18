package br.com.antigravity.fiscalapi.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
    @Schema(description = "JWT emitido pela API Fiscal. Cole este valor no Authorize do Swagger.", example = "eyJhbGciOiJIUzI1NiJ9...")
    String accessToken,
    @Schema(description = "Tipo do token.", example = "Bearer")
    String tokenType,
    @Schema(description = "Tempo de validade em segundos.", example = "3600")
    long expiresInSeconds,
    @Schema(description = "Nome do header usado nas chamadas protegidas.", example = "Authorization")
    String headerName,
    @Schema(description = "Valor completo do header Authorization.", example = "Bearer eyJhbGciOiJIUzI1NiJ9...")
    String headerValue
) {
}
