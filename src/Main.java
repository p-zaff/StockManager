import dao.AcaoDAO;
import dao.RegistroPrecoDAO;
import dao.UsuarioDAO;
import exception.AutenticacaoFalhouException;
import exception.AtivoNaoEncontradoException;
import exception.SaldoInsuficienteException;
import model.Acao;
import model.RegistroPreco;
import model.Transacao;
import model.Usuario;
import service.AutenticacaoService;
import service.NegociacaoService;
import service.PosicaoCarteira;
import simulador.SimuladorMercado;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

public final class Main {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final long TICK = 3L;
    private static final long TEMPO = 86_400L;

    private Main() {
    }

    public static void main(String[] args) {
        UsuarioDAO uDao = UsuarioDAO.getInstance();
        AcaoDAO aDao = AcaoDAO.getInstance();
        RegistroPrecoDAO pDao = RegistroPrecoDAO.getInstance();

        List<Acao> acoes = carregarOuInicializarCatalogo(aDao);
        SimuladorMercado sim = new SimuladorMercado(
            acoes,
            TICK,
                0.10d,
                0.25d,
            TEMPO,
            pDao);

        AutenticacaoService auth = new AutenticacaoService(uDao);
        NegociacaoService neg = new NegociacaoService(uDao, aDao, sim);

        sim.iniciar();

        try (Scanner scanner = new Scanner(System.in)) {
            exibirCabecalho();

            while (true) {
                Usuario u = telaLoginOuRegistro(scanner, auth);
                if (u == null) {
                    System.out.println("Saindo...");
                    break;
                }

                executarAreaLogada(scanner, u, neg, sim, aDao);
            }
        } finally {
            sim.close();
        }
    }

    private static Usuario telaLoginOuRegistro(Scanner scanner, AutenticacaoService autenticacaoService) {
        while (true) {
            imprimirSeparador();
            System.out.println("ACESSO");
            System.out.println("1 - Login");
            System.out.println("2 - Registo");
            System.out.println("0 - Sair");

            int opcao = lerInteiroSeguro(scanner, "Escolha uma opcao: ");

            try {
                if (opcao == 1) {
                    return executarLogin(scanner, autenticacaoService);
                }

                if (opcao == 2) {
                    return executarRegistro(scanner, autenticacaoService);
                }

                if (opcao == 0) {
                    return null;
                }

                System.out.println("Opcao invalida. Tente novamente.");
            } catch (AutenticacaoFalhouException | IllegalArgumentException e) {
                System.out.println("Aviso: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Erro inesperado no acesso: " + e.getMessage());
            }
        }
    }

    private static Usuario executarLogin(Scanner scanner, AutenticacaoService autenticacaoService) {
        String email = lerTextoNaoVazio(scanner, "E-mail: ");
        String senha = lerTextoNaoVazio(scanner, "Senha: ");
        Usuario usuario = autenticacaoService.autenticar(email, senha);
        System.out.println("Login ok.");
        return usuario;
    }

    private static Usuario executarRegistro(Scanner scanner, AutenticacaoService autenticacaoService) {
        String nome = lerTextoNaoVazio(scanner, "Nome: ");
        String documento = lerTextoNaoVazio(scanner, "Documento (11 digitos): ");
        String email = lerTextoNaoVazio(scanner, "E-mail: ");
        String senha = lerTextoNaoVazio(scanner, "Senha: ");
        BigDecimal saldoInicial = lerBigDecimalSeguro(scanner, "Saldo inicial (ex: 1500.00): ");

        Usuario usuario = autenticacaoService.registrar(nome, documento, email, senha, saldoInicial);
        System.out.println("Registo ok: " + usuario.getNome());
        return usuario;
    }

    private static void executarAreaLogada(Scanner scanner,
                                           Usuario usuarioLogado,
                                           NegociacaoService negociacaoService,
                                           SimuladorMercado simuladorMercado,
                                           AcaoDAO acaoDAO) {
        Usuario sessao = usuarioLogado;

        while (true) {
            imprimirSeparador();
            System.out.println("AREA LOGADA");
            System.out.println("Utilizador: " + sessao.getNome() + " | Saldo: R$ " + formatarDinheiro(sessao.getSaldoFinanceiro()));
            System.out.println("1 - Ver Mercado");
            System.out.println("2 - Comprar");
            System.out.println("3 - Vender");
            System.out.println("4 - Ver Carteira");
            System.out.println("5 - Ver Extrato");
            System.out.println("6 - Logout");

            int opcao = lerInteiroSeguro(scanner, "Escolha uma opcao: ");

            try {
                if (opcao == 1) {
                    exibirMercado(acaoDAO, simuladorMercado);
                    continue;
                }

                if (opcao == 2) {
                    String ticker = lerTextoNaoVazio(scanner, "Ticker para compra: ");
                    int quantidade = lerInteiroSeguro(scanner, "Quantidade para comprar: ");
                    sessao = negociacaoService.comprar(sessao, ticker, quantidade);
                    System.out.println("Compra ok.");
                    continue;
                }

                if (opcao == 3) {
                    String ticker = lerTextoNaoVazio(scanner, "Ticker para venda: ");
                    int quantidade = lerInteiroSeguro(scanner, "Quantidade para vender: ");
                    sessao = negociacaoService.vender(sessao, ticker, quantidade);
                    System.out.println("Venda ok.");
                    continue;
                }

                if (opcao == 4) {
                    exibirCarteira(negociacaoService, sessao);
                    continue;
                }

                if (opcao == 5) {
                    exibirExtrato(negociacaoService, sessao);
                    continue;
                }

                if (opcao == 6) {
                    System.out.println("Logout.");
                    return;
                }

                System.out.println("Opcao invalida. Tente novamente.");
            } catch (SaldoInsuficienteException | AtivoNaoEncontradoException | IllegalArgumentException e) {
                System.out.println("Aviso: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Erro inesperado na operacao: " + e.getMessage());
            }
        }
    }

    private static void exibirMercado(AcaoDAO acaoDAO, SimuladorMercado simuladorMercado) {
        List<Acao> acoes = acaoDAO.listarTodos();
        acoes.sort(Comparator.comparing(Acao::getTicker));
        Map<String, Double> precosAtuais = simuladorMercado.obterPrecosAtuaisSnapshot();

        imprimirSeparador();
        System.out.println("MERCADO EM TEMPO REAL");
        System.out.println("+--------+----------------------+-------------------+------------+-----------+");
        System.out.printf("| %-6s | %-20s | %-17s | %-10s | %-9s |%n", "Ticker", "Empresa", "Setor", "Preco", "Var %");
        System.out.println("+--------+----------------------+-------------------+------------+-----------+");

        for (Acao acao : acoes) {
            String ticker = acao.getTicker();
            BigDecimal preco = BigDecimal.valueOf(precosAtuais.getOrDefault(ticker, acao.getPrecoBaseInicial().doubleValue()))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal variacao = calcularVariacaoPercentual(simuladorMercado, ticker);
            System.out.printf("| %-6s | %-20.20s | %-17.17s | %10s | %9s |%n",
                    ticker,
                    acao.getNomeEmpresa(),
                    acao.getSetor(),
                    formatarDinheiro(preco),
                    formatarPercentual(variacao));
        }

        System.out.println("+--------+----------------------+-------------------+------------+-----------+");
    }

    private static void exibirCarteira(NegociacaoService negociacaoService, Usuario usuarioLogado) {
        List<PosicaoCarteira> posicoes = negociacaoService.consolidarCarteira(usuarioLogado);

        imprimirSeparador();
        System.out.println("CARTEIRA CONSOLIDADA");
        System.out.println("+--------+------------+------------+------------+--------------+--------------+----------+");
        System.out.printf("| %-6s | %-10s | %-10s | %-10s | %-12s | %-12s | %-8s |%n",
                "Ticker", "Qtd", "P. Medio", "P. Atual", "Valor Mercado", "Lucro/Prej", "Ret %");
        System.out.println("+--------+------------+------------+------------+--------------+--------------+----------+");

        if (posicoes.isEmpty()) {
            System.out.printf("| %-92s |%n", "Sem posicoes abertas no momento.");
        } else {
            for (PosicaoCarteira posicao : posicoes) {
                System.out.printf("| %-6s | %10d | %10s | %10s | %12s | %12s | %8s |%n",
                        posicao.getTicker(),
                        posicao.getQuantidade(),
                        formatarDinheiro(posicao.getPrecoMedio()),
                        formatarDinheiro(posicao.getPrecoAtual()),
                        formatarDinheiro(posicao.getValorMercado()),
                        formatarDinheiro(posicao.getLucroPrejuizo()),
                        formatarPercentual(posicao.getRetornoPercentual()));
            }
        }

        System.out.println("+--------+------------+------------+------------+--------------+--------------+----------+");
    }

    private static void exibirExtrato(NegociacaoService negociacaoService, Usuario usuarioLogado) {
        List<Transacao> extrato = negociacaoService.obterExtratoOrdenado(usuarioLogado);

        imprimirSeparador();
        System.out.println("EXTRATO DE TRANSACOES");
        System.out.println("+-----+-------------------+--------+--------+------------+------------+------------+");
        System.out.printf("| %-3s | %-17s | %-6s | %-6s | %-10s | %-10s | %-10s |%n",
                "ID", "Data/Hora", "Ticker", "Tipo", "Quantidade", "P. Unit", "Disp.");
        System.out.println("+-----+-------------------+--------+--------+------------+------------+------------+");

        if (extrato.isEmpty()) {
            System.out.printf("| %-77s |%n", "Sem transacoes no extrato.");
        } else {
            for (Transacao transacao : extrato) {
                System.out.printf("| %3d | %-17s | %-6s | %-6s | %10d | %10s | %10d |%n",
                        transacao.getId(),
                        transacao.getDataHora().format(FMT),
                        transacao.getTicker(),
                        transacao.getTipo(),
                        transacao.getQuantidade(),
                        formatarDinheiro(transacao.getPrecoUnitario()),
                        transacao.getQuantidadeDisponivel());
            }
        }

        System.out.println("+-----+-------------------+--------+--------+------------+------------+------------+");
    }

    private static List<Acao> carregarOuInicializarCatalogo(AcaoDAO acaoDAO) {
        List<Acao> acoes = acaoDAO.listarTodos();
        if (!acoes.isEmpty()) {
            return acoes;
        }

        acoes = new ArrayList<>();
        acoes.add(new Acao("Petrobras", "PETR4", "Energia", new BigDecimal("34.80")));
        acoes.add(new Acao("Vale", "VALE3", "Mineracao", new BigDecimal("61.25")));
        acoes.add(new Acao("Itau Unibanco", "ITUB4", "Financeiro", new BigDecimal("29.10")));
        acoes.add(new Acao("Bradesco", "BBDC4", "Financeiro", new BigDecimal("14.65")));
        acoes.add(new Acao("Ambev", "ABEV3", "Consumo", new BigDecimal("12.40")));

        acaoDAO.salvarTodos(acoes);
        return acoes;
    }

    private static int lerInteiroSeguro(Scanner scanner, String mensagem) {
        while (true) {
            System.out.print(mensagem);
            try {
                int valor = scanner.nextInt();
                scanner.nextLine();
                return valor;
            } catch (InputMismatchException e) {
                System.out.println("Entrada invalida. Digite um numero inteiro.");
                scanner.nextLine();
            } catch (NoSuchElementException e) {
                throw new IllegalStateException("Entrada interrompida ou finalizada.", e);
            }
        }
    }

    private static BigDecimal lerBigDecimalSeguro(Scanner scanner, String mensagem) {
        while (true) {
            System.out.print(mensagem);
            try {
                BigDecimal valor = scanner.nextBigDecimal();
                scanner.nextLine();
                if (valor.compareTo(BigDecimal.ZERO) < 0) {
                    System.out.println("Informe um valor maior ou igual a zero.");
                    continue;
                }
                return valor.setScale(2, RoundingMode.HALF_UP);
            } catch (InputMismatchException e) {
                System.out.println("Entrada invalida. Digite um valor numerico (ex: 1500.00).");
                scanner.nextLine();
            } catch (NoSuchElementException e) {
                throw new IllegalStateException("Entrada interrompida ou finalizada.", e);
            }
        }
    }

    private static String lerTextoNaoVazio(Scanner scanner, String mensagem) {
        while (true) {
            System.out.print(mensagem);
            try {
                String valor = scanner.nextLine();
                if (valor != null && !valor.trim().isEmpty()) {
                    return valor.trim();
                }
                System.out.println("Campo obrigatorio. Tente novamente.");
            } catch (NoSuchElementException e) {
                throw new IllegalStateException("Entrada interrompida ou finalizada.", e);
            }
        }
    }

    private static String formatarDinheiro(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String formatarPercentual(BigDecimal valorPercentual) {
        return valorPercentual.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private static BigDecimal calcularVariacaoPercentual(SimuladorMercado simuladorMercado, String ticker) {
        List<RegistroPreco> historico = simuladorMercado.obterHistoricoPorTicker(ticker);
        if (historico.size() < 2) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        double precoAnterior = historico.get(historico.size() - 2).getValor();
        double precoAtual = historico.get(historico.size() - 1).getValor();

        if (precoAnterior <= 0.0d) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf((precoAtual - precoAnterior) / precoAnterior * 100)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static void exibirCabecalho() {
        imprimirSeparador();
        System.out.println("BOLSA APP");
        System.out.println("simples em Java");
        imprimirSeparador();
    }

    private static void imprimirSeparador() {
        System.out.println("===============================================================");
    }
}