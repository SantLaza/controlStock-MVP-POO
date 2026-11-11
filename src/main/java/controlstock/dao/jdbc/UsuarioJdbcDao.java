package controlstock.dao.jdbc;

import controlstock.dao.UsuarioDao;
import controlstock.modelo.Estado;
import controlstock.modelo.Rol;
import controlstock.modelo.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioJdbcDao implements UsuarioDao {

    @Override
    public Usuario buscarPorNombre(Connection conn, String nombreUsuario) throws SQLException {
        String sql = "SELECT idUsuario, nombreUsuario, hashContrasena, rol, estadoUsuario "
                   + "FROM USUARIO WHERE nombreUsuario = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
                return null;
            }
        }
    }

    @Override
    public Usuario buscarPorId(Connection conn, int idUsuario) throws SQLException {
        String sql = "SELECT idUsuario, nombreUsuario, hashContrasena, rol, estadoUsuario "
                   + "FROM USUARIO WHERE idUsuario = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
                return null;
            }
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("idUsuario"));
        u.setNombreUsuario(rs.getString("nombreUsuario"));
        u.setHashContrasena(rs.getString("hashContrasena"));
        u.setRol(Rol.desdeValor(rs.getString("rol")));
        u.setEstadoUsuario(Estado.desdeValor(rs.getString("estadoUsuario")));
        return u;
    }
}
