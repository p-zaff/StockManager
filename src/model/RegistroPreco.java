package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class RegistroPreco implements Serializable {
    private static final long serialVersionUID = 1L;

    private String ticker;
    private LocalDateTime dataHora;
    private double valor;

    public RegistroPreco() {
        this(null, LocalDateTime.now(), 0.0d);
    }

    public RegistroPreco(String ticker, LocalDateTime dataHora, double valor) {
        setTicker(ticker);
        setDataHora(dataHora);
        setValor(valor);
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            throw new IllegalArgumentException("O ticker do registro de preço não pode ser vazio.");
        }
        this.ticker = ticker.trim().toUpperCase();
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        if (dataHora == null) {
            throw new IllegalArgumentException("A data/hora do preço não pode ser nula.");
        }
        this.dataHora = dataHora;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        if (valor <= 0.0d) {
            throw new IllegalArgumentException("O valor do preço deve ser maior que zero.");
        }
        this.valor = valor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticker, dataHora, valor);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RegistroPreco)) {
            return false;
        }
        RegistroPreco outro = (RegistroPreco) obj;
        return Double.compare(outro.valor, valor) == 0
                && Objects.equals(ticker, outro.ticker)
                && Objects.equals(dataHora, outro.dataHora);
    }

    @Override
    public String toString() {
        return "RegistroPreco{" +
                "ticker='" + ticker + '\'' +
                ", dataHora=" + dataHora +
                ", valor=" + valor +
                '}';
    }
}
