package service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PosicaoCarteira {
    private final String ticker;
    private final int quantidade;
    private final BigDecimal precoMedio;
    private final BigDecimal precoAtual;

    public PosicaoCarteira(String ticker, int quantidade, BigDecimal precoMedio, BigDecimal precoAtual) {
        this.ticker = ticker;
        this.quantidade = quantidade;
        this.precoMedio = precoMedio == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : precoMedio.setScale(2, RoundingMode.HALF_UP);
        this.precoAtual = precoAtual == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : precoAtual.setScale(2, RoundingMode.HALF_UP);
    }

    public String getTicker() {
        return ticker;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoMedio() {
        return precoMedio;
    }

    public BigDecimal getPrecoAtual() {
        return precoAtual;
    }

    public BigDecimal getValorMercado() {
        return precoAtual.multiply(BigDecimal.valueOf(quantidade)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getCustoTotal() {
        return precoMedio.multiply(BigDecimal.valueOf(quantidade)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getLucroPrejuizo() {
        return getValorMercado().subtract(getCustoTotal()).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getRetornoPercentual() {
        BigDecimal custoTotal = getCustoTotal();
        if (custoTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return getLucroPrejuizo()
                .multiply(BigDecimal.valueOf(100))
                .divide(custoTotal, 2, RoundingMode.HALF_UP);
    }
}
