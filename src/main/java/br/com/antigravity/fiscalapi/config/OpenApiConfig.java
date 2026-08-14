package br.com.antigravity.fiscalapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String API_KEY_SCHEME = "Chave da API";

    @Bean
    OpenAPI fiscalOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("API Fiscal")
                .version("0.0.1")
                .description("""
                    API para emissao de NF-e e NFC-e em ambiente multiempresa, com contingencia local,
                    armazenamento de XML/comprovante e reprocessamento automatico quando a SEFAZ voltar.
                    """)
                .contact(new Contact().name("Antigravity")))
            .addTagsItem(new Tag()
                .name("Empresas")
                .description("Cadastro e consulta de empresas emissoras."))
            .addTagsItem(new Tag()
                .name("Documentos fiscais")
                .description("Emissao, consulta, retry, XML e comprovante de NF-e/NFC-e."))
            .addTagsItem(new Tag()
                .name("Auditoria fiscal")
                .description("Rastreabilidade de eventos fiscais por empresa e documento."))
            .components(new Components()
                .addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-Key")
                    .description("Informe a chave configurada em app.security.api-key.")))
            .addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME));
    }
}
