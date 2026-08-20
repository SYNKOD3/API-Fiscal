package br.com.antigravity.fiscalapi.fiscal;

import br.com.antigravity.fiscalapi.certificate.CertificateCredentialResolver;
import br.com.antigravity.fiscalapi.certificate.CertificateCredentials;
import br.com.antigravity.fiscalapi.config.AppProperties;
import br.com.antigravity.fiscalapi.company.Company;
import br.com.antigravity.fiscalapi.company.CompanyRepository;
import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.antigravity.fiscalapi.sefaz.SefazRouter;
import br.com.antigravity.fiscalapi.shared.NotFoundException;
import br.com.swconsultoria.nfe.Nfe;
import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.dom.enuns.DocumentoEnum;
import br.com.swconsultoria.nfe.exception.NfeException;
import br.com.swconsultoria.nfe.schemas.TEnviNFe;
import br.com.swconsultoria.nfe.schemas.TProtNFe;
import br.com.swconsultoria.nfe.schemas.TRetEnviNFe;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LibraryFiscalGateway implements FiscalGateway {

    private final AppProperties properties;
    private final CompanyRepository companyRepository;
    private final FiscalXmlBuilder fiscalXmlBuilder;
    private final FiscalXmlPreviewRenderer fiscalXmlPreviewRenderer;
    private final JavaNfeMapper javaNfeMapper;
    private final JavaNfeConfigurationFactory javaNfeConfigurationFactory;
    private final SefazRouter sefazRouter;
    private final CertificateCredentialResolver certificateCredentialResolver;
    private final NfceQrCodeFactory nfceQrCodeFactory;

    public LibraryFiscalGateway(AppProperties properties,
                                CompanyRepository companyRepository,
                                FiscalXmlBuilder fiscalXmlBuilder,
                                FiscalXmlPreviewRenderer fiscalXmlPreviewRenderer,
                                JavaNfeMapper javaNfeMapper,
                                JavaNfeConfigurationFactory javaNfeConfigurationFactory,
                                SefazRouter sefazRouter,
                                CertificateCredentialResolver certificateCredentialResolver,
                                NfceQrCodeFactory nfceQrCodeFactory) {
        this.properties = properties;
        this.companyRepository = companyRepository;
        this.fiscalXmlBuilder = fiscalXmlBuilder;
        this.fiscalXmlPreviewRenderer = fiscalXmlPreviewRenderer;
        this.javaNfeMapper = javaNfeMapper;
        this.javaNfeConfigurationFactory = javaNfeConfigurationFactory;
        this.sefazRouter = sefazRouter;
        this.certificateCredentialResolver = certificateCredentialResolver;
        this.nfceQrCodeFactory = nfceQrCodeFactory;
    }

    @Override
    public boolean isAvailable(FiscalSubmission submission) {
        return properties.getFiscal().getProvider() == AppProperties.Provider.LIBRARY
            && sefazRouter.isAvailable(submission.companyStateCode());
    }

    @Override
    public FiscalSubmissionResult submit(FiscalSubmission submission) {
        Company company = companyRepository.findById(submission.companyId())
            .orElseThrow(() -> new NotFoundException("Empresa emissora nao encontrada"));

        validateFiscalConfiguration(company);
        if (!isAvailable(submission)) {
            throw new FiscalGatewayException("SEFAZ da UF " + submission.companyStateCode() + " marcada como indisponivel", true);
        }

        FiscalXmlDraft draft = fiscalXmlBuilder.build(submission);
        TEnviNFe enviNFe = javaNfeMapper.toEnviNFe(draft);
        CertificateCredentials credentials = certificateCredentialResolver.resolve(company);
        try {
            ConfiguracoesNfe config = javaNfeConfigurationFactory.create(company, credentials);
            // Antes de assinar: a assinatura cobre o infNFe, e o suplemento e
            // irmao dele. Depois, o XML ja estaria fechado.
            nfceQrCodeFactory.aplicar(enviNFe, draft, config);
            TEnviNFe signedEnvelope = sign(config, enviNFe, draft);
            TRetEnviNFe response = send(config, signedEnvelope, submission.model());
            return toSubmissionResult(response);
        } finally {
            credentials.cleanup();
        }
    }

    private void validateFiscalConfiguration(Company company) {
        if (!certificateCredentialResolver.hasResolvableCertificate(company)) {
            throw new FiscalGatewayException("Certificado digital da empresa nao configurado", false);
        }
    }

    private TEnviNFe sign(ConfiguracoesNfe config, TEnviNFe enviNFe, FiscalXmlDraft draft) {
        try {
            // A validacao so entra quando ha XSD em disco. Pedi-la sem os
            // schemas nao valida nada: quebra a assinatura reclamando de
            // arquivo, e a nota nem chega a ser tentada.
            return Nfe.montaNfe(config, enviNFe, javaNfeConfigurationFactory.schemasDisponiveis());
        } catch (NfeException ex) {
            throw new FiscalGatewayException(
                "Falha ao assinar/validar XML fiscal: " + exceptionSummary(ex)
                    + " | Rascunho: " + draftSummary(draft),
                false
            );
        } catch (RuntimeException ex) {
            throw new FiscalGatewayException(
                "Falha inesperada ao assinar/validar XML fiscal: " + exceptionSummary(ex)
                    + " | Rascunho: " + draftSummary(draft),
                false
            );
        }
    }

    private TRetEnviNFe send(ConfiguracoesNfe config, TEnviNFe signedEnvelope, DocumentModel model) {
        try {
            return Nfe.enviarNfe(config, signedEnvelope, document(model));
        } catch (NfeException ex) {
            throw new FiscalGatewayException("Falha temporaria ao transmitir para SEFAZ: " + ex.getMessage(), true);
        }
    }

    private FiscalSubmissionResult toSubmissionResult(TRetEnviNFe response) {
        TProtNFe.InfProt protocol = response.getProtNFe() == null ? null : response.getProtNFe().getInfProt();
        String status = protocol == null ? response.getCStat() : protocol.getCStat();
        String reason = protocol == null ? response.getXMotivo() : protocol.getXMotivo();

        if ("100".equals(status) || "150".equals(status)) {
            return new FiscalSubmissionResult(
                protocol.getNProt(),
                protocol.getChNFe(),
                "SEFAZ autorizou documento fiscal: " + status + " - " + reason
            );
        }

        if (response.getInfRec() != null || isTemporaryStatus(status)) {
            String receipt = response.getInfRec() == null ? null : response.getInfRec().getNRec();
            throw new FiscalGatewayException(
                "SEFAZ nao autorizou imediatamente. Status: " + status
                    + " - " + reason
                    + (receipt == null ? "" : " | Recibo: " + receipt),
                true
            );
        }

        throw new FiscalGatewayException("SEFAZ rejeitou o documento. Status: " + status + " - " + reason, false);
    }

    private boolean isTemporaryStatus(String status) {
        return "103".equals(status)
            || "105".equals(status)
            || "106".equals(status)
            || "108".equals(status)
            || "109".equals(status);
    }

    private DocumentoEnum document(DocumentModel model) {
        return model == DocumentModel.NFE ? DocumentoEnum.NFE : DocumentoEnum.NFCE;
    }

    private String exceptionSummary(Throwable throwable) {
        List<String> parts = new ArrayList<>();
        Throwable current = throwable;
        while (current != null && parts.size() < 5) {
            String type = current.getClass().getSimpleName();
            String message = current.getMessage();
            parts.add(message == null || message.isBlank() ? type : type + ": " + message);
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return String.join(" <- ", parts);
    }

    private String draftSummary(FiscalXmlDraft draft) {
        return "modelo=%s, serie=%s, numero=%s, ambiente=%s, chave=%s, emitente=%s, uf=%s, cMun=%s, itens=%s, total=%s"
            .formatted(
                draft.model(),
                draft.seriesNumber(),
                draft.invoiceNumber(),
                draft.fiscalEnvironmentCode(),
                draft.accessKey(),
                draft.issuer().taxId(),
                draft.issuer().stateCode(),
                draft.issuer().cityCode(),
                draft.items().size(),
                draft.totalAmount()
            );
    }
}
