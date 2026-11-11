package controlstock.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionDB {

    private static String url;
    private static String usuario;
    private static String contrasena;
    private static boolean inicializada = false;

    private ConexionDB() {
    }

    public static void inicializar() throws SQLException {
        Configuracion config = Configuracion.cargar();
        url = config.getUrl();
        usuario = config.getUsuario();
        contrasena = config.getContrasena();

        try (Connection conn = DriverManager.getConnection(url, usuario, contrasena)) {
            verificarModoEstricto(conn);
        }
        inicializada = true;
    }

    public static Connection obtenerConexion() throws SQLException {
        if (!inicializada) {
            throw new IllegalStateException(
                    "ConexionDB no fue inicializada. Llamar a inicializar() primero.");
        }
        return DriverManager.getConnection(url, usuario, contrasena);
    }

    private static void verificarModoEstricto(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT @@SESSION.sql_mode")) {
            if (rs.next()) {
                String sqlMode = rs.getString(1);
                if (!sqlMode.contains("STRICT_TRANS_TABLES")) {
                    throw new SQLException(
                            "El servidor MySQL no tiene STRICT_TRANS_TABLES habilitado. "
                            + "La aplicación requiere modo estricto para funcionar correctamente. "
                            + "sql_mode actual: " + sqlMode);
                }
            }
        }
    }
}
