package br.com.antigravity.fiscalapi.certificate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record CertificateCredentials(
    String storagePath,
    String password,
    boolean managedCertificate,
    boolean temporaryFile
) {
    public static CertificateCredentials stored(String storagePath, String password, boolean managedCertificate) {
        return new CertificateCredentials(storagePath, password, managedCertificate, false);
    }

    public static CertificateCredentials temporary(Path storagePath, String password) {
        return new CertificateCredentials(storagePath.toString(), password, false, true);
    }

    public void cleanup() {
        if (!temporaryFile || storagePath == null || storagePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(storagePath));
        } catch (IOException ignored) {
            // Best effort: arquivo temporario nao deve interromper o fluxo fiscal.
        }
    }
}
