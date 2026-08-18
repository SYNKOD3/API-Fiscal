package br.com.antigravity.fiscalapi.config;

import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Security security = new Security();
    private final Fiscal fiscal = new Fiscal();
    private final Retry retry = new Retry();
    private final DevConsole devConsole = new DevConsole();
    private final OpenApi openApi = new OpenApi();
    private final Certificates certificates = new Certificates();

    public Security getSecurity() {
        return security;
    }

    public Fiscal getFiscal() {
        return fiscal;
    }

    public Retry getRetry() {
        return retry;
    }

    public DevConsole getDevConsole() {
        return devConsole;
    }

    public OpenApi getOpenApi() {
        return openApi;
    }

    public Certificates getCertificates() {
        return certificates;
    }

    public static class Security {
        private boolean basicAuthEnabled = true;
        private boolean apiKeyEnabled = false;
        private String apiKey = "change-me";
        private final Jwt jwt = new Jwt();
        private final IntegrationClient integrationClient = new IntegrationClient();

        public boolean isBasicAuthEnabled() {
            return basicAuthEnabled;
        }

        public void setBasicAuthEnabled(boolean basicAuthEnabled) {
            this.basicAuthEnabled = basicAuthEnabled;
        }

        public boolean isApiKeyEnabled() {
            return apiKeyEnabled;
        }

        public void setApiKeyEnabled(boolean apiKeyEnabled) {
            this.apiKeyEnabled = apiKeyEnabled;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public Jwt getJwt() {
            return jwt;
        }

        public IntegrationClient getIntegrationClient() {
            return integrationClient;
        }
    }

    public static class Jwt {
        private boolean enabled = false;
        private String secret = "dev-only-jwt-secret-change-before-production";
        private String issuer = "fiscal-platform";
        private String audience = "fiscal-api";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }
    }

    public static class IntegrationClient {
        private String username = "dev-client";
        private String password = "dev-password-change-me";
        private Set<String> defaultScopes = new HashSet<>(Set.of("fiscal:admin"));
        private int tokenTtlMinutes = 60;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Set<String> getDefaultScopes() {
            return defaultScopes;
        }

        public void setDefaultScopes(Set<String> defaultScopes) {
            this.defaultScopes = defaultScopes == null ? new HashSet<>() : defaultScopes;
        }

        public int getTokenTtlMinutes() {
            return tokenTtlMinutes;
        }

        public void setTokenTtlMinutes(int tokenTtlMinutes) {
            this.tokenTtlMinutes = tokenTtlMinutes;
        }
    }

    public static class Fiscal {
        private Provider provider = Provider.STUB;
        private boolean stubOnline = true;
        private Set<String> unavailableStates = new HashSet<>();

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        public boolean isStubOnline() {
            return stubOnline;
        }

        public void setStubOnline(boolean stubOnline) {
            this.stubOnline = stubOnline;
        }

        public Set<String> getUnavailableStates() {
            return unavailableStates;
        }

        public void setUnavailableStates(Set<String> unavailableStates) {
            this.unavailableStates = unavailableStates == null ? new HashSet<>() : unavailableStates;
        }
    }

    public static class Retry {
        private long fixedDelayMs = 30000;
        private int batchSize = 50;

        public long getFixedDelayMs() {
            return fixedDelayMs;
        }

        public void setFixedDelayMs(long fixedDelayMs) {
            this.fixedDelayMs = fixedDelayMs;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }

    public static class DevConsole {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class OpenApi {
        private boolean publicAccess = true;

        public boolean isPublicAccess() {
            return publicAccess;
        }

        public void setPublicAccess(boolean publicAccess) {
            this.publicAccess = publicAccess;
        }
    }

    public static class Certificates {
        private String storagePath = "certificates";
        private long maxSizeBytes = 5242880;
        private String remoteAuthToken = "";

        public String getStoragePath() {
            return storagePath;
        }

        public void setStoragePath(String storagePath) {
            this.storagePath = storagePath;
        }

        public long getMaxSizeBytes() {
            return maxSizeBytes;
        }

        public void setMaxSizeBytes(long maxSizeBytes) {
            this.maxSizeBytes = maxSizeBytes;
        }

        public String getRemoteAuthToken() {
            return remoteAuthToken;
        }

        public void setRemoteAuthToken(String remoteAuthToken) {
            this.remoteAuthToken = remoteAuthToken;
        }
    }

    public enum Provider {
        STUB,
        LIBRARY
    }
}
