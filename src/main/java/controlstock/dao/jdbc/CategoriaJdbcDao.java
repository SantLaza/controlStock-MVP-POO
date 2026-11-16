package controlstock.dao.jdbc;

import controlstock.dao.CategoriaDao;
import controlstock.modelo.Categoria;
import controlstock.modelo.Estado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoriaJdbcDao implements CategoriaDao {

    private static final String COLUMNAS =
            "SELECT idCategoria, nombreCategoria, estadoCategoria FROM CATEGORIA ";

    @Override
    public void insertar(Connection conn, Categoria categoria) throws SQLException {
        String sql = "INSERT INTO CATEGORIA (nombreCategoria, estadoCategoria) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, categoria.getNombreCategoria());
            ps.setString(2, categoria.getEstado().getValor());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    categoria.setIdCategoria(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void actualizar(Connection conn, Categoria categoria) throws SQLException {
        String sql = "UPDATE CATEGORIA SET nombreCategoria = ?, estadoCategoria = ? "
                   + "WHERE idCategoria = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoria.getNombreCategoria());
            ps.setString(2, categoria.getEstado().getValor());
            ps.setInt(3, categoria.getIdCategoria());
            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException(
                        "No existe la categoría con id " + categoria.getIdCategoria() + ".");
            }
        }
    }

    @Override
    public Categoria buscarPorId(Connection conn, int idCategoria) throws SQLException {
        String sql = COLUMNAS + "WHERE idCategoria = ?";
        return buscarUna(conn, sql, idCategoria);
    }

    @Override
    public Categoria buscarPorIdConBloqueo(Connection conn, int idCategoria) throws SQLException {
        String sql = COLUMNAS + "WHERE idCategoria = ? FOR UPDATE";
        return buscarUna(conn, sql, idCategoria);
    }

    @Override
    public List<Categoria> listarActivas(Connection conn) throws SQLException {
        String sql = COLUMNAS + "WHERE estadoCategoria = 'activo' ORDER BY nombreCategoria";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return listar(ps);
        }
    }

    @Override
    public List<Categoria> listarTodas(Connection conn) throws SQLException {
        String sql = COLUMNAS + "ORDER BY nombreCategoria";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return listar(ps);
        }
    }

    private Categoria buscarUna(Connection conn, String sql, int idCategoria) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCategoria);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
                return null;
            }
        }
    }

    private List<Categoria> listar(PreparedStatement ps) throws SQLException {
        List<Categoria> resultado = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        }
        return resultado;
    }

    private Categoria mapear(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setIdCategoria(rs.getInt("idCategoria"));
        c.setNombreCategoria(rs.getString("nombreCategoria"));
        if (Estado.desdeValor(rs.getString("estadoCategoria")) == Estado.INACTIVO) {
            c.darDeBaja();
        }
        return c;
    }
}
