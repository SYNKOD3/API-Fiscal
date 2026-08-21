package br.com.antigravity.fiscalapi.fiscal;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.antigravity.fiscalapi.document.FiscalItemRequest;
import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.schemas.TEnviNFe;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * QR Code e URL de consulta da NFC-e.
 *
 * O grupo e obrigatorio para NFC-e por regra de negocio, e o conteudo segue a
 * NT 2025-001: o codigo "online" e apenas <url>?p=<chave>|3|<ambiente>, sem
 * CSC e sem hash — o padrao anterior, que os usava, esta descontinuado.
 *
 * As URLs saem do catalogo da java-nfe, que as mantem por UF e por ambiente.
 * A ConfiguracoesNfe entra so para dizer de onde ler esse catalogo: uma vazia
 * basta, e evita exigir certificado para exercitar o que interessa aqui.
 */
class NfceQrCodeFactoryTests {

    private final FiscalXmlBuilder builder = new FiscalXmlBuilder();
    private final JavaNfeMapper mapper = new JavaNfeMapper();
    private final NfceQrCodeFactory factory = new NfceQrCodeFactory();

    @Test
    void nfceEmHomologacaoApontaParaOAmbienteDeHomologacao() {
        var enviNFe = comQrCode(DocumentModel.NFCE, "2");
        var suplemento = enviNFe.getNFe().get(0).getInfNFeSupl();

        assertThat(suplemento).isNotNull();
        assertThat(suplemento.getQrCode())
            .as("o QR Code de teste nao pode apontar para producao: abriria e diria que a nota nao existe")
            .contains("homologacao");
        assertThat(suplemento.getUrlChave()).contains("homologacao");
    }

    @Test
    void oQrCodeSegueOFormatoDaNT2025001() {
        var enviNFe = comQrCode(DocumentModel.NFCE, "2");
        var infNFe = enviNFe.getNFe().get(0).getInfNFe();
        String chave = infNFe.getId().substring("NFe".length());

        assertThat(enviNFe.getNFe().get(0).getInfNFeSupl().getQrCode())
            .endsWith("?p=" + chave + "|3|2");
    }

    @Test
    void nfceEmProducaoApontaParaProducao() {
        var suplemento = comQrCode(DocumentModel.NFCE, "1").getNFe().get(0).getInfNFeSupl();

        assertThat(suplemento.getQrCode()).doesNotContain("homologacao");
        assertThat(suplemento.getUrlChave()).doesNotContain("homologacao");
    }

    /**
     * NF-e nao tem QR Code. Preencher o grupo nela seria inventar um dado que
     * o modelo 55 nao preve.
     */
    /**
     * O codigo aplicado no XML e o devolvido precisam ser o mesmo texto.
     *
     * Quem imprime o cupom e a plataforma integradora, do outro lado, e ela
     * nao tem como recalcular: o codigo depende da URL da UF e do ambiente.
     * Devolver coisa diferente do que foi para a SEFAZ daria um cupom com QR
     * Code que nao corresponde a nota.
     */
    @Test
    void oCodigoDevolvidoEOMesmoQueFoiParaANota() {
        var draft = draftDe(DocumentModel.NFCE, "2");
        var enviNFe = mapper.toEnviNFe(draft);

        var devolvido = factory.aplicar(enviNFe, draft, new ConfiguracoesNfe());

        assertThat(devolvido).contains(enviNFe.getNFe().get(0).getInfNFeSupl().getQrCode());
    }

    @Test
    void nfeNaoDevolveCodigoAlgum() {
        var draft = draftDe(DocumentModel.NFE, "2");

        assertThat(factory.aplicar(mapper.toEnviNFe(draft), draft, new ConfiguracoesNfe()))
            .as("modelo 55 nao tem QR Code, e nulo aqui e a verdade")
            .isEmpty();
    }

    @Test
    void nfeNaoRecebeOGrupo() {
        assertThat(comQrCode(DocumentModel.NFE, "2").getNFe().get(0).getInfNFeSupl()).isNull();
    }

    private TEnviNFe comQrCode(DocumentModel modelo, String ambiente) {
        FiscalXmlDraft draft = draftDe(modelo, ambiente);
        TEnviNFe enviNFe = mapper.toEnviNFe(draft);
        factory.aplicar(enviNFe, draft, new ConfiguracoesNfe());
        return enviNFe;
    }

    private FiscalXmlDraft draftDe(DocumentModel modelo, String ambiente) {
        return builder.build(new FiscalSubmission(
            UUID.randomUUID(),
            modelo,
            "Empresa Fiscal Teste LTDA",
            "Empresa Teste",
            "55092719000191",
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
            ambiente,
            OffsetDateTime.now(),
            1,
            8L,
            "PEDIDO-QRCODE-1",
            BigDecimal.valueOf(15.00),
            "Cliente Teste",
            List.of(new FiscalItemRequest(
                "PROD-001", "Produto A", "01012100", null, "SEM GTIN", "5102", "UN",
                BigDecimal.ONE, BigDecimal.valueOf(15.00), BigDecimal.valueOf(15.00),
                "0", "102", "49", "49", null, null, BigDecimal.ZERO
            ))
        ));
    }
}
