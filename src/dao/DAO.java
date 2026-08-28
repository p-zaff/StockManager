package dao;

import java.util.List;

public interface DAO<T> {
    List<T> listarTodos();

    void salvarTodos(List<T> registros);
}
