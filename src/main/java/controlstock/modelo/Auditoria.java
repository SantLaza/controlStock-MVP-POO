package controlstock.modelo;

import java.time.LocalDateTime;

public class Auditoria {

    private int idAuditoria;
    private int idUsuario;
    private LocalDateTime fechaHoraAuditoria;
    private Accion accion;
    private EntidadAfectada entidadAfectada;
    private String registroAfectado;
    private String descripcionCambio;

    public Auditoria() {
    }

    public Auditoria(int idUsuario, Accion accion, EntidadAfectada entidadAfectada,
                     String registroAfectado, String descripcionCambio) {
        this.idUsuario = idUsuario;
        this.accion = accion;
        this.entidadAfectada = entidadAfectada;
        this.registroAfectado = registroAfectado;
        this.descripcionCambio = descripcionCambio;
    }

    public int getIdAuditoria() {
        return idAuditoria;
    }

    public void setIdAuditoria(int idAuditoria) {
        this.idAuditoria = idAuditoria;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDateTime getFechaHoraAuditoria() {
        return fechaHoraAuditoria;
    }

    public void setFechaHoraAuditoria(LocalDateTime fechaHoraAuditoria) {
        this.fechaHoraAuditoria = fechaHoraAuditoria;
    }

    public Accion getAccion() {
        return accion;
    }

    public void setAccion(Accion accion) {
        this.accion = accion;
    }

    public EntidadAfectada getEntidadAfectada() {
        return entidadAfectada;
    }

    public void setEntidadAfectada(EntidadAfectada entidadAfectada) {
        this.entidadAfectada = entidadAfectada;
    }

    public String getRegistroAfectado() {
        return registroAfectado;
    }

    public void setRegistroAfectado(String registroAfectado) {
        this.registroAfectado = registroAfectado;
    }

    public String getDescripcionCambio() {
        return descripcionCambio;
    }

    public void setDescripcionCambio(String descripcionCambio) {
        this.descripcionCambio = descripcionCambio;
    }

    @Override
    public String toString() {
        return "[" + idAuditoria + "] " + accion.getValor() + " en "
                + entidadAfectada.getValor() + " — " + descripcionCambio;
    }
}
