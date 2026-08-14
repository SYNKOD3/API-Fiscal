package br.com.antigravity.fiscalapi.fiscal;

import br.com.antigravity.fiscalapi.config.AppProperties;
import br.com.antigravity.fiscalapi.company.Company;
import br.com.antigravity.fiscalapi.company.CompanyRepository;
import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.antigravity.fiscalapi.shared.NotFoundException;
import br.com.swconsultoria.nfe.Nfe;
import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.dom.enuns.DocumentoEnum;
import br.com.swconsultoria.nfe.exception.NfeException;
import br.com.swconsultoria.nfe.schemas.TEnviNFe;
import br.com.swconsultoria.nfe.schemas.TProtNFe;
import br.com.swconsultoria.nfe.schemas.TRetEnviNFe;
import org.springframework.stereotype.Component;

@Component
public class LibraryFiscalGateway implements FiscalGateway {

    private final AppProperties properties;
    private final CompanyRepository companyRepository;
    private final FiscalXmlBuilder fiscalXmlBuilder;
    private final FiscalXmlPreviewRenderer fiscalXmlPreviewRenderer;
    private final JavaNfeMapper javaNfeMapper;
    private final JavaNfeConfigurationFactory javaNfeConfigurationFactory;

    public LibraryFiscalGateway(AppProperties properties,
                                CompanyRepository companyRepository,
                                FiscalXmlBuilder fiscalXmlBuilder,
                                FiscalXmlPreviewRenderer fiscalXmlPreviewRenderer,
                                JavaNfeMapper javaNfeMapper,
                                JavaNfeConfigurationFactory javaNfeConfigurationFactory) {
        this.properties = properties;
        this.companyRepository = companyRepository;
        this.fiscalXmlBuilder = fiscalXmlBuilder;
        this.fiscalXmlPreviewRenderer = fiscalXmlPreviewRenderer;
        this.javaNfeMapper = javaNfeMapper;
        this.javaNfeConfigurationFactory = javaNfeConfigurationFactory;
    }

    @Override
    public boolean isAvailable(String companyTaxId) {
        return properties.getFiscal().getProvider() == AppProperties.Provider.LIBRARY;
    }

    @Override
    public FiscalSubmissionResult submit(FiscalSubmission submission) {
        Company company = companyRepository.findById(submission.companyId())
            .orElseThrow(() -> new NotFoundException("Empresa emissora nao encontrada"));

        validateFiscalConfiguration(company);
        FiscalXmlDraft draft = fiscalXmlBuilder.build(submission);
        TEnviNFe enviNFe = javaNfeMapper.toEnviNFe(draft);
        String xmlPreview = fiscalXmlPreviewRenderer.render(draft);
        ConfiguracoesNfe config = javaNfeConfigurationFactory.create(company);

        TEnviNFe signedEnvelope = sign(config, enviNFe, xmlPreview);
        TRetEnviNFe response = send(config, signedEnvelope, submission.model());
        return toSubmissionResult(response);
    }

    private void validateFiscalConfiguration(Company company) {
        if (company.getCertificatePath() == null || company.getCertificatePath().isBlank()) {
            throw new FiscalGatewayException("Certificado digital da empresa nao configurado", false);
        }

        if (company.getCertificatePassword() == null || company.getCertificatePassword().isBlank()) {
            throw new FiscalGatewayException("Senha do certificado digital da empresa nao configurada", false);
        }
    }

    private TEnviNFe sign(ConfiguracoesNfe config, TEnviNFe enviNFe, String xmlPreview) {
        try {
            return Nfe.montaNfe(config, enviNFe, true);
        } catch (NfeException ex) {
            throw new FiscalGatewayException(
                "Falha ao assinar/validar XML fiscal: " + ex.getMessage()
                    + " | Preview: " + xmlPreview.lines().findFirst().orElse("sem preview"),
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
}
