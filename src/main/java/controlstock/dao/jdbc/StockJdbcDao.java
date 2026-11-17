package controlstock.dao.jdbc;

import controlstock.dao.StockDao;
import controlstock.modelo.Stock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de {@link StockDao} sobre la tabla STOCK.
 *
 * El DAO no abre, no confirma ni cierra conexiones: recibe la conexión del
 * servicio, que es quien decide dónde empieza y dónde termina la transacción.
 */
public class StockJdbcDao implements StockDao {

    private static final String COLUMNAS =
            "SELECT idProducto, idDeposito, cantidad, stockMinimo FROM STOCK ";

    @Override
    public void insertar(Connection conn, Stock stock) throws SQLException {
        String sql = "INSERT INTO STOCK (idProducto, idDeposito, cantidad, stockMinimo) "
                   + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stock.getIdProducto());
            ps.setInt(2, stock.getIdDeposito());
            ps.setInt(3, stock.getCantidad());
            ps.setInt(4, stock.getStockMinimo());
            ps.executeUpdate();
        }
    }

    @Override
    public void actualizar(Connection conn, Stock stock) throws SQLException {
        String sql = "UPDATE STOCK SET cantidad = ?, stockMinimo = ? "
                   + "WHERE idProducto = ? AND idDeposito = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stock.getCantidad());
            ps.setInt(2, stock.getStockMinimo());
            ps.setInt(3, stock.getIdProducto());
            ps.setInt(4, stock.getIdDeposito());
            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException("No existe stock para el producto "
                        + stock.getIdProducto() + " en el depósito " + stock.getIdDeposito() + ".");
            }
        }
    }

    @Override
    public Stock buscarPorIds(Connection conn, int idProducto, int idDeposito)
            throws SQLException {
        String sql = COLUMNAS + "WHERE idProducto = ? AND idDeposito = ?";
        return buscarUno(conn, sql, idProducto, idDeposito);
    }

    @Override
    public Stock buscarPorIdsConBloqueo(Connection conn, int idProducto, int idDeposito)
            throws SQLException {
        String sql = COLUMNAS + "WHERE idProducto = ? AND idDeposito = ? FOR UPDATE";
        return buscarUno(conn, sql, idProducto, idDeposito);
    }

    @Override
    public List<Stock> listarPorProducto(Connection conn, int idProducto) throws SQLException {
        String sql = COLUMNAS + "WHERE idProducto = ? ORDER BY idDeposito";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            return listar(ps);
        }
    }

    @Override
    public List<Stock> listarPorDeposito(Connection conn, int idDeposito) throws SQLException {
        String sql = COLUMNAS + "WHERE idDeposito = ? ORDER BY idProducto";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDeposito);
            return listar(ps);
        }
    }

    @Override
    public List<Stock> listarStockBajo(Connection conn) throws SQLException {
        String sql = COLUMNAS + "WHERE stockMinimo > 0 AND cantidad < stockMinimo "
                   + "ORDER BY idProducto, idDeposito";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return listar(ps);
        }
    }

    /*
     * Los dos métodos siguientes los usan las bajas de producto y de depósito.
     * Llevan FOR SHARE (modelo_relacional_mvp.md §5.4) para leer el saldo ya
     * confirmado y no la foto de la transacción: un ajuste en curso sobre esas
     * filas obliga a esperar su commit antes de decidir la baja.
     */

    @Override
    public boolean existeConCantidadPositiva(Connection conn, int idProducto)
            throws SQLException {
        String sql = "SELECT 1 FROM STOCK WHERE idProducto = ? AND cantidad > 0 "
                   + "LIMIT 1 FOR SHARE";
        return existe(conn, sql, idProducto);
    }

    @Override
    public boolean existeConCantidadPositivaPorDeposito(Connection conn, int idDeposito)
            throws SQLException {
        String sql = "SELECT 1 FROM STOCK WHERE idDeposito = ? AND cantidad > 0 "
                   + "LIMIT 1 FOR SHARE";
        return existe(conn, sql, idDeposito);
    }

    // Auxiliares privados

    /** Ejecuta una búsqueda por clave compuesta. Devuelve null si no hay fila. */
    private Stock buscarUno(Connection conn, String sql, int idProducto, int idDeposito)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            ps.setInt(2, idDeposito);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
                return null;
            }
        }
    }

    /** Ejecuta una consulta ya parametrizada y convierte todas las filas. */
    private List<Stock> listar(PreparedStatement ps) throws SQLException {
        List<Stock> resultado = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        }
        return resultado;
    }

    /** Ejecuta un "SELECT 1 ... LIMIT 1" con un parámetro: true si encontró alguna fila. */
    private boolean existe(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Convierte la fila actual del ResultSet en un objeto Stock. */
    private Stock mapear(ResultSet rs) throws SQLException {
        return new Stock(
                rs.getInt("idProducto"),
                rs.getInt("idDeposito"),
                rs.getInt("cantidad"),
                rs.getInt("stockMinimo"));
    }
}
