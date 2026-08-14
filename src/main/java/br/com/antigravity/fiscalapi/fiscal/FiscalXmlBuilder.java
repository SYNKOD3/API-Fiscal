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
            submission.companyTaxRegimeCode()
        );
    }
}
