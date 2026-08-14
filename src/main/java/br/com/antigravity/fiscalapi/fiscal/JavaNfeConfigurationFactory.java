package br.com.antigravity.fiscalapi.fiscal;

import br.com.antigravity.fiscalapi.company.Company;
import br.com.antigravity.fiscalapi.company.FiscalEnvironment;
import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.certificado.exception.CertificadoException;
import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.dom.enuns.AmbienteEnum;
import br.com.swconsultoria.nfe.dom.enuns.EstadosEnum;
import java.io.FileNotFoundException;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class JavaNfeConfigurationFactory {

    private static final ZoneId BRAZIL_FISCAL_ZONE = ZoneId.of("America/Sao_Paulo");

    public ConfiguracoesNfe create(Company company) {
        try {
            Certificado certificate = CertificadoService.certificadoPfx(
                company.getCertificatePath(),
                company.getCertificatePassword()
            );

            return ConfiguracoesNfe.criarConfiguracoes(
                state(company.getStateCode()),
                environment(company.getFiscalEnvironment()),
                certificate,
                company.getCscToken(),
                BRAZIL_FISCAL_ZONE
            );
        } catch (FileNotFoundException ex) {
            throw new FiscalGatewayException("Arquivo do certificado digital nao encontrado", false);
        } catch (CertificadoException ex) {
            throw new FiscalGatewayException("Falha ao carregar certificado digital: " + ex.getMessage(), false);
        }
    }

    private EstadosEnum state(String stateCode) {
        try {
            return EstadosEnum.valueOf(stateCode);
        } catch (IllegalArgumentException ex) {
            throw new FiscalGatewayException("UF fiscal invalida para Java-NFe: " + stateCode, false);
        }
    }

    private AmbienteEnum environment(FiscalEnvironment fiscalEnvironment) {
        return fiscalEnvironment == FiscalEnvironment.PRODUCTION
            ? AmbienteEnum.PRODUCAO
            : AmbienteEnum.HOMOLOGACAO;
    }
}
