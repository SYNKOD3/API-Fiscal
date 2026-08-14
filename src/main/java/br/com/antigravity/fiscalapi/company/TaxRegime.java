package br.com.antigravity.fiscalapi.company;

public enum TaxRegime {
    SIMPLES_NACIONAL("1"),
    SIMPLES_NACIONAL_EXCESS_SUB_LIMIT("2"),
    NORMAL("3");

    private final String nfeCode;

    TaxRegime(String nfeCode) {
        this.nfeCode = nfeCode;
    }

    public String getNfeCode() {
        return nfeCode;
    }
}
