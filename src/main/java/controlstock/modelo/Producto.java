package controlstock.modelo;

import java.math.BigDecimal;

public class Producto extends EntidadMaestra {

    private int idProducto;
    private String codigoProducto;
    private int idCategoria;
    private String nombreProducto;
    private String descripcion;
    private BigDecimal precioReferenciaCompra;
    private BigDecimal precioReferenciaVenta;

    public Producto() {
    }

    public Producto(String codigoProducto, int idCategoria, String nombreProducto,
                    BigDecimal precioReferenciaCompra, BigDecimal precioReferenciaVenta) {
        this.codigoProducto = codigoProducto;
        this.idCategoria = idCategoria;
        this.nombreProducto = nombreProducto;
        this.precioReferenciaCompra = precioReferenciaCompra;
        this.precioReferenciaVenta = precioReferenciaVenta;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getCodigoProducto() {
        return codigoProducto;
    }

    public void setCodigoProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecioReferenciaCompra() {
        return precioReferenciaCompra;
    }

    public void setPrecioReferenciaCompra(BigDecimal precioReferenciaCompra) {
        this.precioReferenciaCompra = precioReferenciaCompra;
    }

    public BigDecimal getPrecioReferenciaVenta() {
        return precioReferenciaVenta;
    }

    public void setPrecioReferenciaVenta(BigDecimal precioReferenciaVenta) {
        this.precioReferenciaVenta = precioReferenciaVenta;
    }

    @Override
    public String getNombre() {
        return nombreProducto;
    }

    @Override
    public String toString() {
        return "[" + codigoProducto + "] " + nombreProducto
                + " (" + getEstado().getValor() + ")";
    }
}
