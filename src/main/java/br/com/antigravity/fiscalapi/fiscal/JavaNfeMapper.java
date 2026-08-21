package br.com.antigravity.fiscalapi.fiscal;

import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.swconsultoria.nfe.schemas.ObjectFactory;
import br.com.swconsultoria.nfe.schemas.TEnviNFe;
import br.com.swconsultoria.nfe.schemas.TEnderEmi;
import br.com.swconsultoria.nfe.schemas.TNFe;
import br.com.swconsultoria.nfe.schemas.TTribNFe;
import br.com.swconsultoria.nfe.schemas.TUfEmi;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import org.springframework.stereotype.Component;

@Component
public class JavaNfeMapper {

    private static final String NFE_NAMESPACE = "http://www.portalfiscal.inf.br/nfe";

    /**
     * Formato de data-hora da NF-e: segundos e o fuso, sem fracao.
     *
     * O TDateTimeUTC do layout 4.00 termina em ss e vai direto para o fuso —
     * milissegundo ali e schema invalido. ISO_OFFSET_DATE_TIME nao serve
     * porque imprime a fracao quando o instante tem, e o instante vem de
     * OffsetDateTime.now(), que sempre tem. O resultado era a SEFAZ devolvendo
     * "225 - Falha no Schema XML do lote de NFe", que nao nomeia o campo.
     *
     * O 'xxx' minusculo tambem e proposital: o 'XXX' maiusculo imprimiria "Z"
     * num fuso zero, e a NF-e so aceita deslocamento explicito.
     */
    private static final DateTimeFormatter DATA_HORA_FISCAL =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    private final ObjectFactory factory = new ObjectFactory();

    public TEnviNFe toEnviNFe(FiscalXmlDraft draft) {
        TEnviNFe enviNFe = factory.createTEnviNFe();
        enviNFe.setVersao(draft.version());
        enviNFe.setIdLote("%015d".formatted(draft.invoiceNumber()));
        enviNFe.setIndSinc("1");
        enviNFe.getNFe().add(toNFe(draft));
        return enviNFe;
    }

    private TNFe toNFe(FiscalXmlDraft draft) {
        TNFe nfe = factory.createTNFe();
        TNFe.InfNFe infNFe = factory.createTNFeInfNFe();
        infNFe.setVersao(draft.version());
        infNFe.setId(draft.invoiceId());
        infNFe.setIde(toIde(draft));
        infNFe.setEmit(toEmit(draft));
        draft.items().forEach(item -> infNFe.getDet().add(toDet(draft, item)));
        infNFe.setTotal(toTotal(draft));
        infNFe.setTransp(toTransp());
        infNFe.setPag(toPag(draft));
        nfe.setInfNFe(infNFe);
        return nfe;
    }

    private TNFe.InfNFe.Ide toIde(FiscalXmlDraft draft) {
        TNFe.InfNFe.Ide ide = factory.createTNFeInfNFeIde();
        ide.setCUF(fiscalStateCode(draft.issuer().stateCode()));
        ide.setCNF(draft.numericCode());
        ide.setNatOp("VENDA");
        ide.setMod(draft.model() == DocumentModel.NFE ? "55" : "65");
        ide.setSerie(String.valueOf(draft.seriesNumber()));
        ide.setNNF(String.valueOf(draft.invoiceNumber()));
        ide.setDhEmi(draft.issuedAt().format(DATA_HORA_FISCAL));
        ide.setTpNF("1");
        ide.setIdDest("1");
        ide.setCMunFG(draft.issuer().cityCode());
        ide.setTpImp(draft.model() == DocumentModel.NFE ? "1" : "4");
        ide.setTpEmis(draft.emissionType());
        // O digito verificador vai em dobro de proposito no layout: fecha a
        // chave de acesso e ainda aparece sozinho aqui, entre tpEmis e tpAmb.
        // Faltava desde a primeira versao — o Id saia com os 44 digitos certos
        // e o <cDV> nao existia, quebrando a sequencia obrigatoria do <ide>.
        ide.setCDV(draft.checkDigit());
        ide.setTpAmb(draft.fiscalEnvironmentCode());
        ide.setFinNFe("1");
        ide.setIndFinal(draft.model() == DocumentModel.NFCE ? "1" : "0");
        ide.setIndPres(draft.model() == DocumentModel.NFCE ? "1" : "0");
        ide.setProcEmi("0");
        ide.setVerProc("fiscal-api-0.0.1");
        return ide;
    }

    private TNFe.InfNFe.Emit toEmit(FiscalXmlDraft draft) {
        TNFe.InfNFe.Emit emit = factory.createTNFeInfNFeEmit();
        emit.setCNPJ(draft.issuer().taxId());
        emit.setXNome(draft.issuer().legalName());
        if (draft.issuer().tradeName() != null && !draft.issuer().tradeName().isBlank()) {
            emit.setXFant(draft.issuer().tradeName());
        }
        emit.setEnderEmit(toEnderEmit(draft.issuer()));
        emit.setIE(draft.issuer().stateRegistration());
        emit.setCRT(draft.issuer().taxRegimeCode());
        return emit;
    }

    private TEnderEmi toEnderEmit(FiscalIssuerDraft issuer) {
        TEnderEmi address = factory.createTEnderEmi();
        address.setXLgr(issuer.street());
        address.setNro(issuer.number());
        if (issuer.complement() != null && !issuer.complement().isBlank()) {
            address.setXCpl(issuer.complement());
        }
        address.setXBairro(issuer.district());
        address.setCMun(issuer.cityCode());
        address.setXMun(issuer.cityName());
        address.setUF(TUfEmi.fromValue(issuer.stateCode()));
        address.setCEP(digitsOnly(issuer.zipCode()));
        address.setCPais("1058");
        address.setXPais("BRASIL");
        if (issuer.phone() != null && !issuer.phone().isBlank()) {
            address.setFone(digitsOnly(issuer.phone()));
        }
        return address;
    }

    private TNFe.InfNFe.Det toDet(FiscalXmlDraft draft, FiscalXmlItemDraft item) {
        TNFe.InfNFe.Det det = factory.createTNFeInfNFeDet();
        det.setNItem(String.valueOf(item.itemNumber()));
        det.setProd(toProd(draft, item));
        det.setImposto(toImposto(draft, item));
        return det;
    }

    private TNFe.InfNFe.Det.Prod toProd(FiscalXmlDraft draft, FiscalXmlItemDraft item) {
        TNFe.InfNFe.Det.Prod prod = factory.createTNFeInfNFeDetProd();
        prod.setCProd(item.sku());
        prod.setCEAN(gtin(item.gtin()));
        prod.setXProd(descricaoDoProduto(draft, item));
        prod.setNCM(item.ncm());
        if (item.cest() != null && !item.cest().isBlank()) {
            prod.setCEST(item.cest());
        }
        prod.setCFOP(item.cfop());
        prod.setUCom(item.unit());
        prod.setQCom(decimal(item.quantity(), 4));
        prod.setVUnCom(decimal(item.unitAmount(), 10));
        prod.setVProd(decimal(item.totalAmount(), 2));
        prod.setCEANTrib(gtin(item.gtin()));
        prod.setUTrib(item.unit());
        prod.setQTrib(decimal(item.quantity(), 4));
        prod.setVUnTrib(decimal(item.unitAmount(), 10));
        prod.setIndTot("1");
        return prod;
    }

    /**
     * Em homologacao, a descricao do primeiro item e ditada pela SEFAZ.
     *
     * A regra existe para que nenhuma nota de teste possa ser confundida com
     * uma real: o primeiro item precisa dizer, com todas as letras, que o
     * documento nao tem valor fiscal. Fora dela vem a rejeicao 373, que ao
     * menos e explicita sobre o que espera.
     *
     * Vale so para o primeiro item — os demais mantem a descricao de verdade,
     * senao a nota de teste deixaria de exercitar o que se quer testar.
     */
    private static final String DESCRICAO_HOMOLOGACAO =
        "NOTA FISCAL EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL";

    private String descricaoDoProduto(FiscalXmlDraft draft, FiscalXmlItemDraft item) {
        boolean homologacao = "2".equals(draft.fiscalEnvironmentCode());
        return homologacao && item.itemNumber() == 1
            ? DESCRICAO_HOMOLOGACAO
            : item.description();
    }

    private TNFe.InfNFe.Det.Imposto toImposto(FiscalXmlDraft draft, FiscalXmlItemDraft item) {
        TNFe.InfNFe.Det.Imposto imposto = factory.createTNFeInfNFeDetImposto();
        imposto.getContent().add(element("ICMS", toIcms(item)));
        imposto.getContent().add(element("PIS", toPis(item)));
        imposto.getContent().add(element("COFINS", toCofins(item)));
        toIbsCbs(item).ifPresent(tributo -> imposto.getContent().add(element("IBSCBS", tributo)));
        return imposto;
    }

    /**
     * IBS e CBS, da reforma tributaria — por item, como manda o layout.
     *
     * O grupo mora em det/imposto, ao lado do ICMS e do PIS: dois produtos com
     * tratamentos diferentes levam codigos diferentes na mesma nota. O codigo
     * da empresa e apenas o padrao de quem nao tem tratamento proprio, e a
     * escolha entre um e outro ja foi feita ao montar o rascunho.
     *
     * O CST e a classificacao definem o tratamento tributario, e o schema
     * aceita qualquer numero no formato: chutar um valor geraria documento que
     * a SEFAZ autoriza e que entra errado na escrituracao. Por isso a ausencia
     * omite o grupo em vez de inventar conteudo — a SEFAZ recusa com 1115, que
     * e o desfecho visivel e corrigivel.
     */
    private Optional<TTribNFe> toIbsCbs(FiscalXmlItemDraft item) {
        String cst = item.ibsCbsCst();
        String classTrib = item.ibsCbsClassTrib();
        if (cst == null || cst.isBlank() || classTrib == null || classTrib.isBlank()) {
            return Optional.empty();
        }

        TTribNFe tributo = factory.createTTribNFe();
        tributo.setCST(cst);
        tributo.setCClassTrib(classTrib);
        return Optional.of(tributo);
    }

    private TNFe.InfNFe.Det.Imposto.ICMS toIcms(FiscalXmlItemDraft item) {
        TNFe.InfNFe.Det.Imposto.ICMS icms = factory.createTNFeInfNFeDetImpostoICMS();
        TNFe.InfNFe.Det.Imposto.ICMS.ICMSSN102 icmssn102 = factory.createTNFeInfNFeDetImpostoICMSICMSSN102();
        icmssn102.setOrig(item.origin());
        icmssn102.setCSOSN(item.icmsCode());
        icms.setICMSSN102(icmssn102);
        return icms;
    }

    /**
     * O CST decide o grupo, e o grupo errado e schema invalido.
     *
     * PISNT/COFINSNT sao "nao tributado" e so aceitam 04 a 09. O 49 — que e o
     * que o Simples Nacional usa, e o que chega aqui — e "outras operacoes", e
     * mora em PISOutr/COFINSOutr, com base, aliquota e valor. Mandar 49 dentro
     * de NT fazia a SEFAZ recusar o lote inteiro com 225, sem dizer o campo.
     */
    private static final Set<String> CST_NAO_TRIBUTADO =
        Set.of("04", "05", "06", "07", "08", "09");

    private TNFe.InfNFe.Det.Imposto.PIS toPis(FiscalXmlItemDraft item) {
        TNFe.InfNFe.Det.Imposto.PIS pis = factory.createTNFeInfNFeDetImpostoPIS();
        if (CST_NAO_TRIBUTADO.contains(item.pisCode())) {
            TNFe.InfNFe.Det.Imposto.PIS.PISNT pisnt = factory.createTNFeInfNFeDetImpostoPISPISNT();
            pisnt.setCST(item.pisCode());
            pis.setPISNT(pisnt);
            return pis;
        }

        TNFe.InfNFe.Det.Imposto.PIS.PISOutr outr = factory.createTNFeInfNFeDetImpostoPISPISOutr();
        outr.setCST(item.pisCode());
        outr.setVBC(decimal(BigDecimal.ZERO, 2));
        outr.setPPIS(decimal(BigDecimal.ZERO, 4));
        outr.setVPIS(decimal(BigDecimal.ZERO, 2));
        pis.setPISOutr(outr);
        return pis;
    }

    private TNFe.InfNFe.Det.Imposto.COFINS toCofins(FiscalXmlItemDraft item) {
        TNFe.InfNFe.Det.Imposto.COFINS cofins = factory.createTNFeInfNFeDetImpostoCOFINS();
        if (CST_NAO_TRIBUTADO.contains(item.cofinsCode())) {
            TNFe.InfNFe.Det.Imposto.COFINS.COFINSNT cofinsnt =
                factory.createTNFeInfNFeDetImpostoCOFINSCOFINSNT();
            cofinsnt.setCST(item.cofinsCode());
            cofins.setCOFINSNT(cofinsnt);
            return cofins;
        }

        TNFe.InfNFe.Det.Imposto.COFINS.COFINSOutr outr =
            factory.createTNFeInfNFeDetImpostoCOFINSCOFINSOutr();
        outr.setCST(item.cofinsCode());
        outr.setVBC(decimal(BigDecimal.ZERO, 2));
        outr.setPCOFINS(decimal(BigDecimal.ZERO, 4));
        outr.setVCOFINS(decimal(BigDecimal.ZERO, 2));
        cofins.setCOFINSOutr(outr);
        return cofins;
    }

    private TNFe.InfNFe.Total toTotal(FiscalXmlDraft draft) {
        TNFe.InfNFe.Total total = factory.createTNFeInfNFeTotal();
        TNFe.InfNFe.Total.ICMSTot icmsTot = factory.createTNFeInfNFeTotalICMSTot();
        String zero = decimal(BigDecimal.ZERO, 2);
        BigDecimal productTotal = draft.items().stream()
            .map(FiscalXmlItemDraft::totalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal approximateTaxTotal = draft.items().stream()
            .map(FiscalXmlItemDraft::approximateTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        icmsTot.setVBC(zero);
        icmsTot.setVICMS(zero);
        icmsTot.setVICMSDeson(zero);
        icmsTot.setVFCP(zero);
        icmsTot.setVBCST(zero);
        icmsTot.setVST(zero);
        icmsTot.setVFCPST(zero);
        icmsTot.setVFCPSTRet(zero);
        icmsTot.setVProd(decimal(productTotal, 2));
        icmsTot.setVFrete(zero);
        icmsTot.setVSeg(zero);
        icmsTot.setVDesc(zero);
        icmsTot.setVII(zero);
        icmsTot.setVIPI(zero);
        icmsTot.setVIPIDevol(zero);
        icmsTot.setVPIS(zero);
        icmsTot.setVCOFINS(zero);
        icmsTot.setVOutro(zero);
        icmsTot.setVNF(decimal(draft.totalAmount(), 2));
        icmsTot.setVTotTrib(decimal(approximateTaxTotal, 2));
        total.setICMSTot(icmsTot);
        return total;
    }

    private TNFe.InfNFe.Transp toTransp() {
        TNFe.InfNFe.Transp transp = factory.createTNFeInfNFeTransp();
        transp.setModFrete("9");
        return transp;
    }

    private TNFe.InfNFe.Pag toPag(FiscalXmlDraft draft) {
        TNFe.InfNFe.Pag pag = factory.createTNFeInfNFePag();
        TNFe.InfNFe.Pag.DetPag detPag = factory.createTNFeInfNFePagDetPag();
        detPag.setIndPag("0");
        detPag.setTPag("01");
        detPag.setVPag(decimal(draft.totalAmount(), 2));
        pag.getDetPag().add(detPag);
        return pag;
    }

    private <T> JAXBElement<T> element(String name, T value) {
        @SuppressWarnings("unchecked")
        Class<T> declaredType = (Class<T>) value.getClass();
        return new JAXBElement<>(new QName(NFE_NAMESPACE, name), declaredType, value);
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String gtin(String value) {
        return value == null || value.isBlank() ? "SEM GTIN" : value;
    }

    private String decimal(BigDecimal value, int scale) {
        return value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }

    private String fiscalStateCode(String stateCode) {
        return switch (stateCode) {
            case "RO" -> "11";
            case "AC" -> "12";
            case "AM" -> "13";
            case "RR" -> "14";
            case "PA" -> "15";
            case "AP" -> "16";
            case "TO" -> "17";
            case "MA" -> "21";
            case "PI" -> "22";
            case "CE" -> "23";
            case "RN" -> "24";
            case "PB" -> "25";
            case "PE" -> "26";
            case "AL" -> "27";
            case "SE" -> "28";
            case "BA" -> "29";
            case "MG" -> "31";
            case "ES" -> "32";
            case "RJ" -> "33";
            case "SP" -> "35";
            case "PR" -> "41";
            case "SC" -> "42";
            case "RS" -> "43";
            case "MS" -> "50";
            case "MT" -> "51";
            case "GO" -> "52";
            case "DF" -> "53";
            default -> throw new FiscalGatewayException("UF fiscal invalida: " + stateCode, false);
        };
    }
}
