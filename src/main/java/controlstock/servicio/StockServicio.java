package controlstock.servicio;

import controlstock.dao.DepositoDao;
import controlstock.dao.MovimientoStockDao;
import controlstock.dao.ProductoDao;
import controlstock.dao.StockDao;
import controlstock.modelo.Deposito;
import controlstock.modelo.MovimientoStock;
import controlstock.modelo.Producto;
import controlstock.modelo.Stock;
import controlstock.modelo.TipoMovimiento;
import controlstock.util.ConexionDB;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Reglas de negocio del módulo Stock: habilitar un producto en un depósito,
 * definir su stock mínimo y registrar ajustes positivos y negativos.
 *
 * El servicio es quien maneja las transacciones: abre la conexión, apaga el
 * autocommit, llama a los DAOs (que solo ejecutan SQL) y decide si confirma
 * (commit) o deshace (rollback). Los bloqueos se toman siempre en el mismo
 * orden, PRODUCTO → DEPOSITO → STOCK, para evitar bloqueos mutuos.
 *
 * El servicio no escribe en consola: devuelve los datos o lanza
 * {@link ReglaNegocioException}, y la capa ui decide qué mostrar.
 */
public class StockServicio {

    /** Largo máximo del motivo (dominio Motivos: Cadena(255)). */
    private static final int LARGO_MAXIMO_MOTIVO = 255;

    /** Cuántas veces se reintenta un ajuste que falló por deadlock (error 1213). */
    private static final int MAX_REINTENTOS = 1;

    private final StockDao stockDao;
    private final MovimientoStockDao movimientoStockDao;
    private final ProductoDao productoDao;
    private final DepositoDao depositoDao;

    public StockServicio(StockDao stockDao, MovimientoStockDao movimientoStockDao,
                         ProductoDao productoDao, DepositoDao depositoDao) {
        this.stockDao = stockDao;
        this.movimientoStockDao = movimientoStockDao;
        this.productoDao = productoDao;
        this.depositoDao = depositoDao;
    }

    // Casos de uso

    /**
     * Habilita un producto en un depósito: crea su fila en STOCK con cantidad 0
     * y stock mínimo 0. El producto y el depósito tienen que existir y estar activos.
     */
    public void habilitar(int idProducto, int idDeposito) throws ReglaNegocioException {
        try (Connection conn = ConexionDB.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                bloquearProductoActivo(conn, idProducto);
                bloquearDepositoActivo(conn, idDeposito);

                if (stockDao.buscarPorIds(conn, idProducto, idDeposito) != null) {
                    throw new ReglaNegocioException(
                            "El producto ya está habilitado en ese depósito.");
                }

                stockDao.insertar(conn, new Stock(idProducto, idDeposito));
                conn.commit();
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                deshacer(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e);
        }
    }

    /**
     * Cambia el stock mínimo de un producto en un depósito.
     *
     * @return el Stock actualizado, para que el menú pueda avisar si quedó bajo
     */
    public Stock definirMinimo(int idProducto, int idDeposito, int nuevoMinimo)
            throws ReglaNegocioException {
        if (nuevoMinimo < 0) {
            throw new ReglaNegocioException("El stock mínimo no puede ser negativo.");
        }

        try (Connection conn = ConexionDB.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Stock stock = bloquearStockHabilitado(conn, idProducto, idDeposito);
                stock.definirMinimo(nuevoMinimo);
                stockDao.actualizar(conn, stock);
                conn.commit();
                return stock;
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                deshacer(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e);
        }
    }

    /**
     * Suma unidades al saldo y registra el movimiento AJUSTE_POSITIVO.
     *
     * @return el Stock actualizado, para que el menú pueda avisar si sigue bajo
     */
    public Stock ajustarPositivo(int idProducto, int idDeposito, int cantidad,
                                 String motivo, int idUsuario) throws ReglaNegocioException {
        return ajustar(TipoMovimiento.AJUSTE_POSITIVO,
                idProducto, idDeposito, cantidad, motivo, idUsuario);
    }

    /**
     * Resta unidades del saldo y registra el movimiento AJUSTE_NEGATIVO.
     * Se rechaza si el saldo no alcanza: el stock nunca queda negativo.
     *
     * @return el Stock actualizado, para que el menú pueda avisar si quedó bajo
     */
    public Stock ajustarNegativo(int idProducto, int idDeposito, int cantidad,
                                 String motivo, int idUsuario) throws ReglaNegocioException {
        return ajustar(TipoMovimiento.AJUSTE_NEGATIVO,
                idProducto, idDeposito, cantidad, motivo, idUsuario);
    }

    // Ajustes: transacción con reintento ante deadlock

    /**
     * Lógica común de los dos ajustes. Valida los datos, abre la transacción y,
     * si MySQL la cancela por deadlock (1213), la repite una vez desde cero.
     */
    private Stock ajustar(TipoMovimiento tipo, int idProducto, int idDeposito, int cantidad,
                          String motivo, int idUsuario) throws ReglaNegocioException {
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad del ajuste debe ser mayor a 0.");
        }
        String motivoLimpio = validarMotivo(motivo);

        int intentos = 0;
        while (true) {
            try (Connection conn = ConexionDB.obtenerConexion()) {
                conn.setAutoCommit(false);
                try {
                    Stock stock = aplicarAjuste(conn, tipo, idProducto, idDeposito,
                            cantidad, motivoLimpio, idUsuario);
                    conn.commit();
                    return stock;
                } catch (SQLException e) {
                    deshacer(conn, e);
                    if (ReglaNegocioException.esDeadlock(e) && intentos < MAX_REINTENTOS) {
                        intentos++;
                        continue; // se cierra esta conexión y se arranca de nuevo
                    }
                    throw e;
                } catch (ReglaNegocioException | RuntimeException e) {
                    deshacer(conn, e);
                    throw e;
                }
            } catch (SQLException e) {
                throw ReglaNegocioException.desdeSQL(e);
            }
        }
    }

    /**
     * Un intento completo de ajuste dentro de la transacción ya abierta.
     * Saldo y movimiento se escriben con la misma conexión: se confirman juntos
     * o se deshacen juntos.
     */
    private Stock aplicarAjuste(Connection conn, TipoMovimiento tipo, int idProducto,
                                int idDeposito, int cantidad, String motivo, int idUsuario)
            throws SQLException, ReglaNegocioException {
        // Orden de bloqueos: PRODUCTO → DEPOSITO → STOCK
        bloquearProductoActivo(conn, idProducto);
        bloquearDepositoActivo(conn, idDeposito);
        Stock stock = bloquearStockHabilitado(conn, idProducto, idDeposito);

        if (tipo == TipoMovimiento.AJUSTE_POSITIVO) {
            try {
                stock.aumentar(cantidad);
            } catch (IllegalArgumentException e) {
                throw new ReglaNegocioException(e.getMessage());
            }
        } else {
            if (stock.getCantidad() < cantidad) {
                throw new ReglaNegocioException(
                        "Stock insuficiente. Saldo actual: " + stock.getCantidad());
            }
            stock.disminuir(cantidad);
        }

        stockDao.actualizar(conn, stock);

        MovimientoStock movimiento = new MovimientoStock(
                idProducto, idDeposito, idUsuario, tipo, cantidad, motivo);
        movimientoStockDao.insertar(conn, movimiento);

        return stock;
    }

    // Auxiliares privados

    /** Bloquea la fila del producto y verifica que exista y esté activo. */
    private Producto bloquearProductoActivo(Connection conn, int idProducto)
            throws SQLException, ReglaNegocioException {
        Producto producto = productoDao.buscarPorIdConBloqueo(conn, idProducto);
        if (producto == null) {
            throw new ReglaNegocioException("No existe el producto con id " + idProducto + ".");
        }
        if (!producto.estaActivo()) {
            throw new ReglaNegocioException(
                    "El producto '" + producto.getNombre() + "' está inactivo.");
        }
        return producto;
    }

    /** Bloquea la fila del depósito y verifica que exista y esté activo. */
    private Deposito bloquearDepositoActivo(Connection conn, int idDeposito)
            throws SQLException, ReglaNegocioException {
        Deposito deposito = depositoDao.buscarPorIdConBloqueo(conn, idDeposito);
        if (deposito == null) {
            throw new ReglaNegocioException("No existe el depósito con id " + idDeposito + ".");
        }
        if (!deposito.estaActivo()) {
            throw new ReglaNegocioException(
                    "El depósito '" + deposito.getNombre() + "' está inactivo.");
        }
        return deposito;
    }

    /** Bloquea (FOR UPDATE) la fila de STOCK del par y verifica que exista. */
    private Stock bloquearStockHabilitado(Connection conn, int idProducto, int idDeposito)
            throws SQLException, ReglaNegocioException {
        Stock stock = stockDao.buscarPorIdsConBloqueo(conn, idProducto, idDeposito);
        if (stock == null) {
            throw new ReglaNegocioException(
                    "El producto no está habilitado en ese depósito.");
        }
        return stock;
    }

    /** Valida el motivo y lo devuelve sin espacios en los extremos. */
    private String validarMotivo(String motivo) throws ReglaNegocioException {
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new ReglaNegocioException("El motivo es obligatorio.");
        }
        String limpio = motivo.trim();
        if (limpio.length() > LARGO_MAXIMO_MOTIVO) {
            throw new ReglaNegocioException(
                    "El motivo no puede superar los " + LARGO_MAXIMO_MOTIVO + " caracteres.");
        }
        return limpio;
    }

    /**
     * Hace rollback sin tapar el error original: si el rollback también falla,
     * ese segundo error se adjunta al original como "suprimido".
     */
    private void deshacer(Connection conn, Exception original) {
        try {
            conn.rollback();
        } catch (SQLException e) {
            original.addSuppressed(e);
        }
    }
}
