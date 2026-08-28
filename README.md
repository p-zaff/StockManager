# StockManager - Simulador de Bolsa em Java

> **Projeto de Programação Orientada a Objetos** - Sistema de simulação de mercado de ações com persistência em arquivo binário e motor de precificação em tempo real.

---

## 📋 Conteúdo

- [Descrição da Aplicação](#descrição-da-aplicação)
- [Arquitetura](#arquitetura)
- [Padrões de Projeto](#padrões-de-projeto)
- [Estrutura de Diretórios](#estrutura-de-diretórios)
- [Como Executar](#como-executar)
- [Funcionalidades](#funcionalidades)
- [Análise Técnica](#análise-técnica)
- [Limitações Conhecidas](#limitações-conhecidas)

---

## 📱 Descrição da Aplicação

### Contexto
O **StockManager** é uma aplicação terminal que simula um ambiente de negociação de ações em bolsa. Funciona como um broker simplificado onde usuários podem se autenticar, visualizar o mercado em tempo real, comprar e vender ações, além de consultar sua carteira de investimentos.

### Funcionalidades Principais

#### 1. **Autenticação e Gerenciamento de Usuários**
- Registro de novos usuários com documento, e-mail e senha
- Login com validação de credenciais
- Saldo financeiro inicial configurável
- Persistência de dados em arquivo binário

#### 2. **Mercado em Tempo Real**
- Visualização de 5 ações negociáveis (Petrobras, Vale, Itaú, Bradesco, Ambev)
- Precificação dinâmica baseada em **Movimento Browniano Geométrico (GBM)**
- Atualização de preços a cada 3 segundos
- Cálculo de variação percentual em relação ao tick anterior

#### 3. **Operações de Negociação**
- **Compra**: Débito de saldo financeiro, criação de posição
- **Venda**: Execução FIFO (First In, First Out) de lotes, crédito de saldo
- Validação de saldo e quantidade disponível
- Histórico completo de transações

#### 4. **Carteira e Relatórios**
- Consolidação de posições abertas por ticker
- Cálculo de preço médio ponderado
- Demonstração de lucro/prejuízo por posição
- Retorno percentual
- Extrato completo de transações ordenado por data/hora

#### 5. **Persistência de Dados**
- Três arquivos binários (serialização Java):
  - `usuarios.bin`: usuários e histórico de transações
  - `acoes.bin`: catálogo de ações disponíveis
  - `precos.bin`: histórico de preços para análise

---

## 🏗️ Arquitetura

A aplicação segue uma arquitetura **em camadas** com clara separação entre apresentação, lógica de negócio e persistência.

### Camadas da Arquitetura

```
┌─────────────────────────────────────────────────┐
│           CAMADA DE APRESENTAÇÃO (View)         │
│  Main.java, App.java - Interface Terminal (CLI) │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│       CAMADA DE LÓGICA DE NEGÓCIO (Model)       │
│  • AutenticacaoService                          │
│  • NegociacaoService (Compra, Venda, Carteira) │
│  • SimuladorMercado (Motor de Preços)          │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│   CAMADA DE ENTIDADES (Domain Model)            │
│  • Usuario, Acao, Transacao, RegistroPreco     │
│  • PosicaoCarteira (DTO de visualização)       │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│      CAMADA DE PERSISTÊNCIA (Controller/DAO)    │
│  • AbstractBinaryDAO<T> (Template Pattern)     │
│  • UsuarioDAO, AcaoDAO, RegistroPrecoDAO       │
│  • Acesso via Singleton Pattern                 │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│         CAMADA DE ARMAZENAMENTO                 │
│  • Arquivos binários em data/binarios/         │
│  • Serialização ObjectOutputStream/            │
│    ObjectInputStream                            │
└─────────────────────────────────────────────────┘
```

### Model-View-Controller (MVC)

| Componente | Localização | Responsabilidade |
|-----------|-------------|-----------------|
| **View** | `Main.java`, `App.java` | Renderização de telas (CLI), leitura de entrada do usuário, tratamento de exceções de UI |
| **Model** | `src/model/`, `src/service/` | Lógica de domínio: autenticação, negociação, simulação de mercado, consolidação de carteira |
| **Controller** | `src/dao/` | Intermediação entre Model e persistência: operações CRUD em arquivos binários |

---

## 🎯 Padrões de Projeto

### 1. **Singleton Pattern** (DAOs)
```java
// src/dao/UsuarioDAO.java
private static UsuarioDAO instancia;

public static synchronized UsuarioDAO getInstance() {
    if (instancia == null) {
        instancia = new UsuarioDAO(CAMINHO);
    }
    return instancia;
}
```
✅ **Uso**: Garantir uma única instância de acesso aos dados por tipo de entidade.

### 2. **Template Method Pattern** (AbstractBinaryDAO)
```java
// src/dao/AbstractBinaryDAO.java
public synchronized List<T> listarTodos() {
    // Template: Define algoritmo de deserialização
}

public synchronized void salvarTodos(List<T> registros) {
    // Template: Define algoritmo de serialização
}
```
✅ **Uso**: Reutilizar lógica de I/O binário para todas as DAOs específicas.

### 3. **Observer Pattern** (SimuladorMercado)
```java
// src/simulador/SujeitoMercado (interface)
void registrarObservador(ObservadorMercado observador);
void removerObservador(ObservadorMercado observador);

// src/simulador/SimuladorMercado
List<ObservadorMercado> observadores = new CopyOnWriteArrayList<>();
```
✅ **Uso**: Permitir que observadores reajam a mudanças de preço em tempo real.

### 4. **Strategy Pattern** (CalculadoraPrecoGBM)
```java
// src/simulador/CalculadoraPrecoGBM
public double calcularProximoPreco(double precoAtual, long tempoDecorrido)
```
✅ **Uso**: Isolar algoritmo de cálculo de preço (Movimento Browniano Geométrico).

### 5. **DTO Pattern** (PosicaoCarteira)
```java
// src/service/PosicaoCarteira
public class PosicaoCarteira {
    private String ticker;
    private int quantidade;
    private BigDecimal precoMedio;
    private BigDecimal precoAtual;
    // ...
}
```
✅ **Uso**: Transportar dados consolidados da carteira sem expor a lógica de transação.

---

## 📁 Estrutura de Diretórios

```
StockManager/
├── README.md                          # Este arquivo
├── src/
│   ├── Main.java                      # Ponto de entrada (View)
│   ├── App.java                       # Wrapper para execução
│   ├── dao/
│   │   ├── DAO.java                   # Interface genérica (padrão Template)
│   │   ├── AbstractBinaryDAO.java     # Template Method para serialização
│   │   ├── UsuarioDAO.java            # DAO específica: CRUD de usuários
│   │   ├── AcaoDAO.java               # DAO específica: catálogo de ações
│   │   └── RegistroPrecoDAO.java      # DAO específica: histórico de preços
│   ├── model/
│   │   ├── Usuario.java               # Entidade: usuário com saldo e transações
│   │   ├── Acao.java                  # Entidade: ação negociável
│   │   ├── Transacao.java             # Entidade: registro de compra/venda
│   │   └── RegistroPreco.java         # Entidade: snapshot de preço em tempo
│   ├── service/
│   │   ├── AutenticacaoService.java   # Lógica: login e registro
│   │   ├── NegociacaoService.java     # Lógica: compra, venda, carteira
│   │   └── PosicaoCarteira.java       # DTO: consolidação de posição
│   ├── simulador/
│   │   ├── SujeitoMercado.java        # Interface: Subject do padrão Observer
│   │   ├── ObservadorMercado.java     # Interface: Observer do padrão Observer
│   │   ├── SimuladorMercado.java      # Implementação: motor de mercado
│   │   ├── CalculadoraPrecoGBM.java   # Estratégia: cálculo de preço (GBM)
│   │   ├── TickMercado.java           # Event: tick do mercado
│   │   └── ...
│   └── exception/
│       ├── AutenticacaoFalhouException.java
│       ├── AtivoNaoEncontradoException.java
│       └── SaldoInsuficienteException.java
├── bin/                               # Arquivos compilados (.class)
├── data/
│   └── binarios/
│       ├── usuarios/
│       │   └── usuarios.bin           # Persistência: usuários
│       ├── acoes/
│       │   └── acoes.bin              # Persistência: catálogo
│       └── precos/
│           └── precos.bin             # Persistência: histórico
└── lib/                               # Bibliotecas externas (nenhuma)
```

---

## 🔑 Conceitos de POO Utilizados

### Herança
```java
// AbstractBinaryDAO<T> é herdada por classes específicas
public class UsuarioDAO extends AbstractBinaryDAO<Usuario> {
    public UsuarioDAO(Path caminhoArquivo) {
        super(caminhoArquivo, Usuario.class);
    }
}
```

### Interfaces
```java
// DAO.java define contrato
public interface DAO<T> {
    List<T> listarTodos();
    void salvarTodos(List<T> registros);
    Optional<T> buscarPorDocumento(String documento);
}

// SujeitoMercado.java define Observer Pattern
public interface SujeitoMercado {
    void registrarObservador(ObservadorMercado observador);
    void removerObservador(ObservadorMercado observador);
}
```

### Uso de `Set`
```java
// Transacao.TipoTransacao usa enum (similar a Set de constantes)
public enum TipoTransacao {
    COMPRA, VENDA
}

// Observadores únicos em SimuladorMercado
private final List<ObservadorMercado> observadores = new CopyOnWriteArrayList<>();
```

### Uso de `Map`
```java
// SimuladorMercado agrupa dados por ticker
private final Map<String, Acao> acoesPorTicker = new ConcurrentHashMap<>();
private final Map<String, Double> precosAtuaisPorTicker = new ConcurrentHashMap<>();
private final Map<String, CopyOnWriteArrayList<RegistroPreco>> historicoPorTicker 
    = new ConcurrentHashMap<>();

// NegociacaoService consolida posições
Map<String, Integer> quantidadePorTicker = new HashMap<>();
Map<String, BigDecimal> custoAbertoPorTicker = new HashMap<>();
```

### Uso de Arquivos
- **Serialização**: `ObjectOutputStream`, `ObjectInputStream`
- **Arquivo Binário**: `.bin` em `data/binarios/`
- **Criação de Diretórios**: `Files.createDirectories()`
- **Tratamento de EOF**: `EOFException`, `NoSuchFileException`

---

## 🚀 Como Executar

### Pré-requisitos
- Java 11+
- Terminal/PowerShell/CMD
- Compilador javac

### Compilação

**Windows (PowerShell):**
```powershell
cd "c:\Users\pedro.boff\OneDrive - Sonepar\Área de Trabalho\Escola\POO\StockManager\StockManager"
$files = Get-ChildItem -Path src -Recurse -Filter "*.java" -File | ForEach-Object { $_.FullName }
javac -d bin @files
```

**Linux/Mac:**
```bash
cd StockManager
find src -name "*.java" | xargs javac -d bin
```

### Execução

**Windows (PowerShell):**
```powershell
java -cp bin Main
```

**Linux/Mac:**
```bash
java -cp bin Main
```

### Fluxo de Uso

1. **Tela 1 - Acesso**
   - Opção 1: Login com e-mail e senha
   - Opção 2: Registro de novo usuário
   - Opção 0: Sair

2. **Tela 2 - Área Logada**
   - Opção 1: Ver Mercado (cotações em tempo real)
   - Opção 2: Comprar (entra ticker e quantidade)
   - Opção 3: Vender (executa FIFO automático)
   - Opção 4: Ver Carteira (posições consolidadas)
   - Opção 5: Ver Extrato (histórico de transações)
   - Opção 6: Logout

---

## 🧮 Análise Técnica

### Motor de Precificação (GBM)

O **Movimento Browniano Geométrico** é um modelo matemático para simular variações de preço:

$$dS = \mu S \, dt + \sigma S \, dW$$

Onde:
- $S$ = preço da ação
- $\mu$ = tendência anual (drift)
- $\sigma$ = volatilidade anual
- $dW$ = incremento Wiener (ruído aleatório)

**Implementação em `CalculadoraPrecoGBM.java`:**
```java
double novoPreco = precoAtual * 
    Math.exp((tendenciaAnual - 0.5 * volatilidadeAnual * volatilidadeAnual) * 
             tempoDecorrido + 
             volatilidadeAnual * Math.sqrt(tempoDecorrido) * 
             distribuicaoNormal.nextGaussian());
```

### Algoritmo FIFO (First In, First Out)

Na venda, as quantidades são consumidas na ordem de compra:

```
Compras Abertas:
1. Lote A: 100 ações @ R$ 10 → 100 disponíveis
2. Lote B: 50 ações @ R$ 12 → 50 disponíveis
3. Lote C: 75 ações @ R$ 11 → 75 disponíveis

Venda de 180 ações:
- Consome 100 do Lote A (primeiro)
- Consome 50 do Lote B (segundo)
- Consome 30 do Lote C (terceiro)
- Resultado: Lote A (0 disp.), Lote B (0 disp.), Lote C (45 disp.)
```

**Benefício**: Simula realismo de execução e cálculo de imposto (FIFO é critério fiscal no Brasil).

### Concorrência e Thread-Safety

```java
// SimuladorMercado usa ScheduledExecutorService
private final ScheduledExecutorService scheduler = 
    Executors.newSingleThreadScheduledExecutor();

// Dados compartilhados são thread-safe
private final Map<String, Double> precosAtuaisPorTicker = new ConcurrentHashMap<>();
private final List<ObservadorMercado> observadores = new CopyOnWriteArrayList<>();

// DAOs sincronizam operações de leitura/escrita
public synchronized List<T> listarTodos() { ... }
public synchronized void salvarTodos(List<T> registros) { ... }

// Services sincronizam operações críticas
public synchronized Usuario comprar(...) { ... }
public synchronized Usuario vender(...) { ... }
private synchronized void persistirUsuario(...) { ... }
```

### Tratamento Robusto de Scanner

```java
private static int lerInteiroSeguro(Scanner scanner, String mensagem) {
    while (true) {
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
```

---

## ⚠️ Limitações Conhecidas

### 1. **Arquitetura de Dados**
- ❌ Sem banco de dados relacional
- ⚠️ Serialização Java é frágil (mudança de classe quebra arquivos antigos)
- ⚠️ Não há controle de concorrência em nível de arquivo (single-process only)

### 2. **Segurança**
- ❌ Senhas armazenadas em texto plano
- ❌ Sem encriptação de arquivos binários
- ❌ Sem auditoria ou logging de operações

### 3. **Funcionalidades de Negócio**
- ❌ Sem suporte a operações intraday (dia útil apenas)
- ❌ Sem comissões ou taxas
- ❌ Sem margem ou operações alavancadas
- ❌ Sem limite de operações por dia

### 4. **Performance**
- ⚠️ Leitura completa do arquivo a cada operação (sem índices)
- ⚠️ Sincronização em nível de método (pode causar contenção em alta carga)
- ⚠️ Histórico de preços cresce indefinidamente (sem arquivo)

### 5. **Interface**
- ❌ Apenas terminal (CLI)
- ❌ Sem confirmação antes de operações críticas
- ⚠️ Formatação fixa de tabelas (sem responsividade)

### 6. **Validações**
- ⚠️ Documento (CPF) não é validado (apenas string 11 caracteres)
- ⚠️ E-mail não é validado
- ⚠️ Sem rate limiting de tentativas de login

---

## 📊 Exemplo de Saída Esperada

```
===============================================================
BOLSA APP
simples em Java
===============================================================
ACESSO
1 - Login
2 - Registo
0 - Sair
Escolha uma opcao: 2
Nome: João da Silva
Documento (11 digitos): 12345678901
E-mail: joao@example.com
Senha: senha123
Saldo inicial (ex: 1500.00): 5000.00
Registo ok: João da Silva
===============================================================
ACESSO
1 - Login
2 - Registo
0 - Sair
Escolha uma opcao: 1
E-mail: joao@example.com
Senha: senha123
Login ok.
===============================================================
AREA LOGADA
Utilizador: João da Silva | Saldo: R$ 5000.00
1 - Ver Mercado
2 - Comprar
3 - Vender
4 - Ver Carteira
5 - Ver Extrato
6 - Logout
Escolha uma opcao: 1
===============================================================
MERCADO EM TEMPO REAL
+--------+----------------------+-------------------+------------+-----------+
| Ticker | Empresa              | Setor             | Preco      | Var %     |
+--------+----------------------+-------------------+------------+-----------+
| ABEV3  | Ambev                | Consumo           |      12.45 |      0.40%|
| BBDC4  | Bradesco             | Financeiro        |      14.68 |     -0.20%|
| ITUB4  | Itau Unibanco        | Financeiro        |      29.15 |      0.17%|
| PETR4  | Petrobras            | Energia           |      34.82 |      0.06%|
| VALE3  | Vale                 | Mineracao         |      61.28 |      0.05%|
+--------+----------------------+-------------------+------------+-----------+
```

---

## 📚 Referências

- **Padrões de Projeto**: Gamma et al., "Design Patterns: Elements of Reusable Object-Oriented Software"
- **Movimento Browniano Geométrico**: Hull, "Options, Futures, and Other Derivatives"
- **Java Concurrency**: Goetz et al., "Java Concurrency in Practice"
- **Clean Code**: Martin, "Clean Code: A Handbook of Agile Software Craftsmanship"