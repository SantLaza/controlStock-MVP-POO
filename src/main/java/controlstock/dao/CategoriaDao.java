package controlstock.dao;

import controlstock.modelo.Categoria;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface CategoriaDao {

    void insertar(Connection conn, Categoria categoria) throws SQLException;

    void actualizar(Connection conn, Categoria categoria) throws SQLException;

    Categoria buscarPorId(Connection conn, int idCategoria) throws SQLException;

    Categoria buscarPorIdConBloqueo(Connection conn, int idCategoria) throws SQLException;

    List<Categoria> listarActivas(Connection conn) throws SQLException;

    List<Categoria> listarTodas(Connection conn) throws SQLException;
}
