package controlstock.dao.jdbc;

import controlstock.dao.DepositoDao;
import controlstock.modelo.Deposito;
import controlstock.modelo.Estado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DepositoJdbcDao implements DepositoDao {

    private static final String COLUMNAS =
            "SELECT idDeposito, nombreDeposito, direccion, estadoDeposito FROM DEPOSITO ";

    @Override
    public void insertar(Connection conn, Deposito deposito) throws SQLException {
        String sql = "INSERT INTO DEPOSITO (nombreDeposito, direccion, estadoDeposito) "
                   + "VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, deposito.getNombreDeposito());
            ps.setString(2, deposito.getDireccion());
            ps.setString(3, deposito.getEstado().getValor());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    deposito.setIdDeposito(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void actualizar(Connection conn, Deposito deposito) throws SQLException {
        String sql = "UPDATE DEPOSITO SET nombreDeposito = ?, direccion = ?, estadoDeposito = ? "
                   + "WHERE idDeposito = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, deposito.getNombreDeposito());
            ps.setString(2, deposito.getDireccion());
            ps.setString(3, deposito.getEstado().getValor());
            ps.setInt(4, deposito.getIdDeposito());
            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException(
                        "No existe el deposito con id " + deposito.getIdDeposito() + ".");
            }
        }
    }

    @Override
    public Deposito buscarPorId(Connection conn, int idDeposito) throws SQLException {
        String sql = COLUMNAS + "WHERE idDeposito = ?";
        return buscarUno(conn, sql, idDeposito);
    }

    @Override
    public Deposito buscarPorIdConBloqueo(Connection conn, int idDeposito) throws SQLException {
        String sql = COLUMNAS + "WHERE idDeposito = ? FOR UPDATE";
        return buscarUno(conn, sql, idDeposito);
    }

    @Override
    public List<Deposito> listarActivos(Connection conn) throws SQLException {
        String sql = COLUMNAS + "WHERE estadoDeposito = 'activo' ORDER BY nombreDeposito";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return listar(ps);
        }
    }

    @Override
    public List<Deposito> listarTodos(Connection conn) throws SQLException {
        String sql = COLUMNAS + "ORDER BY nombreDeposito";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return listar(ps);
        }
    }

    @Override
    public int contarActivos(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM DEPOSITO WHERE estadoDeposito = 'activo' FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private Deposito buscarUno(Connection conn, String sql, int idDeposito) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDeposito);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
                return null;
            }
        }
    }

    private List<Deposito> listar(PreparedStatement ps) throws SQLException {
        List<Deposito> resultado = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        }
        return resultado;
    }

    private Deposito mapear(ResultSet rs) throws SQLException {
        Deposito d = new Deposito();
        d.setIdDeposito(rs.getInt("idDeposito"));
        d.setNombreDeposito(rs.getString("nombreDeposito"));
        d.setDireccion(rs.getString("direccion"));
        if (Estado.desdeValor(rs.getString("estadoDeposito")) == Estado.INACTIVO) {
            d.darDeBaja();
        }
        return d;
    }
}
