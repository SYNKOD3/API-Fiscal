package br.com.antigravity.fiscalapi.fiscal;

import br.com.antigravity.fiscalapi.certificate.CertificateCredentials;
import br.com.antigravity.fiscalapi.company.Company;
import br.com.antigravity.fiscalapi.company.FiscalEnvironment;
import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.certificado.exception.CertificadoException;
import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.dom.enuns.AmbienteEnum;
import br.com.antigravity.fiscalapi.config.AppProperties;
import br.com.swconsultoria.nfe.dom.enuns.EstadosEnum;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JavaNfeConfigurationFactory {

    private static final Logger log = LoggerFactory.getLogger(JavaNfeConfigurationFactory.class);
    private static final ZoneId BRAZIL_FISCAL_ZONE = ZoneId.of("America/Sao_Paulo");

    private final AppProperties properties;

    public JavaNfeConfigurationFactory(AppProperties properties) {
        this.properties = properties;
    }

    public ConfiguracoesNfe create(Company company, CertificateCredentials credentials) {
        try {
            Certificado certificate = CertificadoService.certificadoPfx(
                credentials.storagePath(),
                credentials.password()
            );

            // O quarto argumento e a pasta dos XSD, nao o CSC. Passar o token
            // ali fazia a biblioteca procurar o schema dentro de uma pasta com
            // o nome do proprio CSC — e a emissao morria com "Schema Nfe nao
            // Localizado: <csc>/enviNFe_v4.00.xsd", que nao aponta para nada
            // reconhecivel. O CSC nao tem lugar nesta configuracao: ele serve
            // ao QR Code da NFC-e, em outro ponto da biblioteca.
            ConfiguracoesNfe config = ConfiguracoesNfe.criarConfiguracoes(
                state(company.getStateCode()),
                environment(company.getFiscalEnvironment()),
                certificate,
                schemasPath(),
                BRAZIL_FISCAL_ZONE
            );
            config.setValidacaoDocumento(schemasDisponiveis());
            return config;
        } catch (FileNotFoundException ex) {
            throw new FiscalGatewayException("Arquivo do certificado digital nao encontrado", false);
        } catch (CertificadoException ex) {
            throw new FiscalGatewayException("Falha ao carregar certificado digital: " + ex.getMessage(), false);
        }
    }

    private String schemasPath() {
        return properties.getFiscal().getSchemasPath();
    }

    /**
     * Se ha XSD para validar contra.
     *
     * Sem os schemas em disco a java-nfe nao valida — ela quebra, com uma
     * mensagem que fala de arquivo e nao de nota. Preferimos emitir sem a
     * conferencia local e dizer isso no log: a SEFAZ valida do lado dela, e a
     * loja nao para por causa de um arquivo de apoio que ninguem baixou.
     */
    public boolean schemasDisponiveis() {
        String caminho = schemasPath();
        if (caminho == null || caminho.isBlank()) {
            log.warn("Pasta de schemas nao configurada: emissao seguira sem validacao XSD local.");
            return false;
        }

        Path pasta = Path.of(caminho);
        if (!Files.isDirectory(pasta)) {
            log.warn(
                "Pasta de schemas '{}' nao encontrada: emissao seguira sem validacao XSD local. "
                    + "Baixe os XSD da SEFAZ e aponte FISCAL_SCHEMAS_PATH para eles.",
                pasta.toAbsolutePath()
            );
            return false;
        }

        // Pasta vazia e pior que pasta ausente: existir nao quer dizer ter os
        // XSD dentro, e ligar a validacao sobre uma pasta vazia devolveria o
        // mesmo "Schema Nfe nao Localizado" que se quer evitar. O ponto de
        // montagem do volume nasce vazio — este caso e o normal, nao o raro.
        if (!contemXsd(pasta)) {
            log.warn(
                "Pasta de schemas '{}' existe mas nao tem nenhum .xsd: emissao seguira sem "
                    + "validacao XSD local.",
                pasta.toAbsolutePath()
            );
            return false;
        }

        return true;
    }

    private boolean contemXsd(Path pasta) {
        try (var arquivos = Files.walk(pasta, 2)) {
            return arquivos.anyMatch(arquivo ->
                Files.isRegularFile(arquivo)
                    && arquivo.getFileName().toString().toLowerCase().endsWith(".xsd"));
        } catch (IOException ex) {
            log.warn("Falha ao ler a pasta de schemas '{}': {}", pasta.toAbsolutePath(), ex.getMessage());
            return false;
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
