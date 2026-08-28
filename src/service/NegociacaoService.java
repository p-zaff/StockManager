package service;

import dao.AcaoDAO;
import dao.UsuarioDAO;
import exception.AtivoNaoEncontradoException;
import exception.SaldoInsuficienteException;
import model.Transacao;
import model.Usuario;
import simulador.SimuladorMercado;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class NegociacaoService {
    private final UsuarioDAO usuarioDAO;
    private final AcaoDAO acaoDAO;
    private final SimuladorMercado simuladorMercado;
    private final AtomicLong sequenciaTransacao;

    public NegociacaoService(UsuarioDAO usuarioDAO, AcaoDAO acaoDAO, SimuladorMercado simuladorMercado) {
        if (usuarioDAO == null || acaoDAO == null || simuladorMercado == null) {
            throw new IllegalArgumentException("Dependencias da negociacao nao podem ser nulas.");
        }

        this.usuarioDAO = usuarioDAO;
        this.acaoDAO = acaoDAO;
        this.simuladorMercado = simuladorMercado;
        this.sequenciaTransacao = new AtomicLong(obterMaiorIdTransacao() + 1L);
    }

    public synchronized Usuario comprar(Usuario usuarioLogado, String ticker, int quantidade) {
        validarQuantidade(quantidade);
        String tickerNormalizado = normalizarTicker(ticker);

        acaoDAO.buscarPorTicker(tickerNormalizado)
                .orElseThrow(() -> new AtivoNaoEncontradoException("Ticker nao encontrado: " + tickerNormalizado));

        Usuario usuario = carregarUsuarioAtualizado(usuarioLogado);
        BigDecimal precoUnitario = obterPrecoAtual(tickerNormalizado);
        BigDecimal custoTotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade)).setScale(2, RoundingMode.HALF_UP);

        if (usuario.getSaldoFinanceiro().compareTo(custoTotal) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente para a compra.");
        }

        BigDecimal novoSaldo = usuario.getSaldoFinanceiro().subtract(custoTotal).setScale(2, RoundingMode.HALF_UP);
        usuario.setSaldoFinanceiro(novoSaldo);

        Transacao compra = new Transacao(
                proximoIdTransacao(),
                tickerNormalizado,
                Transacao.TipoTransacao.COMPRA,
                quantidade,
                precoUnitario,
                LocalDateTime.now(),
                quantidade);

        usuario.adicionarTransacao(compra);
        persistirUsuario(usuario);
        return usuario;
    }

    public synchronized Usuario vender(Usuario usuarioLogado, String ticker, int quantidade) {
        validarQuantidade(quantidade);
        String tickerNormalizado = normalizarTicker(ticker);

        acaoDAO.buscarPorTicker(tickerNormalizado)
                .orElseThrow(() -> new AtivoNaoEncontradoException("Ticker nao encontrado: " + tickerNormalizado));

        Usuario usuario = carregarUsuarioAtualizado(usuarioLogado);
        List<Transacao> historico = new ArrayList<>(usuario.getHistoricoTransacoes());

        List<Transacao> comprasAtivas = new ArrayList<>();
        for (Transacao transacao : historico) {
            if (transacao.getTipo() == Transacao.TipoTransacao.COMPRA
                    && tickerNormalizado.equalsIgnoreCase(transacao.getTicker())
                    && transacao.getQuantidadeDisponivel() > 0) {
                comprasAtivas.add(transacao);
            }
        }

        comprasAtivas.sort(Comparator.comparing(Transacao::getDataHora));

        int quantidadeDisponivelTotal = 0;
        for (Transacao compra : comprasAtivas) {
            quantidadeDisponivelTotal += compra.getQuantidadeDisponivel();
        }

        if (quantidadeDisponivelTotal < quantidade) {
            throw new SaldoInsuficienteException("Quantidade insuficiente para venda no ticker " + tickerNormalizado + ".");
        }

        int restanteParaVender = quantidade;
        Map<Long, Integer> novaQuantidadeDisponivelPorTransacao = new HashMap<>();

        for (Transacao compra : comprasAtivas) {
            if (restanteParaVender <= 0) {
                break;
            }

            int disponivel = compra.getQuantidadeDisponivel();
            int consumida = Math.min(disponivel, restanteParaVender);
            int novaDisponivel = disponivel - consumida;

            novaQuantidadeDisponivelPorTransacao.put(compra.getId(), novaDisponivel);
            restanteParaVender -= consumida;
        }

        List<Transacao> historicoAtualizado = new ArrayList<>(historico.size() + 1);
        for (Transacao transacao : historico) {
            Integer novaDisponivel = novaQuantidadeDisponivelPorTransacao.get(transacao.getId());
            if (novaDisponivel == null) {
                historicoAtualizado.add(transacao);
                continue;
            }

            Transacao compraAtualizada = new Transacao(
                    transacao.getId(),
                    transacao.getTicker(),
                    transacao.getTipo(),
                    transacao.getQuantidade(),
                    transacao.getPrecoUnitario(),
                    transacao.getDataHora(),
                    novaDisponivel);
            historicoAtualizado.add(compraAtualizada);
        }

        BigDecimal precoUnitarioVenda = obterPrecoAtual(tickerNormalizado);
        BigDecimal valorTotalVenda = precoUnitarioVenda.multiply(BigDecimal.valueOf(quantidade)).setScale(2, RoundingMode.HALF_UP);

        Transacao venda = new Transacao(
                proximoIdTransacao(),
                tickerNormalizado,
                Transacao.TipoTransacao.VENDA,
                quantidade,
                precoUnitarioVenda,
                LocalDateTime.now(),
                0);
        historicoAtualizado.add(venda);

        usuario.setHistoricoTransacoes(historicoAtualizado);
        usuario.setSaldoFinanceiro(usuario.getSaldoFinanceiro().add(valorTotalVenda).setScale(2, RoundingMode.HALF_UP));

        persistirUsuario(usuario);
        return usuario;
    }

    public List<PosicaoCarteira> consolidarCarteira(Usuario usuarioLogado) {
        Usuario usuario = carregarUsuarioAtualizado(usuarioLogado);
        Map<String, Integer> quantidadePorTicker = new HashMap<>();
        Map<String, BigDecimal> custoAbertoPorTicker = new HashMap<>();

        for (Transacao transacao : usuario.getHistoricoTransacoes()) {
            if (transacao.getTipo() != Transacao.TipoTransacao.COMPRA || transacao.getQuantidadeDisponivel() <= 0) {
                continue;
            }

            String ticker = transacao.getTicker().toUpperCase();
            int quantidadeAberta = transacao.getQuantidadeDisponivel();
            BigDecimal custoLote = transacao.getPrecoUnitario().multiply(BigDecimal.valueOf(quantidadeAberta));

            quantidadePorTicker.merge(ticker, quantidadeAberta, Integer::sum);
            custoAbertoPorTicker.merge(ticker, custoLote, BigDecimal::add);
        }

        List<PosicaoCarteira> posicoes = new ArrayList<>(quantidadePorTicker.size());
        for (Map.Entry<String, Integer> entrada : quantidadePorTicker.entrySet()) {
            String ticker = entrada.getKey();
            int quantidade = entrada.getValue();
            BigDecimal custoTotal = custoAbertoPorTicker.getOrDefault(ticker, BigDecimal.ZERO);
            BigDecimal precoMedio = custoTotal.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
            BigDecimal precoAtual = obterPrecoAtual(ticker);

            posicoes.add(new PosicaoCarteira(ticker, quantidade, precoMedio, precoAtual));
        }

        posicoes.sort(Comparator.comparing(PosicaoCarteira::getTicker));
        return posicoes;
    }

    public List<Transacao> obterExtratoOrdenado(Usuario usuarioLogado) {
        Usuario usuario = carregarUsuarioAtualizado(usuarioLogado);
        List<Transacao> extrato = new ArrayList<>(usuario.getHistoricoTransacoes());
        extrato.sort(Comparator.comparing(Transacao::getDataHora));
        return extrato;
    }

    private synchronized void persistirUsuario(Usuario usuarioAtualizado) {
        List<Usuario> usuarios = new ArrayList<>(usuarioDAO.listarTodos());
        String documento = usuarioAtualizado.getDocumento();

        for (int i = 0; i < usuarios.size(); i++) {
            if (Objects.equals(usuarios.get(i).getDocumento(), documento)) {
                usuarios.set(i, usuarioAtualizado);
                usuarioDAO.salvarTodos(usuarios);
                return;
            }
        }

        throw new IllegalStateException("Usuario nao encontrado para persistencia.");
    }

    private Usuario carregarUsuarioAtualizado(Usuario referenciaUsuario) {
        if (referenciaUsuario == null || referenciaUsuario.getDocumento() == null) {
            throw new IllegalArgumentException("Usuario de referencia invalido.");
        }

        return usuarioDAO.buscarPorDocumento(referenciaUsuario.getDocumento())
                .orElseThrow(() -> new IllegalStateException("Usuario nao encontrado na base persistida."));
    }

    private BigDecimal obterPrecoAtual(String ticker) {
        Double preco = simuladorMercado.obterPrecosAtuaisSnapshot().get(ticker.toUpperCase());
        if (preco == null || preco <= 0.0d) {
            throw new AtivoNaoEncontradoException("Preco nao disponivel para ticker: " + ticker);
        }

        return BigDecimal.valueOf(preco).setScale(2, RoundingMode.HALF_UP);
    }

    private void validarQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
    }

    private String normalizarTicker(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticker invalido.");
        }
        return ticker.trim().toUpperCase();
    }

    private long proximoIdTransacao() {
        return sequenciaTransacao.getAndIncrement();
    }

    private synchronized long obterMaiorIdTransacao() {
        long maiorId = 0L;
        List<Usuario> usuarios = usuarioDAO.listarTodos();

        for (Usuario usuario : usuarios) {
            for (Transacao transacao : usuario.getHistoricoTransacoes()) {
                if (transacao.getId() != null && transacao.getId() > maiorId) {
                    maiorId = transacao.getId();
                }
            }
        }

        return maiorId;
    }
}
