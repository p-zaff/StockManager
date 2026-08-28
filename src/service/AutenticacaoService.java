package service;

import dao.UsuarioDAO;
import exception.AutenticacaoFalhouException;
import model.Usuario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class AutenticacaoService {
    private final UsuarioDAO usuarioDAO;

    public AutenticacaoService(UsuarioDAO usuarioDAO) {
        if (usuarioDAO == null) {
            throw new IllegalArgumentException("O UsuarioDAO nao pode ser nulo.");
        }
        this.usuarioDAO = usuarioDAO;
    }

    public synchronized Usuario registrar(String nome,
                                          String documento,
                                          String email,
                                          String senha,
                                          BigDecimal saldoInicial) {
        if (usuarioDAO.buscarPorEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ja existe um usuario com este e-mail.");
        }
        if (usuarioDAO.buscarPorDocumento(documento).isPresent()) {
            throw new IllegalArgumentException("Ja existe um usuario com este documento.");
        }

        BigDecimal saldo = saldoInicial == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : saldoInicial.setScale(2, RoundingMode.HALF_UP);

        Usuario novoUsuario = new Usuario(nome, documento, email, senha, saldo);
        List<Usuario> usuarios = new ArrayList<>(usuarioDAO.listarTodos());
        usuarios.add(novoUsuario);
        usuarioDAO.salvarTodos(usuarios);
        return novoUsuario;
    }

    public synchronized Usuario autenticar(String email, String senha) {
        Usuario usuario = usuarioDAO.buscarPorEmail(email)
                .orElseThrow(() -> new AutenticacaoFalhouException("Usuario ou senha invalidos."));

        String senhaFornecida = senha == null ? "" : senha.trim();
        if (!usuario.getSenha().equals(senhaFornecida)) {
            throw new AutenticacaoFalhouException("Usuario ou senha invalidos.");
        }

        return usuario;
    }
}
