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
 * dhEmi nao aceita fracao de segundo.
 *
 * O tipo TDateTimeUTC da NF-e 4.00 termina em ss e vai direto para o fuso:
 * nao ha lugar para milissegundo nenhum. Mas o instante da emissao nasce de
 * OffsetDateTime.now(), que carrega nanossegundos, e ISO_OFFSET_DATE_TIME os
 * imprime — o XML saia com "...T01:23:45.678901-03:00" e a SEFAZ devolvia
 * "225 - Rejeicao: Falha no Schema XML do lote de NFe", que nao diz qual
 * campo.
 */
class DhEmiFormatoTests {

    private final FiscalXmlBuilder builder = new FiscalXmlBuilder();
    private final JavaNfeMapper mapper = new JavaNfeMapper();

    @Test
    void dhEmiSaiSemFracaoDeSegundo() {
        // Exatamente como a emissao faz: o relogio, com os nanos que ele tiver.
        var enviNFe = mapper.toEnviNFe(rascunhoEmitidoEm(OffsetDateTime.now()));

        String dhEmi = enviNFe.getNFe().get(0).getInfNFe().getIde().getDhEmi();

        assertThat(dhEmi)
            .as("TDateTimeUTC vai de ss direto para o fuso, sem fracao")
            .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[-+]\\d{2}:\\d{2}");
    }

    /**
     * O cDV aparece duas vezes no documento de proposito: fecha a chave de
     * acesso e ainda ocupa um elemento proprio no ide. Faltava o elemento —
     * o Id saia com os 44 digitos certos, e nada na chave denunciava a
     * ausencia. A SEFAZ so dizia "225", sem nomear o campo.
     */
    @Test
    void ideTrazOCdvEEleFechaAChaveDeAcesso() {
        var infNFe = mapper.toEnviNFe(rascunhoEmitidoEm(OffsetDateTime.now()))
            .getNFe().get(0).getInfNFe();

        String chave = infNFe.getId().substring("NFe".length());

        assertThat(chave).hasSize(44);
        assertThat(infNFe.getIde().getCDV())
            .as("o cDV do ide e o ultimo digito da chave, nao outro numero")
            .isEqualTo(chave.substring(43));
    }

    @Test
    void instanteComNanosConhecidosPerdeApenasAFracao() {
        OffsetDateTime comNanos = OffsetDateTime.parse("2026-08-20T01:23:45.678901-03:00");

        var enviNFe = mapper.toEnviNFe(rascunhoEmitidoEm(comNanos));

        assertThat(enviNFe.getNFe().get(0).getInfNFe().getIde().getDhEmi())
            .isEqualTo("2026-08-20T01:23:45-03:00");
    }

    private FiscalXmlDraft rascunhoEmitidoEm(OffsetDateTime issuedAt) {
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
            null,
            null,
            "2",
            issuedAt,
            1,
            8L,
            "PEDIDO-DHEMI-1",
            BigDecimal.valueOf(15.00),
            "Cliente Teste",
            List.of(item())
        ));
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
