package controlstock.dao;

import controlstock.modelo.MovimientoStock;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface MovimientoStockDao {

    void insertar(Connection conn, MovimientoStock movimiento) throws SQLException;

    MovimientoStock buscarPorId(Connection conn, int idMovimiento) throws SQLException;

    List<MovimientoStock> listarPorProductoDeposito(Connection conn, int idProducto,
                                                     int idDeposito) throws SQLException;

    List<MovimientoStock> listarTodos(Connection conn) throws SQLException;
}
