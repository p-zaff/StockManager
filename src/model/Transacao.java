package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

public class Transacao implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String ticker;
    private final TipoTransacao tipo;
    private final int quantidade;
    private final BigDecimal precoUnitario;
    private final LocalDateTime dataHora;
    private final int quantidadeDisponivel;

    public Transacao(Long id, String ticker, TipoTransacao tipo, int quantidade, BigDecimal precoUnitario, LocalDateTime dataHora) {
        this(id, ticker, tipo, quantidade, precoUnitario, dataHora, quantidade);
    }

    public Transacao(Long id, String ticker, TipoTransacao tipo, int quantidade, BigDecimal precoUnitario,
                    LocalDateTime dataHora, int quantidadeDisponivel) {
        this.id = validarId(id);
        this.ticker = validarTicker(ticker);
        this.tipo = validarTipo(tipo);
        this.quantidade = validarQuantidade(quantidade);
        this.precoUnitario = validarPreco(precoUnitario);
        this.dataHora = validarDataHora(dataHora);
        this.quantidadeDisponivel = validarQuantidadeDisponivel(quantidadeDisponivel, quantidade);
    }

    public Long getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public int getQuantidadeDisponivel() {
        return quantidadeDisponivel;
    }

    private static Long validarId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O identificador da transação não pode ser nulo.");
        }
        return id;
    }

    private static String validarTicker(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            throw new IllegalArgumentException("O ticker da transação não pode ser vazio.");
        }
        return ticker.trim().toUpperCase();
    }

    private static TipoTransacao validarTipo(TipoTransacao tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("O tipo da transação não pode ser nulo.");
        }
        return tipo;
    }

    private static int validarQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade da transação deve ser maior que zero.");
        }
        return quantidade;
    }

    private static BigDecimal validarPreco(BigDecimal precoUnitario) {
        if (precoUnitario == null) {
            throw new IllegalArgumentException("O preço unitário não pode ser nulo.");
        }
        BigDecimal valorValidado = precoUnitario.setScale(2, RoundingMode.HALF_UP);
        if (valorValidado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O preço unitário deve ser maior que zero.");
        }
        return valorValidado;
    }

    private static LocalDateTime validarDataHora(LocalDateTime dataHora) {
        if (dataHora == null) {
            throw new IllegalArgumentException("A data/hora da transação não pode ser nula.");
        }
        return dataHora;
    }

    private static int validarQuantidadeDisponivel(int quantidadeDisponivel, int quantidade) {
        if (quantidadeDisponivel < 0 || quantidadeDisponivel > quantidade) {
            throw new IllegalArgumentException("A quantidade disponível deve estar entre 0 e a quantidade total da transação.");
        }
        return quantidadeDisponivel;
    }

    public enum TipoTransacao {
        COMPRA,
        VENDA
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ticker, tipo, quantidade, precoUnitario, dataHora, quantidadeDisponivel);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Transacao)) {
            return false;
        }
        Transacao outra = (Transacao) obj;
        return quantidade == outra.quantidade
                && quantidadeDisponivel == outra.quantidadeDisponivel
                && Objects.equals(id, outra.id)
                && Objects.equals(ticker, outra.ticker)
                && tipo == outra.tipo
                && Objects.equals(precoUnitario, outra.precoUnitario)
                && Objects.equals(dataHora, outra.dataHora);
    }

    @Override
    public String toString() {
        return "Transacao{" +
                "id=" + id +
                ", ticker='" + ticker + '\'' +
                ", tipo=" + tipo +
                ", quantidade=" + quantidade +
                ", precoUnitario=" + precoUnitario +
                ", dataHora=" + dataHora +
                ", quantidadeDisponivel=" + quantidadeDisponivel +
                '}';
    }
}
