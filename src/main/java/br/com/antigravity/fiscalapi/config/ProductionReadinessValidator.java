package br.com.antigravity.fiscalapi.config;

import java.util.Arrays;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionReadinessValidator implements ApplicationRunner {

    private static final String DEV_API_KEY = "change-me";
    private static final String DEV_SECRETS_KEY = "dev-insecure-change-me";
    private static final String DEV_JWT_SECRET = "dev-only-jwt-secret-change-before-production";
    private static final String DEV_AUTH_USERNAME = "dev-client";
    private static final String DEV_AUTH_PASSWORD = "dev-password-change-me";

    private final AppProperties properties;
    private final Environment environment;

    public ProductionReadinessValidator(AppProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProductionProfile()) {
            return;
        }

        requireAtLeastOneAuthenticationMode();
        requireIntegrationClientCredentials();
        requireOptionalApiKey();
        requireOptionalJwt();
        requireSecretsKey();
        requireLibraryProvider();
        requirePrivateDevTools();
        requirePostgresDatasource();
        requireCertificateStorage();
    }

    private boolean isProductionProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    private void requireOptionalApiKey() {
        if (!properties.getSecurity().isApiKeyEnabled()) {
            return;
        }
        String apiKey = properties.getSecurity().getApiKey();
        if (apiKey == null || apiKey.isBlank() || DEV_API_KEY.equals(apiKey) || apiKey.length() < 24) {
            throw new IllegalStateException("API_KEY_AUTH_ENABLED=true exige APP_API_KEY forte com pelo menos 24 caracteres.");
        }
    }

    private void requireSecretsKey() {
        String secret = System.getenv().getOrDefault(
            "APP_SECRETS_KEY",
            System.getProperty("app.secrets.key", "")
        );
        if (secret.isBlank() || DEV_SECRETS_KEY.equals(secret) || secret.length() < 32) {
            throw new IllegalStateException("Producao exige APP_SECRETS_KEY forte, fixa e com pelo menos 32 caracteres.");
        }
    }

    private void requireOptionalJwt() {
        AppProperties.Jwt jwt = properties.getSecurity().getJwt();
        if (!jwt.isEnabled()) {
            return;
        }
        String secret = jwt.getSecret();
        if (secret == null || secret.isBlank() || DEV_JWT_SECRET.equals(secret) || secret.length() < 32) {
            throw new IllegalStateException("JWT_AUTH_ENABLED=true exige JWT_SECRET forte com pelo menos 32 caracteres.");
        }
        if (jwt.getIssuer() == null || jwt.getIssuer().isBlank()) {
            throw new IllegalStateException("JWT_AUTH_ENABLED=true exige JWT_ISSUER configurado.");
        }
        if (jwt.getAudience() == null || jwt.getAudience().isBlank()) {
            throw new IllegalStateException("JWT_AUTH_ENABLED=true exige JWT_AUDIENCE configurado.");
        }
    }

    private void requireAtLeastOneAuthenticationMode() {
        if (!properties.getSecurity().isBasicAuthEnabled()
            && !properties.getSecurity().isApiKeyEnabled()
            && !properties.getSecurity().getJwt().isEnabled()) {
            throw new IllegalStateException("Producao exige BASIC_AUTH_ENABLED, API_KEY_AUTH_ENABLED ou JWT_AUTH_ENABLED ativo.");
        }
    }

    private void requireIntegrationClientCredentials() {
        if (!properties.getSecurity().isBasicAuthEnabled()
            && !properties.getSecurity().getJwt().isEnabled()) {
            return;
        }
        AppProperties.IntegrationClient integrationClient = properties.getSecurity().getIntegrationClient();
        String username = integrationClient.getUsername();
        String password = integrationClient.getPassword();
        if (username == null || username.isBlank() || DEV_AUTH_USERNAME.equals(username)) {
            throw new IllegalStateException("Produção exige AUTH_USERNAME configurado.");
        }
        if (password == null || password.isBlank() || DEV_AUTH_PASSWORD.equals(password) || password.length() < 24) {
            throw new IllegalStateException("Produção exige AUTH_PASSWORD forte com pelo menos 24 caracteres.");
        }
        if (properties.getSecurity().getJwt().isEnabled() && integrationClient.getDefaultScopes().isEmpty()) {
            throw new IllegalStateException("JWT_AUTH_ENABLED=true exige AUTH_DEFAULT_SCOPES configurado.");
        }
    }

    private void requireLibraryProvider() {
        if (properties.getFiscal().getProvider() != AppProperties.Provider.LIBRARY) {
            throw new IllegalStateException("Producao exige app.fiscal.provider=LIBRARY.");
        }
    }

    private void requirePrivateDevTools() {
        if (properties.getDevConsole().isEnabled()) {
            throw new IllegalStateException("Producao exige app.dev-console.enabled=false.");
        }
        if (properties.getOpenApi().isPublicAccess()) {
            throw new IllegalStateException("Producao exige app.open-api.public-access=false.");
        }
    }

    private void requirePostgresDatasource() {
        String datasourceUrl = environment.getProperty("spring.datasource.url", "");
        if (!datasourceUrl.startsWith("jdbc:postgresql:")) {
            throw new IllegalStateException("Producao exige spring.datasource.url PostgreSQL.");
        }
    }

    private void requireCertificateStorage() {
        String storagePath = properties.getCertificates().getStoragePath();
        if (storagePath == null || storagePath.isBlank() || "certificates".equals(storagePath)) {
            throw new IllegalStateException("Producao exige CERTIFICATE_STORAGE_PATH privado e persistente.");
        }
    }
}
