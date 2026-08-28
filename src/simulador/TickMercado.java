package simulador;

import model.RegistroPreco;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TickMercado {
    private final LocalDateTime dataHora;
    private final Map<String, Double> precosAtuais;
    private final List<RegistroPreco> registrosGerados;

    public TickMercado(LocalDateTime dataHora, Map<String, Double> precosAtuais, List<RegistroPreco> registrosGerados) {
        if (dataHora == null) {
            throw new IllegalArgumentException("A data/hora do tick não pode ser nula.");
        }

        this.dataHora = dataHora;
        this.precosAtuais = Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(precosAtuais, "Os preços não podem ser nulos.")));
        this.registrosGerados = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(registrosGerados, "Os registros não podem ser nulos.")));
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public Map<String, Double> getPrecosAtuais() {
        return precosAtuais;
    }

    public List<RegistroPreco> getRegistrosGerados() {
        return registrosGerados;
    }
}
