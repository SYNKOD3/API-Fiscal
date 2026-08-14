package br.com.antigravity.fiscalapi.fiscal;

import br.com.antigravity.fiscalapi.document.DocumentModel;
import java.time.OffsetDateTime;

public class NfeAccessKeyFactory {

    private static final String NORMAL_EMISSION = "1";

    public NfeAccessKey create(FiscalSubmission submission, OffsetDateTime issuedAt) {
        String numericCode = numericCode(submission);
        String baseKey = stateCode(submission.companyStateCode())
            + "%02d%02d".formatted(issuedAt.getYear() % 100, issuedAt.getMonthValue())
            + digitsOnly(submission.companyTaxId())
            + modelCode(submission.model())
            + "%03d".formatted(submission.seriesNumber())
            + "%09d".formatted(submission.invoiceNumber())
            + NORMAL_EMISSION
            + numericCode;
        String checkDigit = checkDigit(baseKey);
        return new NfeAccessKey(baseKey + checkDigit, numericCode, checkDigit);
    }

    public String emissionType() {
        return NORMAL_EMISSION;
    }

    private String numericCode(FiscalSubmission submission) {
        int hash = Math.abs((submission.externalReference() + submission.invoiceNumber()).hashCode());
        return "%08d".formatted(hash % 100_000_000);
    }

    private String checkDigit(String baseKey) {
        int weight = 2;
        int sum = 0;
        for (int index = baseKey.length() - 1; index >= 0; index--) {
            sum += Character.digit(baseKey.charAt(index), 10) * weight;
            weight = weight == 9 ? 2 : weight + 1;
        }
        int mod = sum % 11;
        int digit = 11 - mod;
        return String.valueOf(digit >= 10 ? 0 : digit);
    }

    private String modelCode(DocumentModel model) {
        return model == DocumentModel.NFE ? "55" : "65";
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String stateCode(String stateCode) {
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
