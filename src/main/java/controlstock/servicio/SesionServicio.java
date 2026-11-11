package controlstock.servicio;

import controlstock.dao.UsuarioDao;
import controlstock.modelo.Estado;
import controlstock.modelo.Usuario;
import controlstock.util.ConexionDB;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.SQLException;

public class SesionServicio {

    private final UsuarioDao usuarioDao;
    private Usuario usuarioActual;

    public SesionServicio(UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    public Usuario iniciarSesion(String nombreUsuario, String contrasena)
            throws ReglaNegocioException {
        try (Connection conn = ConexionDB.obtenerConexion()) {
            Usuario usuario = usuarioDao.buscarPorNombre(conn, nombreUsuario);

            if (usuario == null) {
                throw new ReglaNegocioException("Usuario o contraseña incorrectos.");
            }

            if (usuario.getEstadoUsuario() != Estado.ACTIVO) {
                throw new ReglaNegocioException("El usuario está inactivo.");
            }

            if (!BCrypt.checkpw(contrasena, usuario.getHashContrasena())) {
                throw new ReglaNegocioException("Usuario o contraseña incorrectos.");
            }

            this.usuarioActual = usuario;
            return usuario;

        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e);
        }
    }

    public void cerrarSesion() {
        this.usuarioActual = null;
    }

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public boolean haySesionActiva() {
        return usuarioActual != null;
    }
}
