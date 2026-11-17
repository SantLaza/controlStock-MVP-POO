package controlstock.ui;

import controlstock.modelo.Deposito;
import controlstock.modelo.Producto;
import controlstock.modelo.Stock;
import controlstock.servicio.ConsultaServicio;
import controlstock.servicio.DepositoServicio;
import controlstock.servicio.LineaStock;
import controlstock.servicio.ProductoServicio;
import controlstock.servicio.ReglaNegocioException;

import java.util.List;
import java.util.Scanner;

/**
 * Menú de consultas: stock de un depósito, stock de un producto con su total
 * y listado de stock bajo.
 *
 * Es solo capa de interfaz: pide los datos, llama a {@link ConsultaServicio}
 * y arma las tablas. Para que el usuario sepa qué id elegir, antes de pedirlo
 * muestra la lista de depósitos o productos activos (servicios de Maestros).
 */
public class MenuConsultas {

    /** Ancho de las columnas de nombres en las tablas. */
    private static final int ANCHO_NOMBRE = 28;

    /** Ancho mínimo de la columna Código; se ensancha si hay códigos más largos. */
    private static final int ANCHO_CODIGO_MINIMO = 12;

    private final Scanner scanner;
    private final ConsultaServicio consultaServicio;
    private final ProductoServicio productoServicio;
    private final DepositoServicio depositoServicio;

    public MenuConsultas(Scanner scanner, ConsultaServicio consultaServicio,
                         ProductoServicio productoServicio,
                         DepositoServicio depositoServicio) {
        this.scanner = scanner;
        this.consultaServicio = consultaServicio;
        this.productoServicio = productoServicio;
        this.depositoServicio = depositoServicio;
    }

    public void ejecutar() {
        while (true) {
            mostrarMenu();
            int opcion = leerOpcion(0, 3);

            if (opcion == 0) {
                return; // vuelve al menú principal
            }

            try {
                switch (opcion) {
                    case 1 -> stockPorDeposito();
                    case 2 -> stockPorProducto();
                    case 3 -> stockBajo();
                }
            } catch (ReglaNegocioException e) {
                System.out.println("Error: " + e.getMessage());
            }
            System.out.println();
        }
    }

    private void mostrarMenu() {
        System.out.println("--- Consultas ---");
        System.out.println("1. Stock por depósito");
        System.out.println("2. Stock total de un producto");
        System.out.println("3. Productos con stock bajo");
        System.out.println("0. Volver");
    }

    // Opciones

    private void stockPorDeposito() throws ReglaNegocioException {
        System.out.println("-- Stock por depósito --");
        List<Deposito> activos = depositoServicio.listarActivos();
        if (activos.isEmpty()) {
            System.out.println("No hay depósitos activos.");
            return;
        }
        System.out.println("Depósitos activos:");
        for (Deposito d : activos) {
            System.out.println("  " + d);
        }
        int idDeposito = leerEntero("Id del depósito: ");

        List<LineaStock> lineas = consultaServicio.stockPorDeposito(idDeposito);
        System.out.println();
        if (lineas.isEmpty()) {
            System.out.println("No hay productos activos habilitados en ese depósito.");
            return;
        }

        int anchoCodigo = anchoCodigo(lineas);
        System.out.println("Stock en " + lineas.get(0).getDeposito().getNombre() + ":");
        System.out.printf("  %-5s %-" + anchoCodigo + "s %-" + ANCHO_NOMBRE + "s %8s %8s %9s%n",
                "Id", "Código", "Producto", "Saldo", "Mínimo", "Faltante");
        for (LineaStock linea : lineas) {
            Producto p = linea.getProducto();
            Stock s = linea.getStock();
            System.out.printf("  %-5d %-" + anchoCodigo + "s %-" + ANCHO_NOMBRE
                            + "s %8d %8d %9d%s%n",
                    p.getIdProducto(), p.getCodigoProducto(), recortar(p.getNombre()),
                    s.getCantidad(), s.getStockMinimo(), s.faltante(), marcaBajo(s));
        }
    }

    private void stockPorProducto() throws ReglaNegocioException {
        System.out.println("-- Stock total de un producto --");
        List<Producto> activos = productoServicio.listarActivos();
        if (activos.isEmpty()) {
            System.out.println("No hay productos activos.");
            return;
        }
        System.out.println("Productos activos:");
        for (Producto p : activos) {
            System.out.println("  ID " + p.getIdProducto() + ": " + p);
        }
        int idProducto = leerEntero("Id del producto: ");

        List<LineaStock> lineas = consultaServicio.stockPorProducto(idProducto);
        System.out.println();
        if (lineas.isEmpty()) {
            System.out.println("El producto no está habilitado en ningún depósito activo. "
                    + "Stock total: 0.");
            return;
        }

        Producto producto = lineas.get(0).getProducto();
        System.out.println("Stock de [" + producto.getCodigoProducto() + "] "
                + producto.getNombre() + ":");
        System.out.printf("  %-5s %-" + ANCHO_NOMBRE + "s %8s %8s %9s%n",
                "Id", "Depósito", "Saldo", "Mínimo", "Faltante");
        for (LineaStock linea : lineas) {
            Deposito d = linea.getDeposito();
            Stock s = linea.getStock();
            System.out.printf("  %-5d %-" + ANCHO_NOMBRE + "s %8d %8d %9d%s%n",
                    d.getIdDeposito(), recortar(d.getNombre()),
                    s.getCantidad(), s.getStockMinimo(), s.faltante(), marcaBajo(s));
        }
        System.out.println("  Stock total: " + consultaServicio.total(lineas));
    }

    private void stockBajo() throws ReglaNegocioException {
        System.out.println("-- Productos con stock bajo --");
        List<LineaStock> lineas = consultaServicio.stockBajo();
        if (lineas.isEmpty()) {
            System.out.println("No hay productos con stock bajo.");
            return;
        }

        int anchoCodigo = anchoCodigo(lineas);
        System.out.printf("  %-" + anchoCodigo + "s %-" + ANCHO_NOMBRE + "s %-" + ANCHO_NOMBRE
                        + "s %8s %8s %9s%n",
                "Código", "Producto", "Depósito", "Saldo", "Mínimo", "Faltante");
        for (LineaStock linea : lineas) {
            Producto p = linea.getProducto();
            Stock s = linea.getStock();
            System.out.printf("  %-" + anchoCodigo + "s %-" + ANCHO_NOMBRE + "s %-" + ANCHO_NOMBRE
                            + "s %8d %8d %9d%n",
                    p.getCodigoProducto(), recortar(p.getNombre()),
                    recortar(linea.getDeposito().getNombre()),
                    s.getCantidad(), s.getStockMinimo(), s.faltante());
        }
    }

    // Auxiliares

    /** Marca las filas con stock bajo en las tablas por depósito y por producto. */
    private String marcaBajo(Stock stock) {
        return stock.esBajo() ? "  BAJO" : "";
    }

    /**
     * Ancho de la columna Código: el del código más largo de la tabla, con un
     * mínimo de {@link #ANCHO_CODIGO_MINIMO}. Los códigos no se recortan porque
     * identifican al producto; la base los limita a 30 caracteres.
     */
    private int anchoCodigo(List<LineaStock> lineas) {
        int ancho = ANCHO_CODIGO_MINIMO;
        for (LineaStock linea : lineas) {
            ancho = Math.max(ancho, linea.getProducto().getCodigoProducto().length());
        }
        return ancho;
    }

    /** Corta los nombres largos para que no se desarmen las columnas. */
    private String recortar(String texto) {
        if (texto.length() <= ANCHO_NOMBRE) {
            return texto;
        }
        return texto.substring(0, ANCHO_NOMBRE - 3) + "...";
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

    /** Pide un número entero y vuelve a preguntar hasta que lo sea. */
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
}
