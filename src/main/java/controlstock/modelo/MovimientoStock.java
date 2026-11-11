package controlstock.modelo;

import java.time.LocalDateTime;

public class MovimientoStock {

    private int idMovimiento;
    private int idProducto;
    private int idDeposito;
    private int idUsuario;
    private LocalDateTime fechaHoraMovimiento;
    private TipoMovimiento tipoMovimiento;
    private int cantidad;
    private String motivo;
    private Integer idMovimientoCorregido;

    public MovimientoStock() {
    }

    public MovimientoStock(int idProducto, int idDeposito, int idUsuario,
                           TipoMovimiento tipoMovimiento, int cantidad, String motivo) {
        this.idProducto = idProducto;
        this.idDeposito = idDeposito;
        this.idUsuario = idUsuario;
        this.tipoMovimiento = tipoMovimiento;
        this.cantidad = cantidad;
        this.motivo = motivo;
    }

    public int getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public int getIdDeposito() {
        return idDeposito;
    }

    public void setIdDeposito(int idDeposito) {
        this.idDeposito = idDeposito;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDateTime getFechaHoraMovimiento() {
        return fechaHoraMovimiento;
    }

    public void setFechaHoraMovimiento(LocalDateTime fechaHoraMovimiento) {
        this.fechaHoraMovimiento = fechaHoraMovimiento;
    }

    public TipoMovimiento getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(TipoMovimiento tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Integer getIdMovimientoCorregido() {
        return idMovimientoCorregido;
    }

    public void setIdMovimientoCorregido(Integer idMovimientoCorregido) {
        this.idMovimientoCorregido = idMovimientoCorregido;
    }

    @Override
    public String toString() {
        return "[" + idMovimiento + "] " + tipoMovimiento.getValor()
                + " x" + cantidad + " — " + motivo;
    }
}
