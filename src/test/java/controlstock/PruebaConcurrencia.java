package controlstock;

import controlstock.dao.CategoriaDao;
import controlstock.dao.DepositoDao;
import controlstock.dao.MovimientoStockDao;
import controlstock.dao.ProductoDao;
import controlstock.dao.StockDao;
import controlstock.dao.jdbc.CategoriaJdbcDao;
import controlstock.dao.jdbc.DepositoJdbcDao;
import controlstock.dao.jdbc.MovimientoStockJdbcDao;
import controlstock.dao.jdbc.ProductoJdbcDao;
import controlstock.dao.jdbc.StockJdbcDao;
import controlstock.modelo.Categoria;
import controlstock.modelo.Deposito;
import controlstock.modelo.Producto;
import controlstock.servicio.CategoriaServicio;
import controlstock.servicio.DepositoServicio;
import controlstock.servicio.ProductoServicio;
import controlstock.servicio.ReglaNegocioException;
import controlstock.servicio.StockServicio;
import controlstock.util.ConexionDB;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Pruebas de concurrencia del módulo Stock (sección 5 de plan_modulo_stock.md,
 * sección 8 de plan_mvp.md). Corre contra la base configurada en db.properties.
 *
 * Cada caso lanza dos operaciones en dos hilos. Para que se crucen de verdad
 * (y no corran una después de la otra por casualidad), una tercera conexión,
 * el "tapón", toma antes con FOR UPDATE la fila que las dos operaciones
 * bloquean primero. Se comprueba que las dos quedaron esperando y recién
 * entonces se suelta el tapón: las dos compiten por la fila al mismo tiempo.
 *
 * No es un test JUnit a propósito: necesita la base levantada y no tiene que
 * correr en "mvn package". Se ejecuta a mano desde la raíz del proyecto
 * (ver la sección 5 de docs/plan_modulo_stock.md).
 *
 * La cuenta de la aplicación no puede borrar, así que los datos que crea
 * (prefijo CONC-) quedan en la base; se limpian reinstalando con 00 → 03.
 */
public class PruebaConcurrencia {

    /** Cuánto se espera para que los dos hilos lleguen a bloquearse en el tapón. */
    private static final long ESPERA_BLOQUEO_MS = 1000;

    /** Menor que innodb_lock_wait_timeout (50 s por defecto). */
    private static final long TIMEOUT_HILO_S = 30;

    /** Rondas del caso 3; se alterna qué operación se lanza primero. */
    private static final int RONDAS_CASO_3 = 4;

    private final CategoriaServicio categoriaServicio;
    private final ProductoServicio productoServicio;
    private final DepositoServicio depositoServicio;
    private final StockServicio stockServicio;

    private final String sufijo = String.valueOf(System.currentTimeMillis() % 100_000_000L);
    private final ExecutorService hilos = Executors.newFixedThreadPool(2);

    private int idUsuario;
    private int idCategoria;
    private int contadorProductos = 0;
    private int fallas = 0;

    public static void main(String[] args) throws Exception {
        ConexionDB.inicializar();
        PruebaConcurrencia prueba = new PruebaConcurrencia();
        try {
            prueba.ejecutar();
        } finally {
            prueba.hilos.shutdownNow();
        }
        System.exit(prueba.fallas == 0 ? 0 : 1);
    }

    private PruebaConcurrencia() {
        CategoriaDao categoriaDao = new CategoriaJdbcDao();
        ProductoDao productoDao = new ProductoJdbcDao();
        DepositoDao depositoDao = new DepositoJdbcDao();
        StockDao stockDao = new StockJdbcDao();
        MovimientoStockDao movimientoStockDao = new MovimientoStockJdbcDao();

        categoriaServicio = new CategoriaServicio(categoriaDao, productoDao);
        productoServicio = new ProductoServicio(productoDao, categoriaDao, stockDao);
        depositoServicio = new DepositoServicio(depositoDao, stockDao);
        stockServicio = new StockServicio(stockDao, movimientoStockDao, productoDao, depositoDao);
    }

    private void ejecutar() throws Exception {
        idUsuario = consultarEntero(
                "SELECT idUsuario FROM USUARIO WHERE nombreUsuario = 'admin'");
        Categoria categoria = categoriaServicio.crear("CONC-" + sufijo);
        idCategoria = categoria.getIdCategoria();

        // Depósito propio para los casos 1 y 3, así no se toca el saldo de otros datos.
        Deposito depositoStock = depositoServicio.crear("CONC-" + sufijo + " stock", null);
        int idDeposito = depositoStock.getIdDeposito();

        System.out.println("Prueba de concurrencia, sufijo de datos: CONC-" + sufijo);
        System.out.println();

        caso1AjustesNegativosSimultaneos(idDeposito);
        caso3AjusteContraBajaDeProducto(idDeposito);
        caso2BajasDeDepositoSimultaneas();

        System.out.println();
        System.out.println(fallas == 0
                ? "RESULTADO: todas las comprobaciones pasaron."
                : "RESULTADO: " + fallas + " comprobación(es) fallaron.");
    }

    // Caso 1

    /** Saldo 3 y dos ajustes de −2 a la vez: uno pasa (saldo 1), el otro se rechaza. */
    private void caso1AjustesNegativosSimultaneos(int idDeposito) throws Exception {
        System.out.println("== Caso 1: saldo 3, dos ajustes simultáneos de -2 ==");
        int idProducto = crearProductoHabilitado(idDeposito);
        stockServicio.ajustarPositivo(idProducto, idDeposito, 3, "carga inicial caso 1", idUsuario);
        int movimientosAntes = contarMovimientos(idProducto, idDeposito);

        Resultado[] r = enParalelo(
                tapon("SELECT idProducto FROM PRODUCTO WHERE idProducto = ? FOR UPDATE", idProducto),
                () -> stockServicio.ajustarNegativo(idProducto, idDeposito, 2, "caso 1 hilo A", idUsuario),
                () -> stockServicio.ajustarNegativo(idProducto, idDeposito, 2, "caso 1 hilo B", idUsuario));

        imprimir("hilo A", r[0]);
        imprimir("hilo B", r[1]);

        int saldo = consultarSaldo(idProducto, idDeposito);
        int movimientosNuevos = contarMovimientos(idProducto, idDeposito) - movimientosAntes;

        comprobar(r[0].esExito() != r[1].esExito(), "exactamente un ajuste pasa");
        Resultado rechazo = r[0].esExito() ? r[1] : r[0];
        comprobar(rechazo.esRechazo("Stock insuficiente. Saldo actual: 1"),
                "el rechazo informa saldo 1 (leyó el saldo después del commit del otro)");
        comprobar(saldo == 1, "saldo final 1 (es " + saldo + ")");
        comprobar(movimientosNuevos == 1,
                "un solo movimiento nuevo (hay " + movimientosNuevos + ")");
        System.out.println();
    }

    // Caso 3

    /**
     * Ajuste positivo y baja del mismo producto (saldo 0) a la vez. Gane quien
     * gane, nunca puede quedar un producto inactivo con existencias.
     */
    private void caso3AjusteContraBajaDeProducto(int idDeposito) throws Exception {
        System.out.println("== Caso 3: ajuste +5 y baja del mismo producto al mismo tiempo ==");
        for (int ronda = 1; ronda <= RONDAS_CASO_3; ronda++) {
            int idProducto = crearProductoHabilitado(idDeposito);
            boolean ajustePrimero = ronda % 2 == 1;
            String motivo = "caso 3 ronda " + ronda;

            Accion ajuste = () -> stockServicio.ajustarPositivo(
                    idProducto, idDeposito, 5, motivo, idUsuario);
            Accion baja = () -> productoServicio.darDeBaja(idProducto);

            System.out.println("ronda " + ronda + " (se lanza primero: "
                    + (ajustePrimero ? "ajuste" : "baja") + ")");
            Resultado[] r = enParalelo(
                    tapon("SELECT idProducto FROM PRODUCTO WHERE idProducto = ? FOR UPDATE", idProducto),
                    ajustePrimero ? ajuste : baja,
                    ajustePrimero ? baja : ajuste);
            Resultado rAjuste = ajustePrimero ? r[0] : r[1];
            Resultado rBaja = ajustePrimero ? r[1] : r[0];

            String estado = consultarTexto(
                    "SELECT estadoProducto FROM PRODUCTO WHERE idProducto = ?", idProducto);
            int saldo = consultarSaldo(idProducto, idDeposito);

            imprimir("ajuste", rAjuste);
            imprimir("baja  ", rBaja);
            System.out.println("    final: producto " + estado + ", saldo " + saldo);

            comprobar(rAjuste.esExito() != rBaja.esExito(), "exactamente una operación pasa");
            if (rAjuste.esExito()) {
                comprobar(rBaja.esRechazo("tiene stock con cantidad mayor a 0"),
                        "la baja ve el saldo 5 del ajuste y rechaza");
            } else if (rBaja.esExito()) {
                comprobar(rAjuste.esRechazo("está inactivo"),
                        "el ajuste ve el producto inactivo y rechaza");
            }
            comprobar(!("inactivo".equals(estado) && saldo > 0),
                    "nunca queda un producto inactivo con existencias");
        }
        System.out.println();
    }

    // Caso 2

    /**
     * Con exactamente dos depósitos activos, dos bajas de depósitos distintos a
     * la vez: una pasa, la otra se rechaza y queda un depósito activo.
     *
     * Para tener exactamente dos activos sin tocar los datos existentes, se crean
     * dos depósitos nuevos y los demás activos se pasan a inactivo con un UPDATE
     * directo; se restauran al final pase lo que pase.
     */
    private void caso2BajasDeDepositoSimultaneas() throws Exception {
        System.out.println("== Caso 2: dos depósitos activos, dos bajas simultáneas ==");
        int idA = depositoServicio.crear("CONC-" + sufijo + " A", null).getIdDeposito();
        int idB = depositoServicio.crear("CONC-" + sufijo + " B", null).getIdDeposito();

        List<Integer> apartados = listarEnteros(
                "SELECT idDeposito FROM DEPOSITO WHERE estadoDeposito = 'activo' "
                + "AND idDeposito NOT IN (?, ?)", idA, idB);
        System.out.println("    se desactivan temporalmente los depósitos " + apartados
                + " (si el programa se corta, volver a activarlos a mano)");
        try {
            cambiarEstado(apartados, "inactivo");
            int activosAntes = consultarEntero(
                    "SELECT COUNT(*) FROM DEPOSITO WHERE estadoDeposito = 'activo'");
            comprobar(activosAntes == 2, "precondición: 2 depósitos activos (hay " + activosAntes + ")");

            Resultado[] r = enParalelo(
                    tapon("SELECT idDeposito FROM DEPOSITO WHERE idDeposito IN (?, ?) FOR UPDATE",
                            idA, idB),
                    () -> depositoServicio.darDeBaja(idA),
                    () -> depositoServicio.darDeBaja(idB));

            imprimir("baja A", r[0]);
            imprimir("baja B", r[1]);

            int activosNuevos = consultarEntero(
                    "SELECT COUNT(*) FROM DEPOSITO WHERE estadoDeposito = 'activo' "
                    + "AND idDeposito IN (?, ?)", idA, idB);

            comprobar(r[0].esExito() != r[1].esExito(), "exactamente una baja pasa");
            Resultado rechazo = r[0].esExito() ? r[1] : r[0];
            comprobar(rechazo.esRechazo("debe quedar al menos un depósito activo"),
                    "la otra se rechaza por ser el último activo");
            comprobar(activosNuevos == 1, "queda 1 depósito activo (quedan " + activosNuevos + ")");
        } finally {
            cambiarEstado(apartados, "activo");
            System.out.println("    depósitos " + apartados + " restaurados a activo");
        }
        System.out.println();
    }

    // Ejecución en paralelo

    /** Una operación del servicio que puede terminar con un rechazo de negocio. */
    @FunctionalInterface
    private interface Accion {
        void ejecutar() throws ReglaNegocioException;
    }

    /**
     * Lanza las dos acciones con el tapón tomado, verifica que las dos quedaron
     * esperando, suelta el tapón y espera los resultados.
     */
    private Resultado[] enParalelo(Connection tapon, Accion a, Accion b) throws Exception {
        Future<Resultado> fa;
        Future<Resultado> fb;
        try (tapon) {
            fa = hilos.submit(() -> correr(a));
            fb = hilos.submit(() -> correr(b));
            Thread.sleep(ESPERA_BLOQUEO_MS);
            comprobar(!fa.isDone() && !fb.isDone(),
                    "las dos operaciones quedaron esperando el bloqueo a la vez");
            tapon.commit(); // se suelta la fila: las dos compiten por ella
        }
        return new Resultado[] {
                fa.get(TIMEOUT_HILO_S, TimeUnit.SECONDS),
                fb.get(TIMEOUT_HILO_S, TimeUnit.SECONDS)
        };
    }

    private Resultado correr(Accion accion) {
        try {
            accion.ejecutar();
            return Resultado.exito();
        } catch (ReglaNegocioException e) {
            return Resultado.rechazo(e.getMessage());
        } catch (Throwable t) {
            return Resultado.error(t.toString());
        }
    }

    /** Abre una conexión, bloquea las filas indicadas y la devuelve sin confirmar. */
    private Connection tapon(String sql, int... ids) throws SQLException {
        Connection conn = ConexionDB.obtenerConexion();
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = preparar(conn, sql, ids);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // solo interesa el bloqueo
                }
            }
            return conn;
        } catch (SQLException e) {
            conn.close();
            throw e;
        }
    }

    /** Cómo terminó una operación: éxito, rechazo de negocio o error inesperado. */
    private record Resultado(String tipo, String mensaje) {
        static Resultado exito() {
            return new Resultado("OK", "");
        }

        static Resultado rechazo(String mensaje) {
            return new Resultado("RECHAZO", mensaje);
        }

        static Resultado error(String mensaje) {
            return new Resultado("ERROR", mensaje);
        }

        boolean esExito() {
            return tipo.equals("OK");
        }

        boolean esRechazo(String fragmento) {
            return tipo.equals("RECHAZO") && mensaje.contains(fragmento);
        }

        @Override
        public String toString() {
            return mensaje.isEmpty() ? tipo : tipo + ": " + mensaje;
        }
    }

    // Datos de prueba

    private int crearProductoHabilitado(int idDeposito) throws ReglaNegocioException {
        contadorProductos++;
        String codigo = "CONC-" + sufijo + "-" + contadorProductos;
        Producto producto = productoServicio.crear(codigo, idCategoria,
                "Producto " + codigo, null, BigDecimal.ONE, BigDecimal.TEN);
        stockServicio.habilitar(producto.getIdProducto(), idDeposito);
        return producto.getIdProducto();
    }

    private void cambiarEstado(List<Integer> idsDeposito, String estado) throws SQLException {
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE DEPOSITO SET estadoDeposito = ? WHERE idDeposito = ?")) {
            for (int id : idsDeposito) {
                ps.setString(1, estado);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
        }
    }

    // Consultas de verificación (cada una con conexión nueva: ve lo confirmado)

    private int consultarSaldo(int idProducto, int idDeposito) throws SQLException {
        return consultarEntero(
                "SELECT cantidad FROM STOCK WHERE idProducto = ? AND idDeposito = ?",
                idProducto, idDeposito);
    }

    private int contarMovimientos(int idProducto, int idDeposito) throws SQLException {
        return consultarEntero(
                "SELECT COUNT(*) FROM MOVIMIENTO_STOCK WHERE idProducto = ? AND idDeposito = ?",
                idProducto, idDeposito);
    }

    private int consultarEntero(String sql, int... ids) throws SQLException {
        return Integer.parseInt(consultarTexto(sql, ids));
    }

    private String consultarTexto(String sql, int... ids) throws SQLException {
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = preparar(conn, sql, ids);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new SQLException("La consulta no devolvió filas: " + sql);
            }
            return rs.getString(1);
        }
    }

    private List<Integer> listarEnteros(String sql, int... ids) throws SQLException {
        List<Integer> resultado = new ArrayList<>();
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = preparar(conn, sql, ids);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(rs.getInt(1));
            }
        }
        return resultado;
    }

    private PreparedStatement preparar(Connection conn, String sql, int... ids)
            throws SQLException {
        PreparedStatement ps = conn.prepareStatement(sql);
        for (int i = 0; i < ids.length; i++) {
            ps.setInt(i + 1, ids[i]);
        }
        return ps;
    }

    // Salida

    private void imprimir(String etiqueta, Resultado resultado) {
        System.out.println("    " + etiqueta + " -> " + resultado);
    }

    private void comprobar(boolean condicion, String descripcion) {
        System.out.println("  [" + (condicion ? "OK" : "FALLA") + "] " + descripcion);
        if (!condicion) {
            fallas++;
        }
    }
}
