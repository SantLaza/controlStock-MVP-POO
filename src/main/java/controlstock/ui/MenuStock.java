package controlstock.ui;

import controlstock.modelo.Deposito;
import controlstock.modelo.Producto;
import controlstock.modelo.Stock;
import controlstock.servicio.ConsultaServicio;
import controlstock.servicio.DepositoServicio;
import controlstock.servicio.LineaStock;
import controlstock.servicio.ProductoServicio;
import controlstock.servicio.ReglaNegocioException;
import controlstock.servicio.SesionServicio;
import controlstock.servicio.StockServicio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * Menú del módulo Stock: habilitar un producto en un depósito, definir su
 * stock mínimo y registrar ajustes positivos y negativos.
 *
 * Es solo capa de interfaz: pide los datos por consola, llama a
 * {@link StockServicio} y muestra el resultado o el mensaje de error.
 *
 * Para que el usuario no cargue todo y recién al final se entere de que un id
 * no sirve, antes de pedir cada id muestra las opciones válidas y valida la
 * elección en el momento: primero los productos activos y después los
 * depósitos que corresponden a la operación (donde el producto todavía no está
 * habilitado, o donde ya lo está, con su saldo y mínimo). Con 0 se cancela.
 *
 * Esa validación es solo de interfaz: las reglas de negocio las vuelve a
 * comprobar el servicio dentro de la transacción, con bloqueos, porque otra
 * sesión puede cambiar los datos entre que se muestran y se confirma.
 */
public class MenuStock {

    private final Scanner scanner;
    private final StockServicio stockServicio;
    private final SesionServicio sesionServicio;
    private final ProductoServicio productoServicio;
    private final DepositoServicio depositoServicio;
    private final ConsultaServicio consultaServicio;

    public MenuStock(Scanner scanner, StockServicio stockServicio,
                     SesionServicio sesionServicio, ProductoServicio productoServicio,
                     DepositoServicio depositoServicio, ConsultaServicio consultaServicio) {
        this.scanner = scanner;
        this.stockServicio = stockServicio;
        this.sesionServicio = sesionServicio;
        this.productoServicio = productoServicio;
        this.depositoServicio = depositoServicio;
        this.consultaServicio = consultaServicio;
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
        Producto producto = elegirProducto();
        if (producto == null) {
            return;
        }

        // Depósitos activos donde el producto todavía no está habilitado
        Set<Integer> habilitados = new HashSet<>();
        for (LineaStock linea : consultaServicio.stockPorProducto(producto.getIdProducto())) {
            habilitados.add(linea.getDeposito().getIdDeposito());
        }
        List<Deposito> disponibles = new ArrayList<>();
        for (Deposito d : depositoServicio.listarActivos()) {
            if (!habilitados.contains(d.getIdDeposito())) {
                disponibles.add(d);
            }
        }
        if (disponibles.isEmpty()) {
            System.out.println("El producto ya está habilitado en todos los depósitos activos.");
            return;
        }

        System.out.println("Depósitos activos donde todavía no está habilitado:");
        for (Deposito d : disponibles) {
            System.out.println("  [" + d.getIdDeposito() + "] " + d.getNombre());
        }
        Deposito deposito = elegirPorId("Id del depósito (0 para cancelar): ", disponibles,
                Deposito::getIdDeposito, "Ese depósito no está en la lista.");
        if (deposito == null) {
            System.out.println("Operación cancelada.");
            return;
        }

        stockServicio.habilitar(producto.getIdProducto(), deposito.getIdDeposito());
        System.out.println("Producto habilitado en el depósito (saldo 0, mínimo 0).");
    }

    private void definirMinimo() throws ReglaNegocioException {
        System.out.println("-- Definir stock mínimo --");
        LineaStock linea = elegirParHabilitado();
        if (linea == null) {
            return;
        }

        int nuevoMinimo = leerEnteroDesde("Nuevo stock mínimo: ", 0,
                "El stock mínimo no puede ser negativo.");

        Stock stock = stockServicio.definirMinimo(idProducto(linea), idDeposito(linea),
                nuevoMinimo);
        System.out.println("Mínimo actualizado a " + stock.getStockMinimo()
                + ". Saldo actual: " + stock.getCantidad() + ".");
        mostrarAvisoStockBajo(stock);
    }

    private void ajustarPositivo() throws ReglaNegocioException {
        System.out.println("-- Ajuste positivo --");
        LineaStock linea = elegirParHabilitado();
        if (linea == null) {
            return;
        }

        int cantidad = leerEnteroDesde("Cantidad a sumar: ", 1,
                "La cantidad del ajuste debe ser mayor a 0.");
        String motivo = leerTextoObligatorio("Motivo: ", "El motivo es obligatorio.");

        Stock stock = stockServicio.ajustarPositivo(idProducto(linea), idDeposito(linea),
                cantidad, motivo, idUsuarioActual());
        System.out.println("Ajuste registrado. Saldo actual: " + stock.getCantidad() + ".");
        mostrarAvisoStockBajo(stock);
    }

    private void ajustarNegativo() throws ReglaNegocioException {
        System.out.println("-- Ajuste negativo --");
        LineaStock linea = elegirParHabilitado();
        if (linea == null) {
            return;
        }
        if (linea.getStock().getCantidad() == 0) {
            System.out.println("El saldo es 0: no hay unidades para restar.");
            return;
        }

        // El saldo mostrado orienta al usuario; si otra sesión lo cambia antes de
        // confirmar, el servicio vuelve a comprobarlo con la fila bloqueada.
        int cantidad = leerEnteroDesde("Cantidad a restar: ", 1,
                "La cantidad del ajuste debe ser mayor a 0.");
        String motivo = leerTextoObligatorio("Motivo: ", "El motivo es obligatorio.");

        Stock stock = stockServicio.ajustarNegativo(idProducto(linea), idDeposito(linea),
                cantidad, motivo, idUsuarioActual());
        System.out.println("Ajuste registrado. Saldo actual: " + stock.getCantidad() + ".");
        mostrarAvisoStockBajo(stock);
    }

    // Elección de producto y depósito

    /** Muestra los productos activos y pide uno. Devuelve null si se cancela. */
    private Producto elegirProducto() throws ReglaNegocioException {
        List<Producto> activos = productoServicio.listarActivos();
        if (activos.isEmpty()) {
            System.out.println("No hay productos activos.");
            return null;
        }
        System.out.println("Productos activos:");
        for (Producto p : activos) {
            System.out.println("  ID " + p.getIdProducto() + ": " + p);
        }
        Producto elegido = elegirPorId("Id del producto (0 para cancelar): ", activos,
                Producto::getIdProducto, "No hay un producto activo con ese id.");
        if (elegido == null) {
            System.out.println("Operación cancelada.");
        }
        return elegido;
    }

    /**
     * Pide un producto y después uno de los depósitos activos donde está
     * habilitado, mostrando saldo y mínimo. Lo usan el mínimo y los ajustes.
     * Devuelve null si se cancela o si no hay dónde operar.
     */
    private LineaStock elegirParHabilitado() throws ReglaNegocioException {
        Producto producto = elegirProducto();
        if (producto == null) {
            return null;
        }

        List<LineaStock> lineas = consultaServicio.stockPorProducto(producto.getIdProducto());
        if (lineas.isEmpty()) {
            System.out.println("El producto no está habilitado en ningún depósito activo. "
                    + "Habilitalo primero (opción 1).");
            return null;
        }

        System.out.println("Depósitos donde está habilitado:");
        for (LineaStock linea : lineas) {
            Stock s = linea.getStock();
            System.out.println("  [" + linea.getDeposito().getIdDeposito() + "] "
                    + linea.getDeposito().getNombre()
                    + " — saldo " + s.getCantidad() + ", mínimo " + s.getStockMinimo());
        }
        LineaStock elegida = elegirPorId("Id del depósito (0 para cancelar): ", lineas,
                l -> l.getDeposito().getIdDeposito(), "Ese depósito no está en la lista.");
        if (elegida == null) {
            System.out.println("Operación cancelada.");
        }
        return elegida;
    }

    /**
     * Pide un id hasta que corresponda a una de las opciones mostradas.
     * Devuelve null si el usuario escribe 0 (los ids empiezan en 1).
     */
    private <T> T elegirPorId(String mensaje, List<T> opciones, ToIntFunction<T> id,
                              String noEncontrado) {
        while (true) {
            int elegido = leerEntero(mensaje);
            if (elegido == 0) {
                return null;
            }
            for (T opcion : opciones) {
                if (id.applyAsInt(opcion) == elegido) {
                    return opcion;
                }
            }
            System.out.println(noEncontrado);
        }
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

    private int idProducto(LineaStock linea) {
        return linea.getProducto().getIdProducto();
    }

    private int idDeposito(LineaStock linea) {
        return linea.getDeposito().getIdDeposito();
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

    /**
     * Pide un entero mayor o igual a {@code minimo} y vuelve a preguntar si no
     * lo es. Adelanta la validación del servicio para no pedir el motivo en vano.
     */
    private int leerEnteroDesde(String mensaje, int minimo, String error) {
        while (true) {
            int valor = leerEntero(mensaje);
            if (valor >= minimo) {
                return valor;
            }
            System.out.println(error);
        }
    }

    /** Pide un texto no vacío; el largo máximo lo valida el servicio. */
    private String leerTextoObligatorio(String mensaje, String error) {
        while (true) {
            System.out.print(mensaje);
            String linea = scanner.nextLine();
            if (!linea.trim().isEmpty()) {
                return linea;
            }
            System.out.println(error);
        }
    }
}
