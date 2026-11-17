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

            // La contraseña se verifica antes que el estado: así, sin conocer la
            // contraseña, no se puede saber si un usuario existe ni si está inactivo.
            if (usuario == null || !contrasenaCorrecta(contrasena, usuario.getHashContrasena())) {
                throw new ReglaNegocioException("Usuario o contraseña incorrectos.");
            }

            if (usuario.getEstadoUsuario() != Estado.ACTIVO) {
                throw new ReglaNegocioException("El usuario está inactivo.");
            }

            this.usuarioActual = usuario;
            return usuario;

        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e);
        }
    }

    /**
     * Compara la contraseña con el hash guardado. Un hash vacío o mal formado
     * (por ejemplo, cargado a mano en la base) hace que BCrypt lance una
     * excepción: se trata como contraseña incorrecta en lugar de cerrar la app.
     */
    private boolean contrasenaCorrecta(String contrasena, String hash) {
        if (contrasena == null || hash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(contrasena, hash);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return false;
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
