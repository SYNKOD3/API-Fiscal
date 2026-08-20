package br.com.antigravity.fiscalapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.antigravity.fiscalapi.document.FiscalItemRequest;
import br.com.antigravity.fiscalapi.fiscal.FiscalSubmission;
import br.com.antigravity.fiscalapi.fiscal.FiscalXmlBuilder;
import br.com.antigravity.fiscalapi.fiscal.JavaNfeMapper;
import br.com.swconsultoria.nfe.schemas.TEnviNFe;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * A assinatura do XML fiscal depende de JAXB do namespace javax.
 *
 * A java-nfe e uma biblioteca da era JAXB 2.x: ela serializa chamando
 * javax.xml.bind.JAXBContext.newInstance(). O Spring Boot 3 traz o mundo
 * jakarta, e o BOM dele fixa com.sun.xml.bind:jaxb-impl na 4.x — que registra
 * apenas jakarta.xml.bind.JAXBContextFactory, nada para o namespace javax.
 *
 * Sem implementacao javax no classpath, o ContextFinder cai no padrao de
 * plataforma (com.sun.xml.internal.bind.v2.ContextFactory), classe interna do
 * JDK 8 removida no Java 9. O efeito e o pior tipo de falha: compila, sobe,
 * atende requisicao — e morre so na hora de assinar, em producao, com um
 * ClassNotFoundException que nao diz de onde veio.
 *
 * Estes testes falham no lugar onde o defeito e barato de achar. Se quebrarem,
 * alguem tirou a implementacao javax do classpath (ver o pin de
 * com.sun.xml.bind:jaxb-impl no pom).
 */
class JaxbContextTests {

    private final FiscalXmlBuilder builder = new FiscalXmlBuilder();
    private final JavaNfeMapper mapper = new JavaNfeMapper();

    @Test
    void contextoJaxbDoNamespaceJavaxExisteParaOSchemaDaNfe() {
        assertThatCode(() -> javax.xml.bind.JAXBContext.newInstance(TEnviNFe.class))
            .doesNotThrowAnyException();
    }

    /**
     * Criar o contexto nao basta: e a serializacao que a assinatura faz de
     * verdade, e e nela que uma implementacao incompleta apareceria.
     */
    @Test
    void serializaOEnvelopeDaNfeComOMesmoCaminhoQueAAssinaturaUsa() throws Exception {
        TEnviNFe enviNFe = mapper.toEnviNFe(builder.build(new FiscalSubmission(
            UUID.randomUUID(),
            DocumentModel.NFCE,
            "Empresa Fiscal Teste LTDA",
            "Empresa Teste",
            "12345678000199",
            "SP",
            "123456789",
            "Rua Fiscal",
            "100",
            null,
            "Centro",
            "3550308",
            "Sao Paulo",
            "01000000",
            "1133334444",
            "1",
            null,
            null,
            "2",
            OffsetDateTime.parse("2026-08-19T18:00:00-03:00"),
            1,
            8L,
            "PEDIDO-JAXB-1",
            BigDecimal.valueOf(15.00),
            "Cliente Teste",
            List.of(item())
        )));

        StringWriter xml = new StringWriter();
        javax.xml.bind.JAXBContext.newInstance(TEnviNFe.class)
            .createMarshaller()
            .marshal(new br.com.swconsultoria.nfe.schemas.ObjectFactory().createEnviNFe(enviNFe), xml);

        assertThat(xml.toString()).contains("<enviNFe", "versao=\"4.00\"", "<infNFe");
    }

    private FiscalItemRequest item() {
        return new FiscalItemRequest(
            "PROD-001",
            "Produto A",
            "01012100",
            null,
            "SEM GTIN",
            "5102",
            "UN",
            BigDecimal.ONE,
            BigDecimal.valueOf(15.00),
            BigDecimal.valueOf(15.00),
            "0",
            "102",
            "49",
            "49",
            BigDecimal.ZERO
        );
    }
}
