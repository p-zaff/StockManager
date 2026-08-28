package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nome;
    private String documento;
    private String email;
    private String senha;
    private BigDecimal saldoFinanceiro;
    private List<Transacao> historicoTransacoes;

    public Usuario() {
        this(null, null, null, null, BigDecimal.ZERO, new ArrayList<>());
    }

    public Usuario(String nome, String documento, String email, String senha, BigDecimal saldoFinanceiro) {
        this(nome, documento, email, senha, saldoFinanceiro, new ArrayList<>());
    }

    public Usuario(String nome, String documento, String email, String senha, BigDecimal saldoFinanceiro,
                  List<Transacao> historicoTransacoes) {
        setNome(nome);
        setDocumento(documento);
        setEmail(email);
        setSenha(senha);
        setSaldoFinanceiro(saldoFinanceiro);
        this.historicoTransacoes = historicoTransacoes == null ? new ArrayList<>() : new ArrayList<>(historicoTransacoes);
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do usuário não pode ser vazio.");
        }
        this.nome = nome.trim();
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        String documentoValidado = documento == null ? "" : documento.trim();
        if (documentoValidado.isEmpty()) {
            throw new IllegalArgumentException("O documento do usuário não pode ser vazio.");
        }

        String documentoNormalizado = documentoValidado.replaceAll("\\D", "");
        if (documentoNormalizado.length() != 11) {
            throw new IllegalArgumentException("Documento inválido. Informe um CPF/CNPJ/identificação válida.");
        }

        this.documento = documentoNormalizado;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("O e-mail do usuário não pode ser vazio.");
        }

        String emailNormalizado = email.trim();
        if (!emailNormalizado.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("E-mail inválido.");
        }

        this.email = emailNormalizado;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        if (senha == null || senha.trim().isEmpty()) {
            throw new IllegalArgumentException("A senha do usuário não pode ser vazia.");
        }
        this.senha = senha.trim();
    }

    public BigDecimal getSaldoFinanceiro() {
        return saldoFinanceiro;
    }

    public void setSaldoFinanceiro(BigDecimal saldoFinanceiro) {
        if (saldoFinanceiro == null) {
            throw new IllegalArgumentException("O saldo financeiro não pode ser nulo.");
        }

        BigDecimal saldoValidado = saldoFinanceiro.setScale(2, RoundingMode.HALF_UP);
        if (saldoValidado.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O saldo financeiro não pode ficar negativo.");
        }

        this.saldoFinanceiro = saldoValidado;
    }

    public List<Transacao> getHistoricoTransacoes() {
        return Collections.unmodifiableList(historicoTransacoes);
    }

    public void adicionarTransacao(Transacao transacao) {
        if (transacao == null) {
            throw new IllegalArgumentException("A transação não pode ser nula.");
        }
        historicoTransacoes.add(transacao);
    }

    public void removerTransacao(Transacao transacao) {
        if (transacao == null) {
            throw new IllegalArgumentException("A transação não pode ser nula.");
        }
        historicoTransacoes.remove(transacao);
    }

    public void setHistoricoTransacoes(List<Transacao> historicoTransacoes) {
        this.historicoTransacoes = historicoTransacoes == null ? new ArrayList<>() : new ArrayList<>(historicoTransacoes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nome, documento, email, senha, saldoFinanceiro, historicoTransacoes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Usuario)) {
            return false;
        }
        Usuario outro = (Usuario) obj;
        return Objects.equals(nome, outro.nome)
                && Objects.equals(documento, outro.documento)
                && Objects.equals(email, outro.email)
                && Objects.equals(senha, outro.senha)
                && Objects.equals(saldoFinanceiro, outro.saldoFinanceiro)
                && Objects.equals(historicoTransacoes, outro.historicoTransacoes);
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "nome='" + nome + '\'' +
                ", documento='" + documento + '\'' +
                ", email='" + email + '\'' +
                ", senha='" + senha + '\'' +
                ", saldoFinanceiro=" + saldoFinanceiro +
                ", historicoTransacoes=" + historicoTransacoes +
                '}';
    }
}
