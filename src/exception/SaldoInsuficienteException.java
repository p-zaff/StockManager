package exception;

public class SaldoInsuficienteException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SaldoInsuficienteException() {
        super("Saldo insuficiente para realizar esta operação.");
    }

    public SaldoInsuficienteException(String message) {
        super(message);
    }

    public SaldoInsuficienteException(String message, Throwable cause) {
        super(message, cause);
    }
}
