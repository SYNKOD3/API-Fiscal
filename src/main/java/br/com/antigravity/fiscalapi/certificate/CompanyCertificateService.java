package br.com.antigravity.fiscalapi.certificate;

import br.com.antigravity.fiscalapi.audit.FiscalAuditService;
import br.com.antigravity.fiscalapi.company.Company;
import br.com.antigravity.fiscalapi.company.CompanyRepository;
import br.com.antigravity.fiscalapi.config.AppProperties;
import br.com.antigravity.fiscalapi.security.JwtSecurityContext;
import br.com.antigravity.fiscalapi.shared.BadRequestException;
import br.com.antigravity.fiscalapi.shared.ConflictException;
import br.com.antigravity.fiscalapi.shared.NotFoundException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CompanyCertificateService {

    private static final long DEFAULT_MAX_SIZE_BYTES = 5 * 1024 * 1024;

    private final CompanyRepository companyRepository;
    private final CompanyCertificateRepository certificateRepository;
    private final CertificateMetadataReader metadataReader;
    private final FiscalAuditService auditService;
    private final AppProperties properties;
    private final JwtSecurityContext jwtSecurityContext;

    public CompanyCertificateService(CompanyRepository companyRepository,
                                     CompanyCertificateRepository certificateRepository,
                                     CertificateMetadataReader metadataReader,
                                     FiscalAuditService auditService,
                                     AppProperties properties,
                                     JwtSecurityContext jwtSecurityContext) {
        this.companyRepository = companyRepository;
        this.certificateRepository = certificateRepository;
        this.metadataReader = metadataReader;
        this.auditService = auditService;
        this.properties = properties;
        this.jwtSecurityContext = jwtSecurityContext;
    }

    @Transactional
    public CompanyCertificateResponse upload(UUID companyId, MultipartFile file, String password) {
        Company company = companyRepository.findByIdForUpdate(companyId)
            .orElseThrow(() -> new NotFoundException("Empresa nao encontrada"));
        jwtSecurityContext.requireCompanyAccess(company);

        validateInput(file, password);
        byte[] certificateBytes = readBytes(file);
        CertificateMetadata metadata = metadataReader.read(certificateBytes, password);
        validateMetadata(company, metadata);

        UUID certificateId = UUID.randomUUID();
        String extension = extension(file.getOriginalFilename());
        Path storagePath = writeCertificate(companyId, certificateId, extension, certificateBytes);

        certificateRepository.findFirstByCompany_IdAndStatusOrderByActivatedAtDesc(companyId, CertificateStatus.ACTIVE)
            .ifPresent(CompanyCertificate::replace);

        CompanyCertificate certificate = CompanyCertificate.active(
            company,
            storagePath.toString(),
            cleanOriginalFileName(file.getOriginalFilename()),
            password,
            metadata
        );
        CompanyCertificate saved = certificateRepository.save(certificate);
        auditService.record(
            company.getId(),
            null,
            "CERTIFICATE_UPLOADED",
            "Certificado A1 cadastrado e ativado para empresa emissora",
            saved.getId().toString()
        );
        return CompanyCertificateResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CompanyCertificateResponse> list(UUID companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new NotFoundException("Empresa nao encontrada"));
        jwtSecurityContext.requireCompanyAccess(company);
        return certificateRepository.findByCompany_IdOrderByCreatedAtDesc(companyId).stream()
            .map(CompanyCertificateResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public CertificateCredentials resolveForEmission(Company company) {
        return certificateRepository.findFirstByCompany_IdAndStatusOrderByActivatedAtDesc(company.getId(), CertificateStatus.ACTIVE)
            .map(certificate -> new CertificateCredentials(
                certificate.getStoragePath(),
                certificate.getCertificatePassword(),
                true
            ))
            .orElseGet(() -> legacyCredentials(company));
    }

    @Transactional(readOnly = true)
    public boolean hasCertificateForEmission(Company company) {
        return certificateRepository.existsByCompany_IdAndStatus(company.getId(), CertificateStatus.ACTIVE)
            || hasText(company.getCertificatePath()) && hasText(company.getCertificatePassword());
    }

    private CertificateCredentials legacyCredentials(Company company) {
        if (hasText(company.getCertificatePath()) && hasText(company.getCertificatePassword())) {
            return new CertificateCredentials(company.getCertificatePath(), company.getCertificatePassword(), false);
        }
        throw new BadRequestException("Certificado digital da empresa nao configurado");
    }

    private void validateInput(MultipartFile file, String password) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Envie um arquivo de certificado A1 .pfx ou .p12");
        }
        if (!hasText(password)) {
            throw new BadRequestException("Informe a senha do certificado A1");
        }
        if (file.getSize() > maxSizeBytes()) {
            throw new BadRequestException("Certificado excede o tamanho maximo permitido");
        }
        String extension = extension(file.getOriginalFilename());
        if (!".pfx".equals(extension) && !".p12".equals(extension)) {
            throw new BadRequestException("Apenas certificados A1 .pfx ou .p12 sao aceitos");
        }
    }

    private void validateMetadata(Company company, CertificateMetadata metadata) {
        if (metadata.validUntil() != null && metadata.validUntil().isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("Certificado A1 vencido");
        }
        if (hasText(metadata.taxId()) && !company.getTaxId().equals(metadata.taxId())) {
            throw new ConflictException("CNPJ do certificado nao confere com o CNPJ da empresa emissora");
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Falha ao ler arquivo do certificado");
        }
    }

    private Path writeCertificate(UUID companyId, UUID certificateId, String extension, byte[] certificateBytes) {
        try {
            Path root = Path.of(properties.getCertificates().getStoragePath()).toAbsolutePath().normalize();
            Path companyDir = root.resolve(companyId.toString()).normalize();
            if (!companyDir.startsWith(root)) {
                throw new BadRequestException("Caminho de storage de certificado invalido");
            }
            Files.createDirectories(companyDir);
            Path destination = companyDir.resolve(certificateId + extension).normalize();
            Path temp = companyDir.resolve(certificateId + extension + ".tmp").normalize();
            Files.write(temp, certificateBytes);
            moveToFinalPath(temp, destination);
            return destination;
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao salvar certificado A1 em storage privado", ex);
        }
    }

    private void moveToFinalPath(Path temp, Path destination) throws IOException {
        try {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String extension(String fileName) {
        String safeName = cleanOriginalFileName(fileName).toLowerCase(Locale.ROOT);
        int dotIndex = safeName.lastIndexOf('.');
        return dotIndex < 0 ? "" : safeName.substring(dotIndex);
    }

    private String cleanOriginalFileName(String fileName) {
        if (!hasText(fileName)) {
            return "certificado.pfx";
        }
        return Path.of(fileName).getFileName().toString();
    }

    private long maxSizeBytes() {
        long configured = properties.getCertificates().getMaxSizeBytes();
        return configured <= 0 ? DEFAULT_MAX_SIZE_BYTES : configured;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
