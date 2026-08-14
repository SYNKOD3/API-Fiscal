package br.com.antigravity.fiscalapi.fiscal;

import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.swconsultoria.nfe.schemas.ObjectFactory;
import br.com.swconsultoria.nfe.schemas.TEnviNFe;
import br.com.swconsultoria.nfe.schemas.TEnderEmi;
import br.com.swconsultoria.nfe.schemas.TNFe;
import br.com.swconsultoria.nfe.schemas.TUfEmi;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import org.springframework.stereotype.Component;

@Component
public class JavaNfeMapper {

    private static final String NFE_NAMESPACE = "http://www.portalfiscal.inf.br/nfe";
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
        draft.items().forEach(item -> infNFe.getDet().add(toDet(item)));
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
        ide.setDhEmi(draft.issuedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        ide.setTpNF("1");
        ide.setIdDest("1");
        ide.setCMunFG(draft.issuer().cityCode());
        ide.setTpImp(draft.model() == DocumentModel.NFE ? "1" : "4");
        ide.setTpEmis(draft.emissionType());
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

    private TNFe.InfNFe.Det toDet(FiscalXmlItemDraft item) {
        TNFe.InfNFe.Det det = factory.createTNFeInfNFeDet();
        det.setNItem(String.valueOf(item.itemNumber()));
        det.setProd(toProd(item));
        det.setImposto(toImposto(item));
        return det;
    }

    private TNFe.InfNFe.Det.Prod toProd(FiscalXmlItemDraft item) {
        TNFe.InfNFe.Det.Prod prod = factory.createTNFeInfNFeDetProd();
        prod.setCProd(item.sku());
        prod.setCEAN(gtin(item.gtin()));
        prod.setXProd(item.description());
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

    private TNFe.InfNFe.Det.Imposto toImposto(FiscalXmlItemDraft item) {
        TNFe.InfNFe.Det.Imposto imposto = factory.createTNFeInfNFeDetImposto();
        imposto.getContent().add(element("ICMS", toIcms(item)));
        imposto.getContent().add(element("PIS", toPis(item)));
        imposto.getContent().add(element("COFINS", toCofins(item)));
        return imposto;
    }

    private TNFe.InfNFe.Det.Imposto.ICMS toIcms(FiscalXmlItemDraft item) {
        TNFe.InfNFe.Det.Imposto.ICMS icms = factory.createTNFeInfNFeDetImpostoICMS();
        TNFe.InfNFe.Det.Imposto.ICMS.ICMSSN102 icmssn102 = factory.createTNFeInfNFeDetImpostoICMSICMSSN102();
        icmssn102.setOrig(item.origin());
        icmssn102.setCSOSN(item.icmsCode());
        icms.setICMSSN102(icmssn102);
        return icms;
    }

    private TNFe.InfNFe.Det.Imposto.PIS toPis(FiscalXmlItemDraft item) {
        TNFe.InfNFe.Det.Imposto.PIS pis = factory.createTNFeInfNFeDetImpostoPIS();
        TNFe.InfNFe.Det.Imposto.PIS.PISNT pisnt = factory.createTNFeInfNFeDetImpostoPISPISNT();
        pisnt.setCST(item.pisCode());
        pis.setPISNT(pisnt);
        return pis;
    }

    private TNFe.InfNFe.Det.Imposto.COFINS toCofins(FiscalXmlItemDraft item) {
        TNFe.InfNFe.Det.Imposto.COFINS cofins = factory.createTNFeInfNFeDetImpostoCOFINS();
        TNFe.InfNFe.Det.Imposto.COFINS.COFINSNT cofinsnt = factory.createTNFeInfNFeDetImpostoCOFINSCOFINSNT();
        cofinsnt.setCST(item.cofinsCode());
        cofins.setCOFINSNT(cofinsnt);
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
