package controlstock.dao;

import controlstock.modelo.Producto;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface ProductoDao {

    void insertar(Connection conn, Producto producto) throws SQLException;

    void actualizar(Connection conn, Producto producto) throws SQLException;

    Producto buscarPorId(Connection conn, int idProducto) throws SQLException;

    Producto buscarPorIdConBloqueo(Connection conn, int idProducto) throws SQLException;

    Producto buscarPorCodigo(Connection conn, String codigoProducto) throws SQLException;

    List<Producto> listarActivos(Connection conn) throws SQLException;

    List<Producto> listarTodos(Connection conn) throws SQLException;

    boolean existeActivoPorCategoria(Connection conn, int idCategoria) throws SQLException;
}
