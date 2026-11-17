package controlstock.servicio;

import java.sql.SQLException;

public class ReglaNegocioException extends Exception {

    private static final long serialVersionUID = 1L;

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }

    public static ReglaNegocioException desdeSQL(SQLException e) {
        return desdeSQL(e, "Ya existe un registro con ese valor único.");
    }

    /**
     * Igual que {@link #desdeSQL(SQLException)}, pero con el mensaje que se
     * muestra si el error es un duplicado (1062). El servicio sabe qué campo es
     * único en cada caso y puede nombrarlo; MySQL solo informa el índice.
     */
    public static ReglaNegocioException desdeSQL(SQLException e, String mensajeDuplicado) {
        return switch (e.getErrorCode()) {
            case 1062 -> new ReglaNegocioException(mensajeDuplicado);
            case 1452 -> new ReglaNegocioException(
                    "No existe el registro referenciado.");
            case 3819 -> new ReglaNegocioException(
                    "Los datos no cumplen las restricciones de la base.");
            case 1142 -> new ReglaNegocioException(
                    "La cuenta de la aplicación no tiene permisos para esa operación.");
            case 1213 -> new ReglaNegocioException(
                    "Se detectó un bloqueo mutuo (deadlock). Reintentá la operación.");
            case 1205 -> new ReglaNegocioException(
                    "Se superó el tiempo de espera del bloqueo. Reintentá la operación.");
            default -> new ReglaNegocioException(
                    "Error de base de datos: " + e.getMessage());
        };
    }

    /**
     * Mensaje de duplicado para un nombre o código único. La base compara sin
     * distinguir mayúsculas ni tildes (utf8mb4_0900_ai_ci), así que "almacen"
     * choca con "Almacén": se aclara para que el rechazo no parezca un error.
     *
     * @param registro por ejemplo "una categoría"
     * @param campo    por ejemplo "el nombre"
     */
    public static String mensajeDuplicado(String registro, String campo, String valor) {
        return "Ya existe " + registro + " con " + campo + " '" + valor
                + "' (no se distinguen mayúsculas ni tildes).";
    }

    public static boolean esDeadlock(SQLException e) {
        return e.getErrorCode() == 1213;
    }
}
