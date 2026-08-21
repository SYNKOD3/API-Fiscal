package br.com.antigravity.fiscalapi.fiscal;

import java.util.ArrayList;
import org.springframework.stereotype.Component;

@Component
public class FiscalXmlBuilder {

    private static final String NFE_VERSION = "4.00";
    private final NfeAccessKeyFactory accessKeyFactory = new NfeAccessKeyFactory();

    public FiscalXmlDraft build(FiscalSubmission submission) {
        NfeAccessKey accessKey = accessKeyFactory.create(submission, submission.issuedAt());
        var items = new ArrayList<FiscalXmlItemDraft>();
        for (int index = 0; index < submission.items().size(); index++) {
            var item = submission.items().get(index);
            items.add(new FiscalXmlItemDraft(
                index + 1,
                item.sku(),
                item.description(),
                item.ncm(),
                item.cest(),
                item.gtin(),
                item.cfop(),
                item.unit(),
                item.quantity(),
                item.unitAmount(),
                item.totalAmount(),
                item.origin(),
                item.icmsCode(),
                item.pisCode(),
                item.cofinsCode(),
                // O IBS/CBS e por item no layout, dentro de det/imposto, ao lado
                // do ICMS e do PIS. O codigo da empresa e so o padrao de quem
                // nao tem tratamento proprio — resolver aqui evita que cada
                // lugar que le o rascunho precise lembrar dessa precedencia.
                preferindoODoItem(item.ibsCbsCst(), submission.ibsCbsCst()),
                preferindoODoItem(item.ibsCbsClassTrib(), submission.ibsCbsClassTrib()),
                item.approximateTaxAmount()
            ));
        }

        return new FiscalXmlDraft(
            submission.model(),
            NFE_VERSION,
            "NFe" + accessKey.value(),
            accessKey.value(),
            accessKey.numericCode(),
            accessKey.checkDigit(),
            accessKeyFactory.emissionType(),
            submission.fiscalEnvironmentCode(),
            submission.issuedAt(),
            issuer(submission),
            submission.seriesNumber(),
            submission.invoiceNumber(),
            submission.totalAmount(),
            items
        );
    }

    private FiscalIssuerDraft issuer(FiscalSubmission submission) {
        return new FiscalIssuerDraft(
            submission.companyLegalName(),
            submission.companyTradeName(),
            submission.companyTaxId(),
            submission.stateRegistration(),
            submission.companyStateCode(),
            submission.companyStreet(),
            submission.companyAddressNumber(),
            submission.companyAddressComplement(),
            submission.companyDistrict(),
            submission.companyCityCode(),
            submission.companyCityName(),
            submission.companyZipCode(),
            submission.companyPhone(),
            submission.companyTaxRegimeCode(),
            submission.ibsCbsCst(),
            submission.ibsCbsClassTrib()
        );
    }

    /// Vazio conta como ausente: um campo em branco no cadastro do produto e
    /// "nao informado", nao "informado como nada".
    private String preferindoODoItem(String doItem, String daEmpresa) {
        return doItem == null || doItem.isBlank() ? daEmpresa : doItem;
    }
}
