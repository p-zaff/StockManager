package simulador;

public interface SujeitoMercado {
    void registrarObservador(ObservadorMercado observador);

    void removerObservador(ObservadorMercado observador);
}
