package simulador;

import java.util.Random;

public final class CalculadoraPrecoGBM {
    private static final double PRECO_MINIMO = 0.01d;

    private final double tendenciaAnual;
    private final double volatilidadeAnual;
    private final Random random;

    public CalculadoraPrecoGBM(double tendenciaAnual, double volatilidadeAnual, Random random) {
        if (volatilidadeAnual < 0.0d) {
            throw new IllegalArgumentException("A volatilidade anual não pode ser negativa.");
        }

        this.tendenciaAnual = tendenciaAnual;
        this.volatilidadeAnual = volatilidadeAnual;
        this.random = random == null ? new Random() : random;
    }

    public double proximoPreco(double precoAtual, double deltaTempoEmAnos) {
        if (precoAtual <= 0.0d) {
            throw new IllegalArgumentException("O preço atual deve ser maior que zero.");
        }
        if (deltaTempoEmAnos <= 0.0d) {
            throw new IllegalArgumentException("O delta de tempo deve ser maior que zero.");
        }

        double z = random.nextGaussian();
        double parteDeterministica = (tendenciaAnual - 0.5d * volatilidadeAnual * volatilidadeAnual) * deltaTempoEmAnos;
        double parteEstocastica = volatilidadeAnual * Math.sqrt(deltaTempoEmAnos) * z;
        double fatorMultiplicativo = Math.pow(Math.E, parteDeterministica + parteEstocastica);

        double proximoPreco = precoAtual * fatorMultiplicativo;

        if (Double.isNaN(proximoPreco) || Double.isInfinite(proximoPreco)) {
            return PRECO_MINIMO;
        }

        return Math.max(proximoPreco, PRECO_MINIMO);
    }
}
