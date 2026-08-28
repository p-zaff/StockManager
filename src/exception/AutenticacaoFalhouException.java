package exception;

public class AutenticacaoFalhouException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AutenticacaoFalhouException() {
        super("Falha na autenticação. Credenciais inválidas.");
    }

    public AutenticacaoFalhouException(String message) {
        super(message);
    }

    public AutenticacaoFalhouException(String message, Throwable cause) {
        super(message, cause);
    }
}
