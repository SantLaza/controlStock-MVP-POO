package controlstock.ui;

import controlstock.modelo.Stock;
import controlstock.servicio.ReglaNegocioException;
import controlstock.servicio.SesionServicio;
import controlstock.servicio.StockServicio;

import java.util.Scanner;

/**
 * Menú del módulo Stock: habilitar un producto en un depósito, definir su
 * stock mínimo y registrar ajustes positivos y negativos.
 *
 * Es solo capa de interfaz: pide los datos por consola, llama a
 * {@link StockServicio} y muestra el resultado o el mensaje de error.
 * Las reglas de negocio (cantidad > 0, motivo obligatorio, saldo suficiente,
 * producto habilitado...) las valida el servicio, no el menú.
 *
 * Por ahora se piden los ids directamente, sin mostrar listas de productos
 * y depósitos.
 */
public class MenuStock {

    private final Scanner scanner;
    private final StockServicio stockServicio;
    private final SesionServicio sesionServicio;

    public MenuStock(Scanner scanner, StockServicio stockServicio,
                     SesionServicio sesionServicio) {
        this.scanner = scanner;
        this.stockServicio = stockServicio;
        this.sesionServicio = sesionServicio;
    }

    public void ejecutar() {
        while (true) {
            mostrarMenu();
            int opcion = leerOpcion(0, 4);

            if (opcion == 0) {
                return; // vuelve al menú principal
            }

            try {
                switch (opcion) {
                    case 1 -> habilitar();
                    case 2 -> definirMinimo();
                    case 3 -> ajustarPositivo();
                    case 4 -> ajustarNegativo();
                }
            } catch (ReglaNegocioException e) {
                System.out.println("Error: " + e.getMessage());
            }
            System.out.println();
        }
    }

    private void mostrarMenu() {
        System.out.println("--- Stock ---");
        System.out.println("1. Habilitar producto en depósito");
        System.out.println("2. Definir stock mínimo");
        System.out.println("3. Ajuste positivo");
        System.out.println("4. Ajuste negativo");
        System.out.println("0. Volver");
    }

    // Opciones

    private void habilitar() throws ReglaNegocioException {
        System.out.println("-- Habilitar producto en depósito --");
        int idProducto = leerEntero("Id del producto: ");
        int idDeposito = leerEntero("Id del depósito: ");

        stockServicio.habilitar(idProducto, idDeposito);
        System.out.println("Producto habilitado en el depósito (saldo 0, mínimo 0).");
    }

    private void definirMinimo() throws ReglaNegocioException {
        System.out.println("-- Definir stock mínimo --");
        int idProducto = leerEntero("Id del producto: ");
        int idDeposito = leerEntero("Id del depósito: ");
        int nuevoMinimo = leerEntero("Nuevo stock mínimo: ");

        Stock stock = stockServicio.definirMinimo(idProducto, idDeposito, nuevoMinimo);
        System.out.println("Mínimo actualizado a " + stock.getStockMinimo()
                + ". Saldo actual: " + stock.getCantidad() + ".");
        mostrarAvisoStockBajo(stock);
    }

    private void ajustarPositivo() throws ReglaNegocioException {
        System.out.println("-- Ajuste positivo --");
        int idProducto = leerEntero("Id del producto: ");
        int idDeposito = leerEntero("Id del depósito: ");
        int cantidad = leerEntero("Cantidad a sumar: ");
        String motivo = leerTexto("Motivo: ");

        Stock stock = stockServicio.ajustarPositivo(
                idProducto, idDeposito, cantidad, motivo, idUsuarioActual());
        System.out.println("Ajuste registrado. Saldo actual: " + stock.getCantidad() + ".");
        mostrarAvisoStockBajo(stock);
    }

    private void ajustarNegativo() throws ReglaNegocioException {
        System.out.println("-- Ajuste negativo --");
        int idProducto = leerEntero("Id del producto: ");
        int idDeposito = leerEntero("Id del depósito: ");
        int cantidad = leerEntero("Cantidad a restar: ");
        String motivo = leerTexto("Motivo: ");

        Stock stock = stockServicio.ajustarNegativo(
                idProducto, idDeposito, cantidad, motivo, idUsuarioActual());
        System.out.println("Ajuste registrado. Saldo actual: " + stock.getCantidad() + ".");
        mostrarAvisoStockBajo(stock);
    }

    // Auxiliares

    /**
     * Si el par producto–depósito quedó por debajo del mínimo, lo avisa.
     * Lo usan las opciones 2, 3 y 4. La decisión la toma el propio Stock
     * (esBajo / faltante); el menú solo la muestra.
     */
    private void mostrarAvisoStockBajo(Stock stock) {
        if (stock.esBajo()) {
            System.out.println("AVISO - Stock bajo: el mínimo es " + stock.getStockMinimo()
                    + ", faltan " + stock.faltante() + " unidades.");
        }
    }

    /** Id del usuario logueado: queda registrado en el movimiento de stock. */
    private int idUsuarioActual() {
        return sesionServicio.getUsuarioActual().getIdUsuario();
    }

    // Lectura de datos por consola

    private int leerOpcion(int min, int max) {
        while (true) {
            System.out.print("Opción: ");
            String linea = scanner.nextLine().trim();
            try {
                int opcion = Integer.parseInt(linea);
                if (opcion >= min && opcion <= max) {
                    System.out.println();
                    return opcion;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Opción no válida. Ingresá un número entre "
                    + min + " y " + max + ".");
        }
    }

    /**
     * Pide un número entero y vuelve a preguntar hasta que lo sea.
     * Solo controla el formato; si el valor tiene sentido (por ejemplo,
     * cantidad > 0) lo decide el servicio.
     */
    private int leerEntero(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = scanner.nextLine().trim();
            try {
                return Integer.parseInt(linea);
            } catch (NumberFormatException e) {
                System.out.println("Ingresá un número entero.");
            }
        }
    }

    private String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return scanner.nextLine();
    }
}
