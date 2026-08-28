package dao;

import model.Usuario;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public final class UsuarioDAO extends AbstractBinaryDAO<Usuario> {
    private static final Path ARQUIVO_USUARIOS = Paths.get("data", "binarios", "usuarios", "usuarios.bin");
    private static final UsuarioDAO INSTANCIA = new UsuarioDAO();

    private UsuarioDAO() {
        super(ARQUIVO_USUARIOS, Usuario.class);
    }

    public static UsuarioDAO getInstance() {
        return INSTANCIA;
    }

    public synchronized Optional<Usuario> buscarPorEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }

        String emailNormalizado = email.trim().toLowerCase();
        List<Usuario> usuarios = listarTodos();

        for (Usuario usuario : usuarios) {
            if (usuario.getEmail() != null && usuario.getEmail().trim().toLowerCase().equals(emailNormalizado)) {
                return Optional.of(usuario);
            }
        }

        return Optional.empty();
    }

    public synchronized Optional<Usuario> buscarPorDocumento(String documento) {
        if (documento == null || documento.trim().isEmpty()) {
            return Optional.empty();
        }

        String documentoNormalizado = documento.replaceAll("\\D", "");
        List<Usuario> usuarios = listarTodos();

        for (Usuario usuario : usuarios) {
            if (usuario.getDocumento() != null && usuario.getDocumento().equals(documentoNormalizado)) {
                return Optional.of(usuario);
            }
        }

        return Optional.empty();
    }
}
