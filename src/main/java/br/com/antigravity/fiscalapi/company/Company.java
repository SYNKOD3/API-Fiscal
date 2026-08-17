package br.com.antigravity.fiscalapi.company;

import br.com.antigravity.fiscalapi.config.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    private UUID id;

    @Column(length = 80)
    private String tenantId;

    @Column(length = 80)
    private String merchantId;

    private String callbackUrl;

    @Column(nullable = false)
    private String legalName;

    @Column(nullable = false, unique = true, length = 14)
    private String taxId;

    @Column(nullable = false)
    private String stateRegistration;

    @Column(nullable = false, length = 2)
    private String stateCode;

    private String tradeName;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String addressNumber;

    private String addressComplement;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false, length = 7)
    private String cityCode;

    @Column(nullable = false)
    private String cityName;

    @Column(nullable = false, length = 8)
    private String zipCode;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaxRegime taxRegime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FiscalEnvironment fiscalEnvironment;

    private String certificatePath;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 2048)
    private String certificatePassword;

    private String cscId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 2048)
    private String cscToken;

    @Column(nullable = false)
    private int nfeSeriesNumber;

    @Column(nullable = false)
    private long nextNfeNumber;

    @Column(nullable = false)
    private int nfceSeriesNumber;

    @Column(nullable = false)
    private long nextNfceNumber;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public static Company create(String legalName,
                                 String tenantId,
                                 String merchantId,
                                 String callbackUrl,
                                 String taxId,
                                 String stateRegistration,
                                 String stateCode,
                                 String tradeName,
                                 String street,
                                 String addressNumber,
                                 String addressComplement,
                                 String district,
                                 String cityCode,
                                 String cityName,
                                 String zipCode,
                                 String phone,
                                 TaxRegime taxRegime,
                                 FiscalEnvironment fiscalEnvironment,
                                 String certificatePath,
                                 String certificatePassword,
                                 String cscId,
                                 String cscToken,
                                 Integer nfeSeriesNumber,
                                 Long nextNfeNumber,
                                 Integer nfceSeriesNumber,
                                 Long nextNfceNumber) {
        Company company = new Company();
        company.id = UUID.randomUUID();
        company.tenantId = tenantId;
        company.merchantId = merchantId;
        company.callbackUrl = callbackUrl;
        company.legalName = legalName;
        company.taxId = taxId;
        company.stateRegistration = stateRegistration;
        company.stateCode = stateCode;
        company.tradeName = tradeName;
        company.street = street;
        company.addressNumber = addressNumber;
        company.addressComplement = addressComplement;
        company.district = district;
        company.cityCode = cityCode;
        company.cityName = cityName;
        company.zipCode = zipCode;
        company.phone = phone;
        company.taxRegime = taxRegime == null ? TaxRegime.SIMPLES_NACIONAL : taxRegime;
        company.fiscalEnvironment = fiscalEnvironment == null ? FiscalEnvironment.HOMOLOGATION : fiscalEnvironment;
        company.certificatePath = certificatePath;
        company.certificatePassword = certificatePassword;
        company.cscId = cscId;
        company.cscToken = cscToken;
        company.nfeSeriesNumber = nfeSeriesNumber == null ? 1 : nfeSeriesNumber;
        company.nextNfeNumber = nextNfeNumber == null ? 1 : nextNfeNumber;
        company.nfceSeriesNumber = nfceSeriesNumber == null ? 1 : nfceSeriesNumber;
        company.nextNfceNumber = nextNfceNumber == null ? 1 : nextNfceNumber;
        company.active = true;
        company.createdAt = OffsetDateTime.now();
        company.updatedAt = company.createdAt;
        return company;
    }

    public UUID getId() {
        return id;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public String getTaxId() {
        return taxId;
    }

    public String getStateRegistration() {
        return stateRegistration;
    }

    public String getStateCode() {
        return stateCode;
    }

    public String getTradeName() {
        return tradeName;
    }

    public String getStreet() {
        return street;
    }

    public String getAddressNumber() {
        return addressNumber;
    }

    public String getAddressComplement() {
        return addressComplement;
    }

    public String getDistrict() {
        return district;
    }

    public String getCityCode() {
        return cityCode;
    }

    public String getCityName() {
        return cityName;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getPhone() {
        return phone;
    }

    public TaxRegime getTaxRegime() {
        return taxRegime;
    }

    public FiscalEnvironment getFiscalEnvironment() {
        return fiscalEnvironment;
    }

    public String getCertificatePath() {
        return certificatePath;
    }

    public String getCertificatePassword() {
        return certificatePassword;
    }

    public String getCscId() {
        return cscId;
    }

    public String getCscToken() {
        return cscToken;
    }

    public int getNfeSeriesNumber() {
        return nfeSeriesNumber;
    }

    public long getNextNfeNumber() {
        return nextNfeNumber;
    }

    public int getNfceSeriesNumber() {
        return nfceSeriesNumber;
    }

    public long getNextNfceNumber() {
        return nextNfceNumber;
    }

    public FiscalNumber allocateNfeNumber() {
        FiscalNumber fiscalNumber = new FiscalNumber(nfeSeriesNumber, nextNfeNumber);
        nextNfeNumber += 1;
        updatedAt = OffsetDateTime.now();
        return fiscalNumber;
    }

    public FiscalNumber allocateNfceNumber() {
        FiscalNumber fiscalNumber = new FiscalNumber(nfceSeriesNumber, nextNfceNumber);
        nextNfceNumber += 1;
        updatedAt = OffsetDateTime.now();
        return fiscalNumber;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
