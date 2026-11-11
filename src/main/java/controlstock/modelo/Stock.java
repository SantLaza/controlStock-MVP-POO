package controlstock.modelo;

public class Stock {

    private int idProducto;
    private int idDeposito;
    private int cantidad;
    private int stockMinimo;

    public Stock() {
    }

    public Stock(int idProducto, int idDeposito) {
        this.idProducto = idProducto;
        this.idDeposito = idDeposito;
        this.cantidad = 0;
        this.stockMinimo = 0;
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

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public boolean esBajo() {
        return cantidad < stockMinimo;
    }

    public int faltante() {
        return esBajo() ? stockMinimo - cantidad : 0;
    }

    @Override
    public String toString() {
        return "Producto " + idProducto + " en Depósito " + idDeposito
                + ": " + cantidad + " (mín: " + stockMinimo + ")";
    }
}
