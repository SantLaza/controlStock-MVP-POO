package controlstock.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Configuracion {

    private final String url;
    private final String usuario;
    private final String contrasena;

    private Configuracion(String url, String usuario, String contrasena) {
        this.url = url;
        this.usuario = usuario;
        this.contrasena = contrasena;
    }

    public static Configuracion cargar() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("db.properties")) {
            props.load(fis);
        } catch (IOException e) {
            throw new RuntimeException(
                    "No se encontró db.properties. Copiá db.properties.example "
                    + "como db.properties y completá la contraseña.", e);
        }

        String url = props.getProperty("db.url");
        String usuario = props.getProperty("db.usuario");
        String contrasena = props.getProperty("db.contrasena");

        if (url == null || usuario == null || contrasena == null) {
            throw new RuntimeException(
                    "db.properties incompleto: se necesitan db.url, db.usuario y db.contrasena.");
        }

        return new Configuracion(url, usuario, contrasena);
    }

    public String getUrl() {
        return url;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getContrasena() {
        return contrasena;
    }
}
