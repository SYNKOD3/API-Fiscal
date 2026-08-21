package br.com.antigravity.fiscalapi.fiscal;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.antigravity.fiscalapi.document.FiscalItemRequest;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Precedência dos códigos de IBS/CBS: o do item, senão o da empresa.
 *
 * O layout põe o grupo dentro de cada item, e dois produtos com tributações
 * diferentes levam códigos diferentes na mesma nota. O código da empresa é
 * apenas o padrão de quem não tem tratamento próprio.
 *
 * A escolha acontece ao montar o rascunho, e é isso que estes testes cobrem.
 * Quem monta o grupo de valores no XML é a java-nfe, a partir da tabela da
 * SEFAZ, e para isso ela recebe a classificação já resolvida.
 */
class IbsCbsTests {

    private final FiscalXmlBuilder builder = new FiscalXmlBuilder();

    @Test
    void oCodigoDoItemPrevaleceSobreODaEmpresa() {
        var item = primeiroItem("000", "000001", "200", "000002");

        assertThat(item.ibsCbsCst()).isEqualTo("200");
        assertThat(item.ibsCbsClassTrib()).isEqualTo("000002");
    }

    @Test
    void semCodigoNoItemValeODaEmpresa() {
        var item = primeiroItem("000", "000001", null, null);

        assertThat(item.ibsCbsCst()).isEqualTo("000");
        assertThat(item.ibsCbsClassTrib()).isEqualTo("000001");
    }

    @Test
    void oItemSozinhoBastaQuandoAEmpresaNaoTemPadrao() {
        var item = primeiroItem(null, null, "200", "000002");

        assertThat(item.ibsCbsCst()).isEqualTo("200");
    }

    /**
     * Vazio conta como ausente. Um campo em branco no cadastro do produto é
     * "não informado", nunca "informado como nada" — sem isso, salvar o
     * cadastro com o campo em branco derrubaria o padrão da empresa.
     */
    @Test
    void brancoNoItemNaoDerrubaOPadraoDaEmpresa() {
        var item = primeiroItem("000", "000001", "   ", "");

        assertThat(item.ibsCbsCst()).isEqualTo("000");
        assertThat(item.ibsCbsClassTrib()).isEqualTo("000001");
    }

    @Test
    void semCodigoEmLugarNenhumOItemFicaSemClassificacao() {
        var item = primeiroItem(null, null, null, null);

        assertThat(item.ibsCbsClassTrib())
            .as("sem classificação a biblioteca não monta o grupo, e a SEFAZ recusa nomeando o que falta")
            .isNull();
    }

    private FiscalXmlItemDraft primeiroItem(
        String cstDaEmpresa, String classTribDaEmpresa,
        String cstDoItem, String classTribDoItem
    ) {
        return builder.build(new FiscalSubmission(
            UUID.randomUUID(),
            DocumentModel.NFCE,
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
            cstDaEmpresa,
            classTribDaEmpresa,
            "2",
            OffsetDateTime.now(),
            1,
            8L,
            "PEDIDO-IBSCBS-1",
            BigDecimal.valueOf(15.00),
            "Cliente Teste",
            List.of(new FiscalItemRequest(
                "PROD-001", "Produto A", "01012100", null, "SEM GTIN", "5102", "UN",
                BigDecimal.ONE, BigDecimal.valueOf(15.00), BigDecimal.valueOf(15.00),
                "0", "102", "49", "49", cstDoItem, classTribDoItem, BigDecimal.ZERO
            ))
        )).items().get(0);
    }
}
