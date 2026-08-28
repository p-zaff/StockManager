package dao;

import model.Acao;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public final class AcaoDAO extends AbstractBinaryDAO<Acao> {
    private static final Path ARQUIVO_ACOES = Paths.get("data", "binarios", "acoes", "acoes.bin");
    private static final AcaoDAO INSTANCIA = new AcaoDAO();

    private AcaoDAO() {
        super(ARQUIVO_ACOES, Acao.class);
    }

    public static AcaoDAO getInstance() {
        return INSTANCIA;
    }

    public synchronized Optional<Acao> buscarPorTicker(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            return Optional.empty();
        }

        String tickerNormalizado = ticker.trim().toUpperCase();
        List<Acao> acoes = listarTodos();

        for (Acao acao : acoes) {
            if (acao.getTicker() != null && acao.getTicker().equalsIgnoreCase(tickerNormalizado)) {
                return Optional.of(acao);
            }
        }

        return Optional.empty();
    }
}
