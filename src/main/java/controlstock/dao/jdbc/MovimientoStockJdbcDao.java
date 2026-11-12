package controlstock.dao.jdbc;

import controlstock.dao.MovimientoStockDao;
import controlstock.modelo.MovimientoStock;
import controlstock.modelo.TipoMovimiento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de {@link MovimientoStockDao} sobre la tabla MOVIMIENTO_STOCK.
 *
 * Igual que el resto de los DAOs, recibe la conexión del servicio: el movimiento
 * se inserta en la misma transacción que actualiza el saldo en STOCK.
 */
public class MovimientoStockJdbcDao implements MovimientoStockDao {

    private static final String COLUMNAS =
            "SELECT idMovimiento, idProducto, idDeposito, idUsuario, fechaHoraMovimiento, "
          + "tipoMovimiento, cantidad, motivo, idMovimientoCorregido "
          + "FROM MOVIMIENTO_STOCK ";

    /** Más reciente primero; idMovimiento desempata los del mismo segundo. */
    private static final String ORDEN =
            "ORDER BY fechaHoraMovimiento DESC, idMovimiento DESC";

    @Override
    public void insertar(Connection conn, MovimientoStock movimiento) throws SQLException {
        String sql = "INSERT INTO MOVIMIENTO_STOCK (idProducto, idDeposito, idUsuario, "
                   + "tipoMovimiento, cantidad, motivo, idMovimientoCorregido) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, movimiento.getIdProducto());
            ps.setInt(2, movimiento.getIdDeposito());
            ps.setInt(3, movimiento.getIdUsuario());
            ps.setString(4, movimiento.getTipoMovimiento().getValor());
            ps.setInt(5, movimiento.getCantidad());
            ps.setString(6, movimiento.getMotivo());
            if (movimiento.getIdMovimientoCorregido() == null) {
                ps.setNull(7, Types.INTEGER);
            } else {
                ps.setInt(7, movimiento.getIdMovimientoCorregido());
            }
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    movimiento.setIdMovimiento(claves.getInt(1));
                } else {
                    throw new SQLException("La base no devolvió el id del movimiento insertado.");
                }
            }
        }
    }

    @Override
    public MovimientoStock buscarPorId(Connection conn, int idMovimiento) throws SQLException {
        String sql = COLUMNAS + "WHERE idMovimiento = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMovimiento);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
                return null;
            }
        }
    }

    @Override
    public List<MovimientoStock> listarPorProductoDeposito(Connection conn, int idProducto,
                                                            int idDeposito) throws SQLException {
        String sql = COLUMNAS + "WHERE idProducto = ? AND idDeposito = ? " + ORDEN;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            ps.setInt(2, idDeposito);
            return listar(ps);
        }
    }

    @Override
    public List<MovimientoStock> listarTodos(Connection conn) throws SQLException {
        String sql = COLUMNAS + ORDEN;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return listar(ps);
        }
    }

    // Auxiliares privados

    /** Ejecuta una consulta ya parametrizada y convierte todas las filas. */
    private List<MovimientoStock> listar(PreparedStatement ps) throws SQLException {
        List<MovimientoStock> resultado = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        }
        return resultado;
    }

    /** Convierte la fila actual del ResultSet en un objeto MovimientoStock. */
    private MovimientoStock mapear(ResultSet rs) throws SQLException {
        MovimientoStock m = new MovimientoStock();
        m.setIdMovimiento(rs.getInt("idMovimiento"));
        m.setIdProducto(rs.getInt("idProducto"));
        m.setIdDeposito(rs.getInt("idDeposito"));
        m.setIdUsuario(rs.getInt("idUsuario"));
        m.setFechaHoraMovimiento(rs.getObject("fechaHoraMovimiento", LocalDateTime.class));
        m.setTipoMovimiento(TipoMovimiento.desdeValor(rs.getString("tipoMovimiento")));
        m.setCantidad(rs.getInt("cantidad"));
        m.setMotivo(rs.getString("motivo"));
        m.setIdMovimientoCorregido(rs.getObject("idMovimientoCorregido", Integer.class));
        return m;
    }
}
