package br.com.antigravity.fiscalapi.certificate;

public record RemoteCertificateMaterial(
    String certificateBase64,
    String password,
    String originalFileName,
    String taxId
) {
}
