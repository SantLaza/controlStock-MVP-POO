package controlstock.ui;

import controlstock.modelo.Categoria;
import controlstock.modelo.Deposito;
import controlstock.modelo.Producto;
import controlstock.servicio.CategoriaServicio;
import controlstock.servicio.DepositoServicio;
import controlstock.servicio.ProductoServicio;
import controlstock.servicio.ReglaNegocioException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class MenuMaestros {

    /** Al modificar, escribir esto en un campo opcional lo deja vacío (NULL). */
    private static final String MARCA_BORRAR = "-";

    private final Scanner scanner;
    private final CategoriaServicio categoriaServicio;
    private final ProductoServicio productoServicio;
    private final DepositoServicio depositoServicio;

    public MenuMaestros(Scanner scanner, CategoriaServicio categoriaServicio,
                        ProductoServicio productoServicio,
                        DepositoServicio depositoServicio) {
        this.scanner = scanner;
        this.categoriaServicio = categoriaServicio;
        this.productoServicio = productoServicio;
        this.depositoServicio = depositoServicio;
    }

    public void ejecutar() {
        while (true) {
            System.out.println("--- Maestros ---");
            System.out.println("1. Categorías");
            System.out.println("2. Productos");
            System.out.println("3. Depósitos");
            System.out.println("0. Volver");
            int opcion = leerOpcion(0, 3);
            switch (opcion) {
                case 1 -> menuCategorias();
                case 2 -> menuProductos();
                case 3 -> menuDepositos();
                case 0 -> { return; }
            }
        }
    }

    // ====================== Categorías ======================

    private void menuCategorias() {
        while (true) {
            System.out.println("--- Categorías ---");
            System.out.println("1. Alta");
            System.out.println("2. Modificar");
            System.out.println("3. Listar");
            System.out.println("4. Dar de baja");
            System.out.println("0. Volver");
            int opcion = leerOpcion(0, 4);
            switch (opcion) {
                case 1 -> altaCategoria();
                case 2 -> modificarCategoria();
                case 3 -> listarCategorias();
                case 4 -> bajaCategoria();
                case 0 -> { return; }
            }
        }
    }

    private void altaCategoria() {
        System.out.println("--- Alta de categoría ---");
        String nombre = leerPrimerCampoDeAlta("Nombre");
        if (nombre == null) {
            System.out.println("Alta cancelada.\n");
            return;
        }
        try {
            Categoria creada = categoriaServicio.crear(nombre);
            System.out.println("Categoría creada: " + creada + "\n");
        } catch (ReglaNegocioException e) {
            System.out.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void modificarCategoria() {
        System.out.println("--- Modificar categoría ---");
        try {
            List<Categoria> todas = categoriaServicio.listarTodas();
            if (todas.isEmpty()) {
                System.out.println("No hay categorías registradas.\n");
                return;
            }
            System.out.println("Categorías existentes:");
            for (Categoria c : todas) {
                System.out.println("  " + c);
            }
            System.out.println();

            int id = leerEntero("ID de la categoría a modificar: ");
            Categoria actual = buscarCategoria(todas, id);
            if (actual == null) {
                System.out.println("No se encontró una categoría con ese ID.\n");
                return;
            }

            System.out.println("Nombre actual: " + actual.getNombreCategoria());
            String nuevoNombre = leerTextoConActual("Nuevo nombre",
                    actual.getNombreCategoria());

            Categoria modificada = categoriaServicio.modificar(id, nuevoNombre);
            System.out.println("Categoría modificada: " + modificada + "\n");
        } catch (ReglaNegocioException e) {
            System.out.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void listarCategorias() {
        System.out.println("--- Categorías ---");
        try {
            List<Categoria> todas = categoriaServicio.listarTodas();
            if (todas.isEmpty()) {
                System.out.println("No hay categorías registradas.");
            } else {
                for (Categoria c : todas) {
                    System.out.println("  " + c);
                }
            }
            System.out.println();
        } catch (ReglaNegocioException e) {
            System.out.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void bajaCategoria() {
        System.out.println("--- Baja de categoría ---");
        try {
            List<Categoria> activas = categoriaServicio.listarActivas();
            if (activas.isEmpty()) {
                System.out.println("No hay categorías activas.\n");
                return;
            }
            System.out.println("Categorías activas:");
            for (Categoria c : activas) {
                System.out.println("  " + c);
            }
            System.out.println();

            int id = leerEntero("ID de la categoría a dar de baja: ");
            categoriaServicio.darDeBaja(id);
            System.out.println("Categoría dada de baja.\n");
        } catch (ReglaNegocioException e) {
            System.out.println("Error: " + e.getMessage() + "\n");
        }
    }

    // ====================== Productos ======================

    private void menuProductos() {
        while (true) {
            System.out.println("--- Productos ---");
            System.out.println("1. Alta");
            System.out.println("2. Modificar");
            System.out.println("3. Listar");
            System.out.println("4. Dar de baja");
            System.out.println("0. Volver");
            int opcion = leerOpcion(0, 4);
            switch (opcion) {
                case 1 -> altaProducto();
                case 2 -> modificarProducto();
                case 3 -> listarProductos();
                case 4 -> bajaProducto();
                case 0 -> { return; }
            }
        }
    }

    private void altaProducto() {
        System.out.println("--- Alta de producto ---");
        try {
            List<Categoria> activas = categoriaServicio.listarActivas();
            if (activas.isEmpty()) {
                System.out.println("No hay categorías activas. Creá una categoría primero.\n");
                return;
            }
            System.out.println("Categorías activas:");
            for (Categoria c : activas) {
                System.out.println("  " + c);
            }
            System.out.println();

            String codigo = leerPrimerCampoDeAlta("Código");
            if (codigo == null) {
                System.out.println("Alta cancelada.\n");
                return;
            }
            int idCategoria = leerCategoriaActiva(activas);
            if (idCategoria == 0) {
                System.out.println("Alta cancelada.\n");
                return;
            }
            String nombre = leerTextoObligatorio("Nombre: ");
            String descripcion = leerTextoOpcional("Descripción (enter para omitir): ");
            BigDecimal precioCompra = leerBigDecimal("Precio de compra: ");
            BigDecimal precioVenta = leerBigDecimal("Precio de venta: ");

            Producto creado = productoServicio.crear(codigo, idCategoria, nombre,
                    descripcion, precioCompra, precioVenta);
            System.out.println("Producto creado: " + creado + "\n");
        } catch (ReglaNegocioException e) {
            System.out.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void modificarProducto() {
        System.out.println("--- Modificar producto ---");
        try {
            List<Producto> todos = productoServicio.listarTodos();
            if (todos.isEmpty()) {
                System.out.println("No hay productos registrados.\n");
                return;
            }
            System.out.println("Productos existentes:");
            for (Producto p : todos) {
                System.out.println("  ID " + p.getIdProducto() + ": " + p);
            }
            System.out.println();

            int id = leerEntero("ID del producto a modificar: ");
            Producto actual = buscarProducto(todos, id);
            if (actual == null) {
                System.out.println("No se encontró un producto con ese ID.\n");
                return;
            }

            System.out.println("\nValores actuales:");
            System.out.println("  Código: " + actual.getCodigoProducto());
            System.out.println("  Categoría (ID): " + actual.getIdCategoria());
            System.out.println("  Nombre: " + actual.getNombreProducto());
            System.out.println("  Descripción: "
                    + (actual.getDescripcion() != null
                       ? actual.getDescripcion() : "(sin descripción)"));
            System.out.println("  Precio compra: " + actual.getPrecioReferenciaCompra());
            System.out.println("  Precio venta: " + actual.getPrecioReferenciaVenta());

            List<Categoria> catActivas = categoriaServicio.listarActivas();
            if (!catActivas.isEmpty()) {
                System.out.println("\nCategorías activas:");
                for (Categoria c : catActivas) {
                    System.out.println("  " + c);
                }
            }

            System.out.println("\nNuevos valores (enter para mantener el actual):");
            String codigo = leerTextoConActual("Código", actual.getCodigoProducto());
            int idCategoria = leerCategoriaConActual(actual.getIdCategoria(), catActivas);
            String nombre = leerTextoConActual("Nombre", actual.getNombreProducto());

            String descripcion = leerOpcionalConActual("Descripción",
                    actual.getDescripcion(), "sin descripción");

            BigDecimal precioCompra = leerBigDecimalConActual("Precio compra",
                    actual.getPrecioReferenciaCompra());
            BigDecimal precioVenta = leerBigDecimalConActual("Precio venta",
                    actual.getPrecioReferenciaVenta());

            Producto modificado = productoServicio.modificar(id, codigo, idCategoria,
                    nombre, descripcion, precioCompra, precioVenta);
            System.out.println("Producto modificado: " + modificado + "\n");
        } catch (ReglaNegocioException e) {
            System.out.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void listarProductos() {
        System.out.println("--- Productos ---");
        try {
            List<Producto> todos = productoServicio.listarTodos();
            if (todos.isEmpty()) {
                System.out.println("No hay productos registrados.");
            } else {
                for (Producto p : todos) {
                    System.out.println("  ID " + p.getIdProducto() + ": " + p
                            + " | Cat: " + p.getIdCategoria()
                            + " | $" + p.getPrecioReferenciaCompra()
                            + " / $" + p.getPrecioReferenciaVenta());
                }
            }
            System.out.println();
        } catch (ReglaNegocioException e) {
            System.out.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void bajaProducto() {
        System.out.println("--- Baja de producto ---");
        try {
            List<Producto> activos = productoServicio.listarActivos();
            if (activos.isEmpty()) {
                System.out.println("No hay productos activos.\n");
                return;
            }
            System.out.println("Productos activos:");
            for (Producto p : activos) {
                System.out.println("  ID " + p.getIdProducto() + ": " + p);
            }
            System.out.println();

            int id = leerEntero("ID del producto a dar de baja: ");
            productoServicio.darDeBaja(id);
            System.out.println("Producto dado de baja.\n");
        } catch (ReglaNegocioException e) {
            System.out.println("Error: " + e.getMessage() + "\n");
        }
    }

    // ====================== Depósitos ======================

    private void menuDepositos() {
        while (true) {
            System.out.println("--- Depósitos ---");
            System.out.println("1. Alta");
            System.out.println("2. Modificar");
            System.out.println("3. Listar");
            System.out.println("4. Dar de baja");
            System.out.println("0. Volver");
            int opcion = leerOpcion(0, 4);
            switch (opcion) {
                case 1 -> altaDeposito();
                case 2 -> modificarDeposito();
                case 3 -> listarDepositos();
                case 4 -> bajaDeposito();
                case 0 -> { return; }
            }
        }
    }

    private void altaDeposito() {
        System.out.println("--- Alta de depósito ---");
        String nombre = leerPrimerCampoDeAlta("Nombre");
        if (nombre == null) {
            System.out.println("Alta cancelada.\n");
            return;
        }
        String direccion = leerTextoOpcional("Dirección (enter para omitir): ");
        try {
            Deposito creado = depositoServicio.crear(nombre, direccion);
            System.out.println("Depósito creado: " + creado + "\n");
        } catch (ReglaNegocioException e) {
            System.out.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void modificarDeposito() {
        System.out.println("--- Modificar depósito ---");
        try {
            List<Deposito> todos = depositoServicio.listarTodos();
            if (todos.isEmpty()) {
                System.out.println("No hay depósitos registrados.\n");
                return;
            }
            System.out.println("Depósitos existentes:");
            for (Deposito d : todos) {
                System.out.println("  " + d);
            }
            System.out.println();

            int id = leerEntero("ID del depósito a modificar: ");
            Deposito actual = buscarDeposito(todos, id);
            if (actual == null) {
                System.out.println("No se encontró un depósito con ese ID.\n");
                return;
            }

            System.out.println("Valores actuales:");
            System.out.println("  Nombre: " + actual.getNombreDeposito());
            System.out.println("  Dirección: "
                    + (actual.getDireccion() != null
                       ? actual.getDireccion() : "(sin dirección)"));

            System.out.println("\nNuevos valores (enter para mantener el actual):");
            String nombre = leerTextoConActual("Nombre", actual.getNombreDeposito());

            String direccion = leerOpcionalConActual("Dirección",
                    actual.getDireccion(), "sin dirección");

            Deposito modificado = depositoServicio.modificar(id, nombre, direccion);
            System.out.println("Depósito modificado: " + modificado + "\n");
        } catch (ReglaNegocioException e) {
            System.out.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void listarDepositos() {
        System.out.println("--- Depósitos ---");
        try {
            List<Deposito> todos = depositoServicio.listarTodos();
            if (todos.isEmpty()) {
                System.out.println("No hay depósitos registrados.");
            } else {
                for (Deposito d : todos) {
                    System.out.println("  " + d);
                }
            }
            System.out.println();
        } catch (ReglaNegocioException e) {
            System.out.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void bajaDeposito() {
        System.out.println("--- Baja de depósito ---");
        try {
            List<Deposito> activos = depositoServicio.listarActivos();
            if (activos.isEmpty()) {
                System.out.println("No hay depósitos activos.\n");
                return;
            }
            System.out.println("Depósitos activos:");
            for (Deposito d : activos) {
                System.out.println("  " + d);
            }
            System.out.println();

            int id = leerEntero("ID del depósito a dar de baja: ");
            depositoServicio.darDeBaja(id);
            System.out.println("Depósito dado de baja.\n");
        } catch (ReglaNegocioException e) {
            System.out.println("Error: " + e.getMessage() + "\n");
        }
    }

    // ====================== Búsqueda en listas ======================

    private Categoria buscarCategoria(List<Categoria> lista, int id) {
        for (Categoria c : lista) {
            if (c.getIdCategoria() == id) return c;
        }
        return null;
    }

    private Producto buscarProducto(List<Producto> lista, int id) {
        for (Producto p : lista) {
            if (p.getIdProducto() == id) return p;
        }
        return null;
    }

    private Deposito buscarDeposito(List<Deposito> lista, int id) {
        for (Deposito d : lista) {
            if (d.getIdDeposito() == id) return d;
        }
        return null;
    }

    // ====================== Lectura de datos ======================

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

    private int leerEntero(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = scanner.nextLine().trim();
            try {
                return Integer.parseInt(linea);
            } catch (NumberFormatException e) {
                System.out.println("Ingresá un número entero válido.");
            }
        }
    }

    private int leerEnteroConActual(String campo, int actual) {
        while (true) {
            System.out.print(campo + " [" + actual + "]: ");
            String linea = scanner.nextLine().trim();
            if (linea.isEmpty()) return actual;
            try {
                return Integer.parseInt(linea);
            } catch (NumberFormatException e) {
                System.out.println("Ingresá un número entero válido.");
            }
        }
    }

    /**
     * Alta de producto: pide el id hasta que sea de una categoría activa de la
     * lista mostrada, así el error no aparece recién después de cargar todo.
     * Devuelve 0 si el usuario cancela. El servicio lo vuelve a validar.
     */
    private int leerCategoriaActiva(List<Categoria> activas) {
        while (true) {
            int id = leerEntero("Categoría (ID, 0 para cancelar): ");
            if (id == 0 || buscarCategoria(activas, id) != null) {
                return id;
            }
            System.out.println("No hay una categoría activa con ese ID.");
        }
    }

    /**
     * Modificación de producto: enter conserva la categoría actual, aunque esté
     * inactiva (regla 9 de modelo_relacional_mvp.md §5.3). Una categoría nueva
     * tiene que ser una de las activas.
     */
    private int leerCategoriaConActual(int actual, List<Categoria> activas) {
        while (true) {
            int id = leerEnteroConActual("Categoría (ID)", actual);
            if (id == actual || buscarCategoria(activas, id) != null) {
                return id;
            }
            System.out.println("No hay una categoría activa con ese ID.");
        }
    }

    private BigDecimal leerBigDecimal(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = scanner.nextLine().trim();
            try {
                BigDecimal valor = new BigDecimal(linea);
                if (valor.compareTo(BigDecimal.ZERO) >= 0) return valor;
                System.out.println("El valor no puede ser negativo.");
            } catch (NumberFormatException e) {
                System.out.println("Ingresá un número válido (usá punto como separador decimal).");
            }
        }
    }

    private BigDecimal leerBigDecimalConActual(String campo, BigDecimal actual) {
        while (true) {
            System.out.print(campo + " [" + actual + "]: ");
            String linea = scanner.nextLine().trim();
            if (linea.isEmpty()) return actual;
            try {
                BigDecimal valor = new BigDecimal(linea);
                if (valor.compareTo(BigDecimal.ZERO) >= 0) return valor;
                System.out.println("El valor no puede ser negativo.");
            } catch (NumberFormatException e) {
                System.out.println("Ingresá un número válido (usá punto como separador decimal).");
            }
        }
    }

    private String leerTextoObligatorio(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = scanner.nextLine().trim();
            if (!linea.isEmpty()) return linea;
            System.out.println("Este campo es obligatorio.");
        }
    }

    /**
     * Primer campo de un alta: devuelve null si el usuario cancela con enter
     * vacío o con 0 (el mismo número que "Volver" en los menús).
     */
    private String leerPrimerCampoDeAlta(String campo) {
        System.out.print(campo + " (enter o 0 para cancelar): ");
        String linea = scanner.nextLine().trim();
        return (linea.isEmpty() || linea.equals("0")) ? null : linea;
    }

    private String leerTextoOpcional(String mensaje) {
        System.out.print(mensaje);
        String linea = scanner.nextLine().trim();
        return linea.isEmpty() ? null : linea;
    }

    /**
     * Campo opcional al modificar (descripción, dirección): enter conserva el
     * valor actual, {@link #MARCA_BORRAR} lo borra y cualquier otro texto lo
     * reemplaza.
     */
    private String leerOpcionalConActual(String campo, String actual, String textoSinValor) {
        System.out.print(campo + " [" + (actual != null ? actual : textoSinValor)
                + "] (enter mantiene, " + MARCA_BORRAR + " borra): ");
        String linea = scanner.nextLine().trim();
        if (linea.isEmpty()) {
            return actual;
        }
        return linea.equals(MARCA_BORRAR) ? null : linea;
    }

    private String leerTextoConActual(String campo, String actual) {
        System.out.print(campo + " [" + actual + "]: ");
        String linea = scanner.nextLine().trim();
        return linea.isEmpty() ? actual : linea;
    }
}
