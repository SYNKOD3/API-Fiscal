package br.com.antigravity.fiscalapi.company;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CreateCompanyRequest(
    @Schema(description = "Identificador do tenant na plataforma integradora.", example = "tenant-dev")
    String tenantId,
    @Schema(description = "Identificador do lojista/empresa na plataforma integradora.", example = "merchant-dev")
    String merchantId,
    @Schema(description = "URL de callback para eventos fiscais.", example = "https://integrador.example.com/webhooks/fiscal")
    String callbackUrl,
    @Schema(description = "Razão social da empresa emissora.", example = "Empresa Exemplo LTDA")
    @NotBlank String legalName,
    @Schema(description = "CNPJ da empresa emissora, somente números.", example = "12345678000199")
    @NotBlank @Pattern(regexp = "\\d{14}") String taxId,
    @Schema(description = "Inscrição estadual da empresa emissora.", example = "123456789")
    @NotBlank String stateRegistration,
    @Schema(description = "UF da empresa emissora.", example = "SP")
    @NotBlank @Pattern(regexp = "[A-Z]{2}") String stateCode,
    @Schema(description = "Nome fantasia da empresa emissora.", example = "Empresa Exemplo")
    String tradeName,
    @Schema(description = "Logradouro.", example = "Rua Fiscal")
    @NotBlank String street,
    @Schema(description = "Número do endereço.", example = "100")
    @NotBlank String addressNumber,
    @Schema(description = "Complemento do endereço.", example = "Sala 01")
    String addressComplement,
    @Schema(description = "Bairro.", example = "Centro")
    @NotBlank String district,
    @Schema(description = "Código IBGE do município.", example = "3550308")
    @NotBlank @Pattern(regexp = "\\d{7}") String cityCode,
    @Schema(description = "Nome do município.", example = "São Paulo")
    @NotBlank String cityName,
    @Schema(description = "CEP, somente números.", example = "01001000")
    @NotBlank @Pattern(regexp = "\\d{8}") String zipCode,
    @Schema(description = "Telefone de contato.", example = "1133334444")
    String phone,
    @Schema(description = "Regime tributário.", example = "SIMPLES_NACIONAL")
    TaxRegime taxRegime,
    @Schema(description = "Ambiente fiscal.", example = "HOMOLOGATION")
    FiscalEnvironment fiscalEnvironment,
    @Schema(description = "Caminho interno legado do certificado. Prefira o endpoint de upload.", example = "")
    String certificatePath,
    @Schema(description = "Senha do certificado para fluxo legado. Prefira o endpoint de upload.", example = "")
    String certificatePassword,
    @Schema(description = "ID do CSC/token para NFC-e, quando aplicável.", example = "000001")
    String cscId,
    @Schema(description = "CSC/token para NFC-e, quando aplicável.", example = "token-csc")
    String cscToken,
    @Schema(description = "Série da NF-e.", example = "1")
    @Positive Integer nfeSeriesNumber,
    @Schema(description = "Próximo número de NF-e.", example = "1")
    @Positive Long nextNfeNumber,
    @Schema(description = "Série da NFC-e.", example = "1")
    @Positive Integer nfceSeriesNumber,
    @Schema(description = "Próximo número de NFC-e.", example = "1")
    @Positive Long nextNfceNumber
) {
}
