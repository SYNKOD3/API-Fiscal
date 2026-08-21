package br.com.antigravity.fiscalapi.fiscal;

import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.exception.NfeException;
import br.com.swconsultoria.nfe.schemas.TEnviNFe;
import br.com.swconsultoria.nfe.schemas.TNFe;
import br.com.swconsultoria.nfe.util.NFCeUtil;
import br.com.swconsultoria.nfe.util.WebServiceUtil;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Informacoes suplementares da NFC-e: o QR Code e a URL de consulta.
 *
 * O grupo e opcional no XSD e obrigatorio para NFC-e por regra de negocio —
 * sem ele a nota e recusada, e mesmo que passasse, o consumidor ficaria sem o
 * que conferir. Nao vale para NF-e (modelo 55), que nao tem QR Code.
 *
 * O conteudo segue a NT 2025-001, que substituiu o padrao antigo: o QR Code
 * "online" e apenas <url>?p=<chave>|3|<ambiente>, sem CSC e sem hash. O CSC
 * continua no cadastro porque a contingencia offline e outra historia — la o
 * codigo e assinado com o certificado —, mas esse caminho ainda nao existe
 * aqui e nao adianta fingir que existe.
 *
 * As URLs saem do proprio catalogo da java-nfe, que as mantem por UF e por
 * ambiente. Copiar essa tabela para ca seria assumir a manutencao de um dado
 * que muda por decisao de vinte e sete secretarias.
 */
@Component
public class NfceQrCodeFactory {

    /// Devolve o conteudo do QR Code aplicado, ou vazio quando nao ha — NF-e
    /// nao tem. Quem chama precisa dele para guardar: o cupom e impresso do
    /// outro lado, por quem nao consegue recalcular este codigo.
    public Optional<String> aplicar(TEnviNFe enviNFe, FiscalXmlDraft draft, ConfiguracoesNfe config) {
        if (draft.model() != DocumentModel.NFCE) {
            return Optional.empty();
        }

        String urlQrCode = url(config, draft, "URL-QRCode");
        String urlConsulta = url(config, draft, "URL-ConsultaNFCe");

        TNFe.InfNFeSupl suplemento = new TNFe.InfNFeSupl();
        suplemento.setQrCode(NFCeUtil.getCodeQRCodeV3(
            draft.accessKey(),
            draft.fiscalEnvironmentCode(),
            urlQrCode
        ));
        suplemento.setUrlChave(urlConsulta);
        enviNFe.getNFe().get(0).setInfNFeSupl(suplemento);
        return Optional.of(suplemento.getQrCode());
    }

    /**
     * A secao do catalogo e NFCe_<UF>_<P|H> — o ambiente entra na chave porque
     * homologacao e producao tem enderecos diferentes, e apontar o QR Code de
     * teste para producao daria um codigo que abre e mostra "nota nao
     * encontrada" para o cliente.
     */
    private String url(ConfiguracoesNfe config, FiscalXmlDraft draft, String chave) {
        String secao = "NFCe_%s_%s".formatted(
            draft.issuer().stateCode(),
            "1".equals(draft.fiscalEnvironmentCode()) ? "P" : "H"
        );
        try {
            return WebServiceUtil.getCustomUrl(config, secao, chave);
        } catch (NfeException ex) {
            throw new FiscalGatewayException(
                "Endereco de %s nao encontrado para %s: %s".formatted(chave, secao, ex.getMessage()),
                false
            );
        }
    }
}
