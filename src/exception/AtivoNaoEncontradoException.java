package exception;

public class AtivoNaoEncontradoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AtivoNaoEncontradoException() {
        super("Ativo não encontrado para o ticker informado.");
    }

    public AtivoNaoEncontradoException(String message) {
        super(message);
    }

    public AtivoNaoEncontradoException(String message, Throwable cause) {
        super(message, cause);
    }
}
