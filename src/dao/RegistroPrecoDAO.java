package dao;

import model.RegistroPreco;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RegistroPrecoDAO extends AbstractBinaryDAO<RegistroPreco> {
    private static final Path ARQUIVO_PRECOS = Paths.get("data", "binarios", "precos", "precos.bin");
    private static final RegistroPrecoDAO INSTANCIA = new RegistroPrecoDAO();

    private RegistroPrecoDAO() {
        super(ARQUIVO_PRECOS, RegistroPreco.class);
    }

    public static RegistroPrecoDAO getInstance() {
        return INSTANCIA;
    }

    public synchronized List<RegistroPreco> listarPorTicker(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String tickerNormalizado = ticker.trim().toUpperCase();
        List<RegistroPreco> filtrados = new ArrayList<>();

        for (RegistroPreco registroPreco : listarTodos()) {
            if (registroPreco.getTicker() != null && registroPreco.getTicker().equalsIgnoreCase(tickerNormalizado)) {
                filtrados.add(registroPreco);
            }
        }

        filtrados.sort(Comparator.comparing(RegistroPreco::getDataHora));
        return filtrados;
    }
}
