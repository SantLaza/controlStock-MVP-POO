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

    public Stock(int idProducto, int idDeposito, int cantidad, int stockMinimo) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad en stock no puede ser negativa.");
        }
        if (stockMinimo < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo.");
        }
        this.idProducto = idProducto;
        this.idDeposito = idDeposito;
        this.cantidad = cantidad;
        this.stockMinimo = stockMinimo;
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

    public void aumentar(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a aumentar debe ser mayor a 0.");
        }
        try {
            this.cantidad = Math.addExact(this.cantidad, cantidad);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "No se puede aumentar " + cantidad + ": el saldo superaría el máximo permitido.", e);
        }
    }

    public void disminuir(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a disminuir debe ser mayor a 0.");
        }
        if (cantidad > this.cantidad) {
            throw new IllegalStateException(
                    "Stock insuficiente. Saldo actual: " + this.cantidad + ", se pidió disminuir: " + cantidad + ".");
        }
        this.cantidad -= cantidad;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void definirMinimo(int nuevoMinimo) {
        if (nuevoMinimo < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo.");
        }
        this.stockMinimo = nuevoMinimo;
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
