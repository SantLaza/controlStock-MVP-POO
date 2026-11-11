package controlstock.dao;

import controlstock.modelo.Usuario;
import java.sql.Connection;
import java.sql.SQLException;

public interface UsuarioDao {

    Usuario buscarPorNombre(Connection conn, String nombreUsuario) throws SQLException;

    Usuario buscarPorId(Connection conn, int idUsuario) throws SQLException;
}
