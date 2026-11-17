package controlstock.servicio;

import controlstock.modelo.Deposito;
import controlstock.modelo.Producto;
import controlstock.modelo.Stock;

/**
 * Una fila de las consultas de stock: el saldo de un producto en un depósito,
 * junto con el producto y el depósito completos.
 *
 * {@link Stock} solo guarda los ids; para mostrar nombres y códigos, el
 * {@link ConsultaServicio} arma esta clase con los tres objetos. No se guarda
 * en la base: es solo el resultado de una consulta.
 */
public class LineaStock {

    private final Producto producto;
    private final Deposito deposito;
    private final Stock stock;

    public LineaStock(Producto producto, Deposito deposito, Stock stock) {
        this.producto = producto;
        this.deposito = deposito;
        this.stock = stock;
    }

    public Producto getProducto() {
        return producto;
    }

    public Deposito getDeposito() {
        return deposito;
    }

    public Stock getStock() {
        return stock;
    }
}
