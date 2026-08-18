package br.com.antigravity.fiscalapi.certificate;

import br.com.antigravity.fiscalapi.company.Company;
import br.com.antigravity.fiscalapi.config.AppProperties;
import br.com.antigravity.fiscalapi.fiscal.FiscalGatewayException;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RemoteCertificateClient {

    private static final String TOKEN_HEADER = "X-Fiscal-Certificate-Token";

    private final AppProperties properties;
    private final RestClient restClient;

    public RemoteCertificateClient(AppProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    public RemoteCertificateMaterial fetch(Company company) {
        if (company.getCallbackUrl() == null || company.getCallbackUrl().isBlank()) {
            throw new FiscalGatewayException("Certificado digital da empresa nao configurado", false);
        }
        String token = properties.getCertificates().getRemoteAuthToken();
        if (token == null || token.isBlank()) {
            throw new FiscalGatewayException("CERTIFICATE_REMOTE_AUTH_TOKEN nao configurado para buscar certificado remoto", false);
        }

        try {
            RemoteCertificateEnvelope envelope = restClient.post()
                .uri(URI.create(company.getCallbackUrl()))
                .contentType(MediaType.APPLICATION_JSON)
                .header(TOKEN_HEADER, token)
                .body(new RemoteCertificateRequest(
                    company.getTenantId(),
                    company.getMerchantId(),
                    company.getTaxId()
                ))
                .retrieve()
                .body(RemoteCertificateEnvelope.class);

            if (envelope == null || envelope.data() == null) {
                throw new FiscalGatewayException("Callback de certificado retornou resposta vazia", true);
            }
            return envelope.data();
        } catch (RestClientException | IllegalArgumentException ex) {
            throw new FiscalGatewayException("Falha ao buscar certificado remoto: " + ex.getMessage(), true);
        }
    }

    private record RemoteCertificateRequest(String tenantId, String merchantId, String taxId) {
    }

    private record RemoteCertificateEnvelope(RemoteCertificateMaterial data) {
    }
}
