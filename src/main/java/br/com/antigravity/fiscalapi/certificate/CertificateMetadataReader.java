package br.com.antigravity.fiscalapi.certificate;

import br.com.antigravity.fiscalapi.shared.BadRequestException;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class CertificateMetadataReader {

    private static final Pattern TAX_ID_PATTERN = Pattern.compile("(?<!\\d)(\\d{14})(?!\\d)");

    public CertificateMetadata read(byte[] certificateBytes, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(certificateBytes), password.toCharArray());
            X509Certificate certificate = firstCertificate(keyStore);
            String subjectDn = certificate.getSubjectX500Principal().getName();
            return new CertificateMetadata(
                extractTaxId(subjectDn),
                certificate.getSerialNumber().toString(16).toUpperCase(),
                subjectDn,
                certificate.getNotBefore().toInstant().atOffset(ZoneOffset.UTC),
                certificate.getNotAfter().toInstant().atOffset(ZoneOffset.UTC)
            );
        } catch (Exception ex) {
            throw new BadRequestException("Certificado A1 invalido ou senha incorreta");
        }
    }

    private X509Certificate firstCertificate(KeyStore keyStore) throws KeyStoreException {
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.getCertificate(alias) instanceof X509Certificate certificate) {
                return certificate;
            }
        }
        throw new BadRequestException("Certificado A1 sem certificado X509 valido");
    }

    private String extractTaxId(String subjectDn) {
        Matcher matcher = TAX_ID_PATTERN.matcher(subjectDn);
        return matcher.find() ? matcher.group(1) : null;
    }
}
