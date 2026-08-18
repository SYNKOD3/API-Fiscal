package br.com.antigravity.fiscalapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String API_KEY_SCHEME = "Chave da API";
    private static final String JWT_SCHEME = "JWT Integrador";
    private static final String TOKEN_PATH = "/api/v1/auth/token";

    @Bean
    OpenAPI fiscalOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("API Fiscal")
                .version("0.0.1")
                .description("""
                    API para emissão de NF-e e NFC-e em ambiente multiempresa, com contingência local,
                    armazenamento de XML/comprovante e reprocessamento automático quando a SEFAZ voltar.

                    ## Como testar pelo Swagger

                    1. Abra `Autenticação > POST /api/v1/auth/token`.
                    2. Clique em `Try it out` e execute com `username` e `password` da integração.
                    3. Copie o campo `data.accessToken` da resposta.
                    4. Clique no botão `Authorize` no topo do Swagger.
                    5. Em `Chave da API`, informe a API key.
                    6. Em `JWT Integrador`, cole somente o token, sem escrever `Bearer`.
                    7. Execute os endpoints protegidos normalmente pelo próprio Swagger.

                    Em ambiente local, os valores padrão são `change-me`, `dev-client` e `dev-password-change-me`.
                    """)
                .contact(new Contact().name("Antigravity")))
            .addTagsItem(new Tag()
                .name("Autenticação")
                .description("Geração de token Bearer pela própria API Fiscal. Use este endpoint primeiro no Swagger."))
            .addTagsItem(new Tag()
                .name("Empresas")
                .description("Cadastro e consulta de empresas emissoras."))
            .addTagsItem(new Tag()
                .name("Certificados digitais")
                .description("Upload, validação e histórico de certificados A1 por empresa emissora."))
            .addTagsItem(new Tag()
                .name("Documentos fiscais")
                .description("Emissão, consulta, retry, XML e comprovante de NF-e/NFC-e."))
            .addTagsItem(new Tag()
                .name("Auditoria fiscal")
                .description("Rastreabilidade de eventos fiscais por empresa e documento."))
            .addTagsItem(new Tag()
                .name("Logs operacionais")
                .description("Consulta de requests, erros técnicos, status HTTP e request id para suporte."))
            .addTagsItem(new Tag()
                .name("Roteamento SEFAZ")
                .description("Consulta de rota fiscal por UF da empresa emitente e identificadores externos."))
            .components(new Components()
                .addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-Key")
                    .description("Informe a chave configurada em app.security.api-key."))
                .addSecuritySchemes(JWT_SCHEME, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Cole somente o accessToken retornado por POST /api/v1/auth/token. Não inclua a palavra Bearer.")));
    }

    @Bean
    OpenApiCustomizer protectedEndpointsSecurityCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperations().forEach(operation -> {
            if (TOKEN_PATH.equals(path)) {
                operation.setSecurity(List.of());
                return;
            }
            operation.addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME).addList(JWT_SCHEME));
        }));
    }
}
