package br.com.antigravity.fiscalapi.fiscal;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.antigravity.fiscalapi.document.FiscalItemRequest;
import br.com.swconsultoria.nfe.schemas.TNFe;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Em homologacao a SEFAZ dita a descricao do primeiro item.
 *
 * A regra existe para que nenhuma nota de teste possa passar por real: o
 * primeiro item precisa dizer, com todas as letras, que o documento nao tem
 * valor fiscal. Fora dela vem a rejeicao 373.
 *
 * O recorte importa nos dois sentidos — so o primeiro item, e so em
 * homologacao. Trocar a descricao dos demais faria a nota de teste deixar de
 * exercitar o que se quer testar; trocar em producao seria emitir a venda de
 * um produto que nao existe.
 */
class DescricaoHomologacaoTests {

    private static final String EXIGIDA =
        "NOTA FISCAL EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL";

    private final FiscalXmlBuilder builder = new FiscalXmlBuilder();
    private final JavaNfeMapper mapper = new JavaNfeMapper();

    @Test
    void emHomologacaoOPrimeiroItemUsaADescricaoExigida() {
        var itens = itensDe("2");

        assertThat(itens.get(0).getProd().getXProd()).isEqualTo(EXIGIDA);
    }

    @Test
    void emHomologacaoOsDemaisItensMantemADescricaoReal() {
        var itens = itensDe("2");

        assertThat(itens.get(1).getProd().getXProd()).isEqualTo("Produto B");
    }

    @Test
    void emProducaoNenhumItemEMascarado() {
        var itens = itensDe("1");

        assertThat(itens.get(0).getProd().getXProd()).isEqualTo("Produto A");
        assertThat(itens.get(1).getProd().getXProd()).isEqualTo("Produto B");
    }

    private List<TNFe.InfNFe.Det> itensDe(String ambiente) {
        return mapper.toEnviNFe(builder.build(new FiscalSubmission(
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
            ambiente,
            OffsetDateTime.now(),
            1,
            8L,
            "PEDIDO-HOMOLOG-1",
            BigDecimal.valueOf(30.00),
            "Cliente Teste",
            List.of(item("PROD-001", "Produto A"), item("PROD-002", "Produto B"))
        ))).getNFe().get(0).getInfNFe().getDet();
    }

    private FiscalItemRequest item(String sku, String descricao) {
        return new FiscalItemRequest(
            sku, descricao, "01012100", null, "SEM GTIN", "5102", "UN",
            BigDecimal.ONE, BigDecimal.valueOf(15.00), BigDecimal.valueOf(15.00),
            "0", "102", "49", "49", BigDecimal.ZERO
        );
    }
}
