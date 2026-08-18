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

    private static final String BASIC_SCHEME = "Login e senha";
    private static final String API_KEY_SCHEME = "Chave da API";
    private static final String JWT_SCHEME = "JWT Integrador";
    private static final String TOKEN_PATH = "/api/v1/auth/token";

    @Bean
    OpenAPI fiscalOpenApi(AppProperties properties) {
        return new OpenAPI()
            .info(new Info()
                .title("API Fiscal")
                .version("0.0.1")
                .description("""
                    API para emissão de NF-e e NFC-e em ambiente multiempresa, com contingência local,
                    armazenamento de XML/comprovante e reprocessamento automático quando a SEFAZ voltar.

                    ## Como testar pelo Swagger

                    1. Clique no botão `Authorize` no topo do Swagger.
                    2. Em `Login e senha`, informe usuário e senha da integração.
                    3. Execute os endpoints protegidos normalmente pelo próprio Swagger.

                    Em ambiente local, os valores padrão são `dev-client` e `dev-password-change-me`.
                    `X-API-Key` e JWT continuam disponíveis para ativação futura por variável de ambiente,
                    mas ficam desobrigados enquanto `API_KEY_AUTH_ENABLED=false` e `JWT_AUTH_ENABLED=false`.
                    """)
                .contact(new Contact().name("Antigravity")))
            .addTagsItem(new Tag()
                .name("Autenticação")
                .description("Geração opcional de token Bearer para uso futuro, quando JWT estiver ativado."))
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
            .components(securityComponents(properties));
    }

    private Components securityComponents(AppProperties properties) {
        Components components = new Components()
            .addSecuritySchemes(BASIC_SCHEME, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("basic")
                .description("Informe o usuário e a senha da integração. Este é o acesso principal da API."));

        if (properties.getSecurity().isApiKeyEnabled()) {
            components.addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-Key")
                .description("Informe a chave configurada em APP_API_KEY."));
        }

        if (properties.getSecurity().getJwt().isEnabled()) {
            components.addSecuritySchemes(JWT_SCHEME, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Cole somente o accessToken, sem Bearer."));
        }

        return components;
    }

    @Bean
    OpenApiCustomizer protectedEndpointsSecurityCustomizer(AppProperties properties) {
        return openApi -> openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperations().forEach(operation -> {
            if (TOKEN_PATH.equals(path)) {
                operation.setSecurity(List.of());
                return;
            }
            SecurityRequirement basicRequirement = new SecurityRequirement().addList(BASIC_SCHEME);
            if (properties.getSecurity().isApiKeyEnabled()) {
                basicRequirement.addList(API_KEY_SCHEME);
            }
            operation.addSecurityItem(basicRequirement);

            if (properties.getSecurity().getJwt().isEnabled()) {
                SecurityRequirement jwtRequirement = new SecurityRequirement().addList(JWT_SCHEME);
                if (properties.getSecurity().isApiKeyEnabled()) {
                    jwtRequirement.addList(API_KEY_SCHEME);
                }
                operation.addSecurityItem(jwtRequirement);
            }
        }));
    }
}
