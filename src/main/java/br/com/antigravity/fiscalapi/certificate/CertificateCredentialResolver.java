package br.com.antigravity.fiscalapi.certificate;

import br.com.antigravity.fiscalapi.company.Company;
import br.com.antigravity.fiscalapi.config.AppProperties;
import br.com.antigravity.fiscalapi.fiscal.FiscalGatewayException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class CertificateCredentialResolver {

    private final CompanyCertificateService companyCertificateService;
    private final RemoteCertificateClient remoteCertificateClient;
    private final CertificateMetadataReader metadataReader;
    private final AppProperties properties;

    public CertificateCredentialResolver(CompanyCertificateService companyCertificateService,
                                         RemoteCertificateClient remoteCertificateClient,
                                         CertificateMetadataReader metadataReader,
                                         AppProperties properties) {
        this.companyCertificateService = companyCertificateService;
        this.remoteCertificateClient = remoteCertificateClient;
        this.metadataReader = metadataReader;
        this.properties = properties;
    }

    public CertificateCredentials resolve(Company company) {
        if (companyCertificateService.hasCertificateForEmission(company)) {
            return companyCertificateService.resolveForEmission(company);
        }
        return remote(company);
    }

    public boolean hasResolvableCertificate(Company company) {
        return companyCertificateService.hasCertificateForEmission(company)
            || company.getCallbackUrl() != null && !company.getCallbackUrl().isBlank();
    }

    private CertificateCredentials remote(Company company) {
        RemoteCertificateMaterial material = remoteCertificateClient.fetch(company);
        if (!hasText(material.certificateBase64()) || !hasText(material.password())) {
            throw new FiscalGatewayException("Callback de certificado retornou dados incompletos", false);
        }

        byte[] bytes = decode(material.certificateBase64());
        if (bytes.length > maxSizeBytes()) {
            throw new FiscalGatewayException("Certificado remoto excede o tamanho maximo permitido", false);
        }

        validateMetadata(company, bytes, material.password());
        Path temp = writeTemporaryCertificate(material.originalFileName(), bytes);
        return CertificateCredentials.temporary(temp, material.password());
    }

    private byte[] decode(String base64) {
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException ex) {
            throw new FiscalGatewayException("Certificado remoto nao esta em Base64 valido", false);
        }
    }

    private void validateMetadata(Company company, byte[] bytes, String password) {
        try {
            CertificateMetadata metadata = metadataReader.read(bytes, password);
            if (metadata.validUntil() != null && metadata.validUntil().isBefore(OffsetDateTime.now())) {
                throw new FiscalGatewayException("Certificado A1 remoto vencido", false);
            }
            if (hasText(metadata.taxId()) && !company.getTaxId().equals(metadata.taxId())) {
                throw new FiscalGatewayException("CNPJ do certificado remoto nao confere com o CNPJ da empresa emissora", false);
            }
        } catch (RuntimeException ex) {
            if (ex instanceof FiscalGatewayException gatewayException) {
                throw gatewayException;
            }
            throw new FiscalGatewayException("Falha ao validar certificado remoto: " + ex.getMessage(), false);
        }
    }

    private Path writeTemporaryCertificate(String originalFileName, byte[] bytes) {
        try {
            String suffix = suffix(originalFileName);
            Path temp = Files.createTempFile("fiscal-api-certificate-", suffix);
            Files.write(temp, bytes);
            return temp;
        } catch (IOException ex) {
            throw new FiscalGatewayException("Falha ao preparar certificado temporario", true);
        }
    }

    private String suffix(String originalFileName) {
        if (originalFileName != null && originalFileName.toLowerCase().endsWith(".p12")) {
            return ".p12";
        }
        return ".pfx";
    }

    private long maxSizeBytes() {
        long configured = properties.getCertificates().getMaxSizeBytes();
        return configured <= 0 ? 5 * 1024 * 1024 : configured;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
