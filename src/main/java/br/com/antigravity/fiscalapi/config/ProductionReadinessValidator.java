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
    private static final String DEV_JWT_SECRET = "change-me";

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

        requireStrongApiKey();
        requireJwt();
        requireSecretsKey();
        requireLibraryProvider();
        requirePrivateDevTools();
        requirePostgresDatasource();
        requireCertificateStorage();
    }

    private boolean isProductionProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    private void requireStrongApiKey() {
        String apiKey = properties.getSecurity().getApiKey();
        if (apiKey == null || apiKey.isBlank() || DEV_API_KEY.equals(apiKey) || apiKey.length() < 24) {
            throw new IllegalStateException("Producao exige APP_API_KEY forte com pelo menos 24 caracteres.");
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

    private void requireJwt() {
        AppProperties.Jwt jwt = properties.getSecurity().getJwt();
        if (!jwt.isEnabled()) {
            throw new IllegalStateException("Producao exige JWT_AUTH_ENABLED=true.");
        }
        String secret = jwt.getSecret();
        if (secret == null || secret.isBlank() || DEV_JWT_SECRET.equals(secret) || secret.length() < 32) {
            throw new IllegalStateException("Producao exige JWT_SECRET forte com pelo menos 32 caracteres.");
        }
        if (jwt.getIssuer() == null || jwt.getIssuer().isBlank()) {
            throw new IllegalStateException("Producao exige JWT_ISSUER configurado.");
        }
        if (jwt.getAudience() == null || jwt.getAudience().isBlank()) {
            throw new IllegalStateException("Producao exige JWT_AUDIENCE configurado.");
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
