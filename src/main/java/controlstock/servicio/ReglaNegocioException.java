package controlstock.servicio;

import java.sql.SQLException;

public class ReglaNegocioException extends Exception {

    private static final long serialVersionUID = 1L;

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }

    public static ReglaNegocioException desdeSQL(SQLException e) {
        return switch (e.getErrorCode()) {
            case 1062 -> new ReglaNegocioException(
                    "Ya existe un registro con ese valor único.");
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

    public static boolean esDeadlock(SQLException e) {
        return e.getErrorCode() == 1213;
    }
}
