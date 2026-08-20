package br.com.antigravity.fiscalapi.company;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Os codigos de IBS/CBS sobrevivem a sincronizacao da plataforma integradora.
 *
 * Ela envia a empresa inteira antes de cada emissao e nao conhece estes
 * campos. Se o update os sobrescrevesse com o que nao veio, os codigos que o
 * contador cadastrou seriam apagados na primeira venda seguinte — e a nota
 * voltaria a ser recusada com 1115, sem ninguem entender por que o cadastro
 * "desconfigurou sozinho".
 */
@SpringBootTest
class CompanyIbsCbsTests {

    @Autowired
    private CompanyService companyService;

    @Test
    void sincronizacaoSemOsCodigosMantemOsQueOContadorCadastrou() {
        var criada = companyService.create(pedido("tenant-ibs", "merchant-ibs", "000", "000001"));

        assertThat(criada.ibsCbsCst()).isEqualTo("000");
        assertThat(criada.ibsCbsClassTrib()).isEqualTo("000001");

        // A sincronizacao da plataforma: manda a empresa toda, sem os codigos.
        var apos = companyService.update(criada.id(), pedido("tenant-ibs", "merchant-ibs", null, null));

        assertThat(apos.ibsCbsCst()).isEqualTo("000");
        assertThat(apos.ibsCbsClassTrib()).isEqualTo("000001");
    }

    @Test
    void informarOsCodigosDeNovoTrocaOsValores() {
        var criada = companyService.create(pedido("tenant-ibs-2", "merchant-ibs-2", "000", "000001"));

        var apos = companyService.update(criada.id(), pedido("tenant-ibs-2", "merchant-ibs-2", "200", "000002"));

        assertThat(apos.ibsCbsCst()).isEqualTo("200");
        assertThat(apos.ibsCbsClassTrib()).isEqualTo("000002");
    }

    private CreateCompanyRequest pedido(String tenantId, String merchantId, String cst, String classTrib) {
        // O CNPJ e unico por empresa; deriva-lo do tenant evita que dois testes
        // desta classe disputem o mesmo cadastro.
        String taxId = "9234567800017" + (tenantId.endsWith("2") ? "2" : "1");
        return new CreateCompanyRequest(
            tenantId,
            merchantId,
            "https://integrator.example/webhooks/fiscal",
            "Empresa IBS CBS LTDA",
            taxId,
            "923456771",
            "BA",
            "Empresa IBS CBS",
            "Rua Fiscal",
            "300",
            null,
            "Centro",
            "2927408",
            "Salvador",
            "40000000",
            "7133334444",
            TaxRegime.SIMPLES_NACIONAL,
            FiscalEnvironment.HOMOLOGATION,
            null,
            null,
            "000001",
            "token-csc",
            cst,
            classTrib,
            1,
            1L,
            1,
            1L
        );
    }
}
