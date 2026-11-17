package controlstock.servicio;

import controlstock.dao.DepositoDao;
import controlstock.dao.ProductoDao;
import controlstock.dao.StockDao;
import controlstock.modelo.Deposito;
import controlstock.modelo.Producto;
import controlstock.modelo.Stock;
import controlstock.util.ConexionDB;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas de stock (solo lectura): stock de un depósito, stock de un
 * producto con su total y listado de stock bajo.
 *
 * Solo se muestran productos y depósitos activos: incluir los inactivos en
 * las consultas quedó fuera del alcance del MVP (plan_mvp.md, sección 3).
 * Sin ese filtro, un producto dado de baja con mínimo mayor a 0 aparecería
 * para siempre en el listado de stock bajo.
 *
 * Los atributos derivados (stock total, stock bajo, faltante) no se guardan
 * en la base: el total se suma acá y el resto lo calcula {@link Stock}.
 */
public class ConsultaServicio {

    private final StockDao stockDao;
    private final ProductoDao productoDao;
    private final DepositoDao depositoDao;

    public ConsultaServicio(StockDao stockDao, ProductoDao productoDao,
                            DepositoDao depositoDao) {
        this.stockDao = stockDao;
        this.productoDao = productoDao;
        this.depositoDao = depositoDao;
    }

    /**
     * Stock de todos los productos (activos) habilitados en un depósito,
     * ordenado por id de producto.
     */
    public List<LineaStock> stockPorDeposito(int idDeposito) throws ReglaNegocioException {
        try (Connection conn = ConexionDB.obtenerConexion()) {
            Deposito deposito = depositoDao.buscarPorId(conn, idDeposito);
            if (deposito == null) {
                throw new ReglaNegocioException("No existe el depósito con id " + idDeposito + ".");
            }
            if (!deposito.estaActivo()) {
                throw new ReglaNegocioException(
                        "El depósito '" + deposito.getNombre() + "' está inactivo.");
            }

            Map<Integer, Producto> productos = new HashMap<>();
            List<LineaStock> lineas = new ArrayList<>();
            for (Stock stock : stockDao.listarPorDeposito(conn, idDeposito)) {
                Producto producto = buscarProducto(conn, productos, stock.getIdProducto());
                if (producto.estaActivo()) {
                    lineas.add(new LineaStock(producto, deposito, stock));
                }
            }
            return lineas;
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e);
        }
    }

    /**
     * Stock de un producto en cada depósito (activo) donde está habilitado,
     * ordenado por id de depósito. El total se obtiene con {@link #total(List)}.
     */
    public List<LineaStock> stockPorProducto(int idProducto) throws ReglaNegocioException {
        try (Connection conn = ConexionDB.obtenerConexion()) {
            Producto producto = productoDao.buscarPorId(conn, idProducto);
            if (producto == null) {
                throw new ReglaNegocioException("No existe el producto con id " + idProducto + ".");
            }
            if (!producto.estaActivo()) {
                throw new ReglaNegocioException(
                        "El producto '" + producto.getNombre() + "' está inactivo.");
            }

            Map<Integer, Deposito> depositos = new HashMap<>();
            List<LineaStock> lineas = new ArrayList<>();
            for (Stock stock : stockDao.listarPorProducto(conn, idProducto)) {
                Deposito deposito = buscarDeposito(conn, depositos, stock.getIdDeposito());
                if (deposito.estaActivo()) {
                    lineas.add(new LineaStock(producto, deposito, stock));
                }
            }
            return lineas;
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e);
        }
    }

    /**
     * Pares producto–depósito (los dos activos) con cantidad menor al mínimo,
     * ordenados por producto y depósito.
     */
    public List<LineaStock> stockBajo() throws ReglaNegocioException {
        try (Connection conn = ConexionDB.obtenerConexion()) {
            Map<Integer, Producto> productos = new HashMap<>();
            Map<Integer, Deposito> depositos = new HashMap<>();
            List<LineaStock> lineas = new ArrayList<>();
            for (Stock stock : stockDao.listarStockBajo(conn)) {
                Producto producto = buscarProducto(conn, productos, stock.getIdProducto());
                Deposito deposito = buscarDeposito(conn, depositos, stock.getIdDeposito());
                if (producto.estaActivo() && deposito.estaActivo()) {
                    lineas.add(new LineaStock(producto, deposito, stock));
                }
            }
            return lineas;
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e);
        }
    }

    /**
     * Stock total: suma de las cantidades de las líneas. Se suma en long
     * porque cada saldo puede llegar al máximo de un int.
     */
    public long total(List<LineaStock> lineas) {
        long total = 0;
        for (LineaStock linea : lineas) {
            total += linea.getStock().getCantidad();
        }
        return total;
    }

    // Auxiliares privados

    /**
     * Busca un producto por id, recordando los ya leídos para no repetir la
     * consulta. La clave foránea de STOCK garantiza que exista.
     */
    private Producto buscarProducto(Connection conn, Map<Integer, Producto> leidos, int id)
            throws SQLException {
        Producto producto = leidos.get(id);
        if (producto == null) {
            producto = productoDao.buscarPorId(conn, id);
            leidos.put(id, producto);
        }
        return producto;
    }

    /** Igual que {@link #buscarProducto}, para depósitos. */
    private Deposito buscarDeposito(Connection conn, Map<Integer, Deposito> leidos, int id)
            throws SQLException {
        Deposito deposito = leidos.get(id);
        if (deposito == null) {
            deposito = depositoDao.buscarPorId(conn, id);
            leidos.put(id, deposito);
        }
        return deposito;
    }
}
