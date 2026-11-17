package controlstock.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class Configuracion {

    private static final String NOMBRE_ARCHIVO = "db.properties";

    /** Permite indicar otra ubicacion: java -Ddb.config=C:\ruta\db.properties ... */
    private static final String PROPIEDAD_RUTA = "db.config";

    private final String url;
    private final String usuario;
    private final String contrasena;

    private Configuracion(String url, String usuario, String contrasena) {
        this.url = url;
        this.usuario = usuario;
        this.contrasena = contrasena;
    }

    public static Configuracion cargar() {
        Set<Path> candidatas = rutasCandidatas();
        Path archivo = null;
        for (Path ruta : candidatas) {
            if (Files.isRegularFile(ruta)) {
                archivo = ruta;
                break;
            }
        }

        if (archivo == null) {
            StringBuilder msg = new StringBuilder("No se encontró ")
                    .append(NOMBRE_ARCHIVO)
                    .append(". Copiá db.properties.example como db.properties en la raíz ")
                    .append("del proyecto y completá la contraseña. Rutas buscadas:");
            for (Path ruta : candidatas) {
                msg.append(System.lineSeparator()).append("  - ").append(ruta);
            }
            throw new RuntimeException(msg.toString());
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(archivo)) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer " + archivo + ": " + e.getMessage(), e);
        }

        String url = props.getProperty("db.url");
        String usuario = props.getProperty("db.usuario");
        String contrasena = props.getProperty("db.contrasena");

        if (url == null || usuario == null || contrasena == null) {
            throw new RuntimeException(
                    archivo + " incompleto: se necesitan db.url, db.usuario y db.contrasena.");
        }

        return new Configuracion(url.trim(), usuario.trim(), contrasena);
    }

    /**
     * Orden de busqueda:
     * 1. La ruta indicada con -Ddb.config.
     * 2. La carpeta desde donde se ejecuta la aplicacion.
     * 3. La raiz del proyecto, deducida de donde estan las clases compiladas
     *    (target/classes o target/*.jar), para que funcione aunque se ejecute
     *    desde otra carpeta (por ejemplo, con el boton Run del IDE).
     */
    private static Set<Path> rutasCandidatas() {
        Set<Path> rutas = new LinkedHashSet<>();

        String explicita = System.getProperty(PROPIEDAD_RUTA);
        if (explicita != null && !explicita.isBlank()) {
            rutas.add(Paths.get(explicita).toAbsolutePath().normalize());
        }

        rutas.add(Paths.get(NOMBRE_ARCHIVO).toAbsolutePath().normalize());

        for (Path dir : directoriosDelProyecto()) {
            rutas.add(dir.resolve(NOMBRE_ARCHIVO).normalize());
        }
        return rutas;
    }

    private static List<Path> directoriosDelProyecto() {
        List<Path> dirs = new ArrayList<>();
        try {
            var origen = Configuracion.class.getProtectionDomain().getCodeSource();
            if (origen == null) {
                return dirs;
            }
            // target/classes o target/controlstock-mvp.jar -> se sube hasta encontrar pom.xml
            Path actual = Paths.get(origen.getLocation().toURI()).toAbsolutePath();
            for (int i = 0; i < 4 && actual != null; i++) {
                if (Files.isRegularFile(actual.resolve("pom.xml"))) {
                    dirs.add(actual);
                    break;
                }
                actual = actual.getParent();
            }
        } catch (URISyntaxException | SecurityException | IllegalArgumentException e) {
            // Sin ubicacion de las clases: se usan solo las otras rutas
        }
        return dirs;
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
