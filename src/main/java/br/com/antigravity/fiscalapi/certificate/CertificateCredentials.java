package br.com.antigravity.fiscalapi.certificate;

public record CertificateCredentials(
    String storagePath,
    String password,
    boolean managedCertificate
) {
}
