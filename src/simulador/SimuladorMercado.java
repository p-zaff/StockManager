package simulador;

import dao.RegistroPrecoDAO;
import model.Acao;
import model.RegistroPreco;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SimuladorMercado implements SujeitoMercado, AutoCloseable {
    private static final double SEGUNDOS_POR_ANO = 31_536_000.0d;

    private final Map<String, Acao> acoesPorTicker = new ConcurrentHashMap<>();
    private final Map<String, Double> precosAtuaisPorTicker = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<RegistroPreco>> historicoPorTicker = new ConcurrentHashMap<>();
    private final List<ObservadorMercado> observadores = new CopyOnWriteArrayList<>();

    private final long intervaloAtualizacaoSegundos;
    private final long segundosMercadoPorTick;
    private final RegistroPrecoDAO registroPrecoDAO;
    private final CalculadoraPrecoGBM calculadoraPrecoGBM;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean executando = new AtomicBoolean(false);

    private volatile ScheduledFuture<?> tarefaAgendada;

    public SimuladorMercado(List<Acao> catalogoInicial,
                            long intervaloAtualizacaoSegundos,
                            double tendenciaAnual,
                            double volatilidadeAnual,
                            long segundosMercadoPorTick,
                            RegistroPrecoDAO registroPrecoDAO) {
        if (catalogoInicial == null || catalogoInicial.isEmpty()) {
            throw new IllegalArgumentException("O catálogo inicial de ações não pode ser vazio.");
        }
        if (intervaloAtualizacaoSegundos <= 0L) {
            throw new IllegalArgumentException("O intervalo de atualização deve ser maior que zero.");
        }
        if (segundosMercadoPorTick <= 0L) {
            throw new IllegalArgumentException("O tempo de mercado por tick deve ser maior que zero.");
        }
        if (registroPrecoDAO == null) {
            throw new IllegalArgumentException("O RegistroPrecoDAO não pode ser nulo.");
        }

        this.intervaloAtualizacaoSegundos = intervaloAtualizacaoSegundos;
        this.segundosMercadoPorTick = segundosMercadoPorTick;
        this.registroPrecoDAO = registroPrecoDAO;
        this.calculadoraPrecoGBM = new CalculadoraPrecoGBM(tendenciaAnual, volatilidadeAnual, null);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(criarThreadFactory());

        for (Acao acao : catalogoInicial) {
            registrarAcaoInicial(acao);
        }

        carregarHistoricoPersistido();
    }

    public synchronized void iniciar() {
        if (executando.get()) {
            return;
        }

        Runnable cicloMercado = this::executarTickComProtecao;
        tarefaAgendada = scheduler.scheduleAtFixedRate(
                cicloMercado,
                intervaloAtualizacaoSegundos,
                intervaloAtualizacaoSegundos,
                TimeUnit.SECONDS);

        executando.set(true);
    }

    public synchronized void parar() {
        executando.set(false);

        if (tarefaAgendada != null) {
            tarefaAgendada.cancel(false);
            tarefaAgendada = null;
        }
    }

    public boolean estaExecutando() {
        return executando.get();
    }

    public Map<String, Double> obterPrecosAtuaisSnapshot() {
        return Collections.unmodifiableMap(new HashMap<>(precosAtuaisPorTicker));
    }

    public List<RegistroPreco> obterHistoricoPorTicker(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String tickerNormalizado = ticker.trim().toUpperCase();
        List<RegistroPreco> historico = historicoPorTicker.get(tickerNormalizado);

        if (historico == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(new ArrayList<>(historico));
    }

    @Override
    public void registrarObservador(ObservadorMercado observador) {
        if (observador == null) {
            throw new IllegalArgumentException("O observador não pode ser nulo.");
        }
        observadores.add(observador);
    }

    @Override
    public void removerObservador(ObservadorMercado observador) {
        if (observador == null) {
            return;
        }
        observadores.remove(observador);
    }

    @Override
    public synchronized void close() {
        parar();
        scheduler.shutdown();
    }

    private void registrarAcaoInicial(Acao acao) {
        if (acao == null) {
            throw new IllegalArgumentException("A ação do catálogo inicial não pode ser nula.");
        }

        String ticker = acao.getTicker();
        if (ticker == null || ticker.trim().isEmpty()) {
            throw new IllegalArgumentException("A ação deve possuir ticker válido.");
        }

        String tickerNormalizado = ticker.trim().toUpperCase();
        acoesPorTicker.put(tickerNormalizado, acao);

        double precoInicial = acao.getPrecoBaseInicial().doubleValue();
        precosAtuaisPorTicker.put(tickerNormalizado, precoInicial);
        historicoPorTicker.putIfAbsent(tickerNormalizado, new CopyOnWriteArrayList<>());
    }

    private void executarTickComProtecao() {
        try {
            executarTick();
        } catch (RuntimeException e) {
        }
    }

    private void executarTick() {
        LocalDateTime agora = LocalDateTime.now();
        double deltaTempoEmAnos = segundosMercadoPorTick / SEGUNDOS_POR_ANO;

        List<RegistroPreco> registrosDoTick = new ArrayList<>();

        for (Map.Entry<String, Acao> entrada : acoesPorTicker.entrySet()) {
            String ticker = entrada.getKey();
            atualizarPrecoDaAcao(ticker, agora, deltaTempoEmAnos, registrosDoTick);
        }

        persistirRegistrosTick(registrosDoTick);

        TickMercado tickMercado = new TickMercado(agora, precosAtuaisPorTicker, registrosDoTick);
        notificarObservadores(tickMercado);
    }

    private void atualizarPrecoDaAcao(String ticker,
                                      LocalDateTime dataHora,
                                      double deltaTempoEmAnos,
                                      List<RegistroPreco> registrosDoTick) {
        Objects.requireNonNull(ticker, "Ticker não pode ser nulo.");
        Objects.requireNonNull(dataHora, "Data/hora não pode ser nula.");
        Objects.requireNonNull(registrosDoTick, "A lista de registros do tick não pode ser nula.");

        Double precoAtual = precosAtuaisPorTicker.get(ticker);
        if (precoAtual == null) {
            return;
        }

        double novoPreco = calculadoraPrecoGBM.proximoPreco(precoAtual, deltaTempoEmAnos);
        precosAtuaisPorTicker.put(ticker, novoPreco);

        RegistroPreco registro = new RegistroPreco(ticker, dataHora, novoPreco);
        historicoPorTicker.computeIfAbsent(ticker, chave -> new CopyOnWriteArrayList<>()).add(registro);
        registrosDoTick.add(registro);
    }

    private void notificarObservadores(TickMercado tickMercado) {
        for (ObservadorMercado observador : observadores) {
            try {
                observador.aoReceberNovoTick(tickMercado);
            } catch (RuntimeException e) {
            }
        }
    }

    private ThreadFactory criarThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "simulador-mercado-thread");
            thread.setDaemon(true);
            return thread;
        };
    }

    private void carregarHistoricoPersistido() {
        List<RegistroPreco> historicoPersistido = registroPrecoDAO.listarTodos();

        for (RegistroPreco registroPreco : historicoPersistido) {
            if (registroPreco == null || registroPreco.getTicker() == null) {
                continue;
            }

            String ticker = registroPreco.getTicker().trim().toUpperCase();
            if (!acoesPorTicker.containsKey(ticker)) {
                continue;
            }

            historicoPorTicker.computeIfAbsent(ticker, chave -> new CopyOnWriteArrayList<>()).add(registroPreco);
        }

        for (Map.Entry<String, CopyOnWriteArrayList<RegistroPreco>> entrada : historicoPorTicker.entrySet()) {
            List<RegistroPreco> historicoTicker = new ArrayList<>(entrada.getValue());
            if (historicoTicker.isEmpty()) {
                continue;
            }

            historicoTicker.sort((a, b) -> a.getDataHora().compareTo(b.getDataHora()));
            RegistroPreco ultimoRegistro = historicoTicker.get(historicoTicker.size() - 1);
            precosAtuaisPorTicker.put(entrada.getKey(), ultimoRegistro.getValor());

            entrada.getValue().clear();
            entrada.getValue().addAll(historicoTicker);
        }
    }

    private synchronized void persistirRegistrosTick(List<RegistroPreco> registrosDoTick) {
        if (registrosDoTick == null || registrosDoTick.isEmpty()) {
            return;
        }

        List<RegistroPreco> historicoCompleto = new ArrayList<>(registroPrecoDAO.listarTodos());
        historicoCompleto.addAll(registrosDoTick);
        registroPrecoDAO.salvarTodos(historicoCompleto);
    }
}
