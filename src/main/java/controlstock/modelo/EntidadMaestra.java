package controlstock.modelo;

/**
 * Comportamiento común a los datos maestros (Categoría, Producto y Depósito):
 * todos nacen activos y se dan de baja de forma lógica, nunca se borran.
 *
 * La entidad solo controla su propio cambio de estado. Las reglas que dependen
 * de otras entidades (por ejemplo, no dar de baja un depósito con existencias)
 * las valida el servicio antes de llamar a {@link #darDeBaja()}.
 */
public abstract class EntidadMaestra {

    private Estado estado = Estado.ACTIVO;

    /** Nombre visible de la entidad, para listados y mensajes. */
    public abstract String getNombre();

    public Estado getEstado() {
        return estado;
    }

    public boolean estaActivo() {
        return estado == Estado.ACTIVO;
    }

    /**
     * Baja lógica: pasa la entidad a inactivo.
     *
     * @throws IllegalStateException si ya estaba inactiva
     */
    public void darDeBaja() {
        if (!estaActivo()) {
            throw new IllegalStateException("'" + getNombre() + "' ya está inactivo.");
        }
        estado = Estado.INACTIVO;
    }

    /**
     * Vuelve a activar una entidad dada de baja.
     *
     * @throws IllegalStateException si ya estaba activa
     */
    public void reactivar() {
        if (estaActivo()) {
            throw new IllegalStateException("'" + getNombre() + "' ya está activo.");
        }
        estado = Estado.ACTIVO;
    }
}
