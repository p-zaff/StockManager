package dao;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractBinaryDAO<T> implements DAO<T> {
    private final Path caminhoArquivo;
    private final Class<T> tipoEntidade;

    protected AbstractBinaryDAO(Path caminhoArquivo, Class<T> tipoEntidade) {
        this.caminhoArquivo = caminhoArquivo;
        this.tipoEntidade = tipoEntidade;
    }

    @Override
    public synchronized List<T> listarTodos() {
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(caminhoArquivo)))) {
            Object objetoLido = ois.readObject();
            return validarELimparLista(objetoLido);
        } catch (NoSuchFileException e) {
            return new ArrayList<>();
        } catch (EOFException e) {
            return new ArrayList<>();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Falha ao desserializar dados de " + caminhoArquivo + ".", e);
        } catch (IOException e) {
            throw new IllegalStateException("Falha de leitura do arquivo " + caminhoArquivo + ".", e);
        }
    }

    @Override
    public synchronized void salvarTodos(List<T> registros) {
        List<T> dadosParaPersistir = registros == null ? Collections.emptyList() : new ArrayList<>(registros);

        try {
            Path diretorioPai = caminhoArquivo.getParent();
            if (diretorioPai != null) {
                Files.createDirectories(diretorioPai);
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(
                            caminhoArquivo,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE)))) {
                oos.writeObject(dadosParaPersistir);
                oos.flush();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Falha de escrita do arquivo " + caminhoArquivo + ".", e);
        }
    }

    private List<T> validarELimparLista(Object objetoLido) {
        if (!(objetoLido instanceof List<?>)) {
            throw new IllegalStateException("Conteúdo inválido no arquivo " + caminhoArquivo + ".");
        }

        List<?> listaBruta = (List<?>) objetoLido;
        List<T> listaTipada = new ArrayList<>(listaBruta.size());

        for (Object item : listaBruta) {
            if (!tipoEntidade.isInstance(item)) {
                throw new IllegalStateException("Registro inválido encontrado no arquivo " + caminhoArquivo + ".");
            }
            listaTipada.add(tipoEntidade.cast(item));
        }

        return listaTipada;
    }
}
