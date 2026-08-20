package br.com.antigravity.fiscalapi.fiscal;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.antigravity.fiscalapi.config.AppProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A pasta de schemas é o quarto argumento de criarConfiguracoes — nunca o CSC.
 *
 * O engano custou uma emissao inteira em producao: o token do CSC entrava onde
 * a java-nfe espera o caminho dos XSD, e a assinatura morria com "Schema Nfe
 * nao Localizado: 48742c79-.../enviNFe_v4.00.xsd" — um caminho que ninguem
 * reconhece como sendo o CSC da empresa, o que torna o defeito caro de achar.
 *
 * Aqui a checagem e do que a fabrica resolve como caminho e de quando ela
 * liga a validacao, sem precisar de certificado: montar a ConfiguracoesNfe
 * de verdade exigiria um .pfx valido, e o que importa provar e outra coisa.
 */
class JavaNfeConfigurationFactoryTests {

    @Test
    void semPastaDeSchemasEmDisco_aValidacaoFicaDesligadaEmVezDeQuebrar(@TempDir Path base) {
        JavaNfeConfigurationFactory factory = factoryCom(base.resolve("nao-existe").toString());

        assertThat(factory.schemasDisponiveis())
            .as("sem XSD, a emissao segue sem validacao local em vez de falhar")
            .isFalse();
    }

    @Test
    void comOsXsdNoLugar_aValidacaoLigaSozinha(@TempDir Path base) throws Exception {
        Path schemas = Files.createDirectory(base.resolve("schemas"));
        Files.writeString(schemas.resolve("enviNFe_v4.00.xsd"), "<xs:schema/>");

        assertThat(factoryCom(schemas.toString()).schemasDisponiveis())
            .as("com os XSD no lugar, a conferencia local volta a valer")
            .isTrue();
    }

    /**
     * O ponto de montagem do volume nasce vazio: este e o caso comum numa
     * instalacao nova, nao um azar. Ligar a validacao aqui devolveria o mesmo
     * "Schema Nfe nao Localizado" que a correcao existe para evitar.
     */
    @Test
    void pastaExistenteMasSemXsd_naoLigaValidacao(@TempDir Path base) throws Exception {
        Path vazia = Files.createDirectory(base.resolve("schemas"));

        assertThat(factoryCom(vazia.toString()).schemasDisponiveis()).isFalse();
    }

    @Test
    void caminhoVazioNaoLigaValidacao(@TempDir Path base) {
        assertThat(factoryCom("  ").schemasDisponiveis()).isFalse();
    }

    private JavaNfeConfigurationFactory factoryCom(String schemasPath) {
        AppProperties properties = new AppProperties();
        properties.getFiscal().setSchemasPath(schemasPath);
        return new JavaNfeConfigurationFactory(properties);
    }
}
