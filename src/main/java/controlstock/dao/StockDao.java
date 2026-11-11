package controlstock.dao;

import controlstock.modelo.Stock;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface StockDao {

    void insertar(Connection conn, Stock stock) throws SQLException;

    void actualizar(Connection conn, Stock stock) throws SQLException;

    Stock buscarPorIds(Connection conn, int idProducto, int idDeposito) throws SQLException;

    Stock buscarPorIdsConBloqueo(Connection conn, int idProducto, int idDeposito)
            throws SQLException;

    List<Stock> listarPorProducto(Connection conn, int idProducto) throws SQLException;

    List<Stock> listarPorDeposito(Connection conn, int idDeposito) throws SQLException;

    List<Stock> listarStockBajo(Connection conn) throws SQLException;

    boolean existeConCantidadPositiva(Connection conn, int idProducto) throws SQLException;

    boolean existeConCantidadPositivaPorDeposito(Connection conn, int idDeposito)
            throws SQLException;
}
