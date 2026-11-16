package controlstock.dao.jdbc;

import controlstock.dao.ProductoDao;
import controlstock.modelo.Estado;
import controlstock.modelo.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductoJdbcDao implements ProductoDao {

    private static final String COLUMNAS =
            "SELECT idProducto, codigoProducto, idCategoria, nombreProducto, "
          + "descripcion, precioReferenciaCompra, precioReferenciaVenta, estadoProducto "
          + "FROM PRODUCTO ";

    @Override
    public void insertar(Connection conn, Producto producto) throws SQLException {
        String sql = "INSERT INTO PRODUCTO (codigoProducto, idCategoria, nombreProducto, "
                   + "descripcion, precioReferenciaCompra, precioReferenciaVenta, estadoProducto) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, producto.getCodigoProducto());
            ps.setInt(2, producto.getIdCategoria());
            ps.setString(3, producto.getNombreProducto());
            ps.setString(4, producto.getDescripcion());
            ps.setBigDecimal(5, producto.getPrecioReferenciaCompra());
            ps.setBigDecimal(6, producto.getPrecioReferenciaVenta());
            ps.setString(7, producto.getEstado().getValor());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    producto.setIdProducto(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void actualizar(Connection conn, Producto producto) throws SQLException {
        String sql = "UPDATE PRODUCTO SET codigoProducto = ?, idCategoria = ?, nombreProducto = ?, "
                   + "descripcion = ?, precioReferenciaCompra = ?, precioReferenciaVenta = ?, "
                   + "estadoProducto = ? WHERE idProducto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, producto.getCodigoProducto());
            ps.setInt(2, producto.getIdCategoria());
            ps.setString(3, producto.getNombreProducto());
            ps.setString(4, producto.getDescripcion());
            ps.setBigDecimal(5, producto.getPrecioReferenciaCompra());
            ps.setBigDecimal(6, producto.getPrecioReferenciaVenta());
            ps.setString(7, producto.getEstado().getValor());
            ps.setInt(8, producto.getIdProducto());
            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException(
                        "No existe el producto con id " + producto.getIdProducto() + ".");
            }
        }
    }

    @Override
    public Producto buscarPorId(Connection conn, int idProducto) throws SQLException {
        String sql = COLUMNAS + "WHERE idProducto = ?";
        return buscarUno(conn, sql, idProducto);
    }

    @Override
    public Producto buscarPorIdConBloqueo(Connection conn, int idProducto) throws SQLException {
        String sql = COLUMNAS + "WHERE idProducto = ? FOR UPDATE";
        return buscarUno(conn, sql, idProducto);
    }

    @Override
    public Producto buscarPorCodigo(Connection conn, String codigoProducto) throws SQLException {
        String sql = COLUMNAS + "WHERE codigoProducto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigoProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
                return null;
            }
        }
    }

    @Override
    public List<Producto> listarActivos(Connection conn) throws SQLException {
        String sql = COLUMNAS + "WHERE estadoProducto = 'activo' ORDER BY nombreProducto";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return listar(ps);
        }
    }

    @Override
    public List<Producto> listarTodos(Connection conn) throws SQLException {
        String sql = COLUMNAS + "ORDER BY nombreProducto";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return listar(ps);
        }
    }

    @Override
    public boolean existeActivoPorCategoria(Connection conn, int idCategoria) throws SQLException {
        String sql = "SELECT 1 FROM PRODUCTO WHERE idCategoria = ? AND estadoProducto = 'activo' LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCategoria);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Producto buscarUno(Connection conn, String sql, int idProducto) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
                return null;
            }
        }
    }

    private List<Producto> listar(PreparedStatement ps) throws SQLException {
        List<Producto> resultado = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        }
        return resultado;
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setIdProducto(rs.getInt("idProducto"));
        p.setCodigoProducto(rs.getString("codigoProducto"));
        p.setIdCategoria(rs.getInt("idCategoria"));
        p.setNombreProducto(rs.getString("nombreProducto"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setPrecioReferenciaCompra(rs.getBigDecimal("precioReferenciaCompra"));
        p.setPrecioReferenciaVenta(rs.getBigDecimal("precioReferenciaVenta"));
        if (Estado.desdeValor(rs.getString("estadoProducto")) == Estado.INACTIVO) {
            p.darDeBaja();
        }
        return p;
    }
}
