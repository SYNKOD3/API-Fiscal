package br.com.antigravity.fiscalapi.sefaz;

public record SefazStateResponse(
    String stateCode,
    String authorizerStrategy,
    String note
) {
}
