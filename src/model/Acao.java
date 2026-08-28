package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Acao implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String nomeEmpresa;
    private final String ticker;
    private final String setor;
    private final BigDecimal precoBaseInicial;

    public Acao(String nomeEmpresa, String ticker, String setor, BigDecimal precoBaseInicial) {
        this.nomeEmpresa = validarNomeEmpresa(nomeEmpresa);
        this.ticker = validarTicker(ticker);
        this.setor = validarSetor(setor);
        this.precoBaseInicial = validarPrecoBase(precoBaseInicial);
    }

    public String getNomeEmpresa() {
        return nomeEmpresa;
    }

    public String getTicker() {
        return ticker;
    }

    public String getSetor() {
        return setor;
    }

    public BigDecimal getPrecoBaseInicial() {
        return precoBaseInicial;
    }

    private static String validarNomeEmpresa(String nomeEmpresa) {
        if (nomeEmpresa == null || nomeEmpresa.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da empresa não pode ser vazio.");
        }
        return nomeEmpresa.trim();
    }

    private static String validarTicker(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            throw new IllegalArgumentException("O ticker da ação não pode ser vazio.");
        }

        String tickerNormalizado = ticker.trim().toUpperCase();
        if (!tickerNormalizado.matches("[A-Z]{3,10}[0-9]{1,2}")) {
            throw new IllegalArgumentException("Ticker inválido. Use o formato esperado, como AAPL3 ou VALE3.");
        }

        return tickerNormalizado;
    }

    private static String validarSetor(String setor) {
        if (setor == null || setor.trim().isEmpty()) {
            throw new IllegalArgumentException("O setor da ação não pode ser vazio.");
        }
        return setor.trim();
    }

    private static BigDecimal validarPrecoBase(BigDecimal precoBaseInicial) {
        if (precoBaseInicial == null) {
            throw new IllegalArgumentException("O preço base inicial não pode ser nulo.");
        }

        BigDecimal valorValidado = precoBaseInicial.setScale(2, RoundingMode.HALF_UP);
        if (valorValidado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O preço base inicial deve ser maior que zero.");
        }

        return valorValidado;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nomeEmpresa, ticker, setor, precoBaseInicial);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Acao)) {
            return false;
        }
        Acao outra = (Acao) obj;
        return Objects.equals(nomeEmpresa, outra.nomeEmpresa)
                && Objects.equals(ticker, outra.ticker)
                && Objects.equals(setor, outra.setor)
                && Objects.equals(precoBaseInicial, outra.precoBaseInicial);
    }

    @Override
    public String toString() {
        return "Acao{"
                + "nomeEmpresa='" + nomeEmpresa + '\''
                + ", ticker='" + ticker + '\''
                + ", setor='" + setor + '\''
                + ", precoBaseInicial=" + precoBaseInicial
                + '}';
    }
}
