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

    public static class Security {
        private String apiKey = "change-me";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
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

    public enum Provider {
        STUB,
        LIBRARY
    }
}
