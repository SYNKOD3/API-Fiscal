package br.com.antigravity.fiscalapi.fiscal;

public class FiscalGatewayException extends RuntimeException {

    private final boolean temporary;

    public FiscalGatewayException(String message, boolean temporary) {
        super(message);
        this.temporary = temporary;
    }

    public boolean isTemporary() {
        return temporary;
    }
}
