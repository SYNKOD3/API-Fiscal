package br.com.antigravity.fiscalapi.fiscal;

import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class FiscalXmlPreviewRenderer {

    public String render(FiscalXmlDraft draft) {
        StringBuilder xml = new StringBuilder();
        xml.append("<NFeDraft versao=\"").append(draft.version()).append("\">\n");
        xml.append("  <infNFe Id=\"").append(escape(draft.invoiceId())).append("\">\n");
        xml.append("    <ide>\n");
        xml.append("      <mod>").append(draft.model() == br.com.antigravity.fiscalapi.document.DocumentModel.NFE ? "55" : "65").append("</mod>\n");
        xml.append("      <serie>").append(draft.seriesNumber()).append("</serie>\n");
        xml.append("      <nNF>").append(draft.invoiceNumber()).append("</nNF>\n");
        xml.append("      <cNF>").append(escape(draft.numericCode())).append("</cNF>\n");
        xml.append("      <tpEmis>").append(escape(draft.emissionType())).append("</tpEmis>\n");
        xml.append("      <tpAmb>").append(escape(draft.fiscalEnvironmentCode())).append("</tpAmb>\n");
        xml.append("      <chNFe>").append(escape(draft.accessKey())).append("</chNFe>\n");
        xml.append("    </ide>\n");
        xml.append("    <emit>\n");
        xml.append("      <CNPJ>").append(escape(draft.issuer().taxId())).append("</CNPJ>\n");
        xml.append("      <xNome>").append(escape(draft.issuer().legalName())).append("</xNome>\n");
        if (draft.issuer().tradeName() != null && !draft.issuer().tradeName().isBlank()) {
            xml.append("      <xFant>").append(escape(draft.issuer().tradeName())).append("</xFant>\n");
        }
        xml.append("      <IE>").append(escape(draft.issuer().stateRegistration())).append("</IE>\n");
        xml.append("      <CRT>").append(escape(draft.issuer().taxRegimeCode())).append("</CRT>\n");
        xml.append("      <enderEmit>\n");
        xml.append("        <xLgr>").append(escape(draft.issuer().street())).append("</xLgr>\n");
        xml.append("        <nro>").append(escape(draft.issuer().number())).append("</nro>\n");
        xml.append("        <xBairro>").append(escape(draft.issuer().district())).append("</xBairro>\n");
        xml.append("        <cMun>").append(escape(draft.issuer().cityCode())).append("</cMun>\n");
        xml.append("        <xMun>").append(escape(draft.issuer().cityName())).append("</xMun>\n");
        xml.append("        <UF>").append(escape(draft.issuer().stateCode())).append("</UF>\n");
        xml.append("        <CEP>").append(escape(draft.issuer().zipCode())).append("</CEP>\n");
        xml.append("      </enderEmit>\n");
        xml.append("    </emit>\n");

        for (FiscalXmlItemDraft item : draft.items()) {
            xml.append("    <det nItem=\"").append(item.itemNumber()).append("\">\n");
            xml.append("      <prod>\n");
            xml.append("        <cProd>").append(escape(item.sku())).append("</cProd>\n");
            xml.append("        <cEAN>").append(escape(item.gtin())).append("</cEAN>\n");
            xml.append("        <xProd>").append(escape(item.description())).append("</xProd>\n");
            xml.append("        <NCM>").append(escape(item.ncm())).append("</NCM>\n");
            if (item.cest() != null && !item.cest().isBlank()) {
                xml.append("        <CEST>").append(escape(item.cest())).append("</CEST>\n");
            }
            xml.append("        <CFOP>").append(escape(item.cfop())).append("</CFOP>\n");
            xml.append("        <uCom>").append(escape(item.unit())).append("</uCom>\n");
            xml.append("        <qCom>").append(decimal(item.quantity())).append("</qCom>\n");
            xml.append("        <vUnCom>").append(decimal(item.unitAmount())).append("</vUnCom>\n");
            xml.append("        <vProd>").append(decimal(item.totalAmount())).append("</vProd>\n");
            xml.append("      </prod>\n");
            xml.append("      <imposto>\n");
            xml.append("        <ICMS origem=\"").append(escape(item.origin())).append("\" codigo=\"").append(escape(item.icmsCode())).append("\" />\n");
            xml.append("        <PIS codigo=\"").append(escape(item.pisCode())).append("\" />\n");
            xml.append("        <COFINS codigo=\"").append(escape(item.cofinsCode())).append("\" />\n");
            xml.append("        <vTotTrib>").append(decimal(item.approximateTaxAmount())).append("</vTotTrib>\n");
            xml.append("      </imposto>\n");
            xml.append("    </det>\n");
        }

        xml.append("    <total>\n");
        xml.append("      <vNF>").append(decimal(draft.totalAmount())).append("</vNF>\n");
        xml.append("    </total>\n");
        xml.append("  </infNFe>\n");
        xml.append("</NFeDraft>\n");
        return xml.toString();
    }

    private String decimal(java.math.BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
