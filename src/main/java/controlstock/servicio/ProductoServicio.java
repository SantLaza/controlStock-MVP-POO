package controlstock.servicio;

import controlstock.dao.CategoriaDao;
import controlstock.dao.ProductoDao;
import controlstock.dao.StockDao;
import controlstock.modelo.Categoria;
import controlstock.modelo.Producto;
import controlstock.util.ConexionDB;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ProductoServicio {

    private static final int LARGO_MAXIMO_CODIGO = 30;
    private static final int LARGO_MAXIMO_NOMBRE = 100;
    private static final int LARGO_MAXIMO_DESCRIPCION = 255;

    /** Columnas de precio: DECIMAL(12,2), es decir 10 dígitos enteros y 2 decimales. */
    private static final int DECIMALES_PRECIO = 2;
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("9999999999.99");

    private final ProductoDao productoDao;
    private final CategoriaDao categoriaDao;
    private final StockDao stockDao;

    public ProductoServicio(ProductoDao productoDao, CategoriaDao categoriaDao,
                            StockDao stockDao) {
        this.productoDao = productoDao;
        this.categoriaDao = categoriaDao;
        this.stockDao = stockDao;
    }

    public Producto crear(String codigo, int idCategoria, String nombre,
                          String descripcion, BigDecimal precioCompra, BigDecimal precioVenta)
            throws ReglaNegocioException {
        String codigoLimpio = validarCodigo(codigo);
        String nombreLimpio = validarNombre(nombre);
        String descripcionLimpia = validarDescripcion(descripcion);
        validarPrecio(precioCompra, "compra");
        validarPrecio(precioVenta, "venta");

        Producto producto = new Producto(codigoLimpio, idCategoria, nombreLimpio,
                precioCompra, precioVenta);
        producto.setDescripcion(descripcionLimpia);

        try (Connection conn = ConexionDB.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                verificarCategoriaActiva(conn, idCategoria);
                productoDao.insertar(conn, producto);
                conn.commit();
                return producto;
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                deshacer(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e, ReglaNegocioException.mensajeDuplicado(
                    "un producto", "el código", codigoLimpio));
        }
    }

    public Producto modificar(int id, String nuevoCodigo, int nuevaCategoria,
                              String nuevoNombre, String nuevaDescripcion,
                              BigDecimal nuevoPrecioCompra, BigDecimal nuevoPrecioVenta)
            throws ReglaNegocioException {
        String codigoLimpio = validarCodigo(nuevoCodigo);
        String nombreLimpio = validarNombre(nuevoNombre);
        String descripcionLimpia = validarDescripcion(nuevaDescripcion);
        validarPrecio(nuevoPrecioCompra, "compra");
        validarPrecio(nuevoPrecioVenta, "venta");

        try (Connection conn = ConexionDB.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                // Regla 9 (modelo_relacional_mvp.md §5.3): la categoría se valida solo si
                // cambia. Para saberlo se lee el producto SIN bloqueo; así, si hay que
                // bloquear la categoría, se bloquea antes que el producto (orden §5.4).
                Producto leido = productoDao.buscarPorId(conn, id);
                if (leido == null) {
                    throw new ReglaNegocioException(
                            "No existe el producto con id " + id + ".");
                }
                boolean cambiaCategoria = nuevaCategoria != leido.getIdCategoria();
                if (cambiaCategoria) {
                    verificarCategoriaActiva(conn, nuevaCategoria);
                }

                Producto producto = productoDao.buscarPorIdConBloqueo(conn, id);
                // Entre las dos lecturas otra sesión pudo cambiarle la categoría: si no
                // se validó, guardar la recibida la devolvería a una sin verificar.
                if (!cambiaCategoria && producto.getIdCategoria() != nuevaCategoria) {
                    throw new ReglaNegocioException(
                            "Otra sesión cambió la categoría del producto. Volvé a intentarlo.");
                }
                producto.setCodigoProducto(codigoLimpio);
                producto.setIdCategoria(nuevaCategoria);
                producto.setNombreProducto(nombreLimpio);
                producto.setDescripcion(descripcionLimpia);
                producto.setPrecioReferenciaCompra(nuevoPrecioCompra);
                producto.setPrecioReferenciaVenta(nuevoPrecioVenta);
                productoDao.actualizar(conn, producto);
                conn.commit();
                return producto;
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                deshacer(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e, ReglaNegocioException.mensajeDuplicado(
                    "un producto", "el código", codigoLimpio));
        }
    }

    public void darDeBaja(int id) throws ReglaNegocioException {
        try (Connection conn = ConexionDB.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Producto producto = productoDao.buscarPorIdConBloqueo(conn, id);
                if (producto == null) {
                    throw new ReglaNegocioException(
                            "No existe el producto con id " + id + ".");
                }
                if (!producto.estaActivo()) {
                    throw new ReglaNegocioException(
                            "El producto '" + producto.getNombre() + "' ya está inactivo.");
                }
                if (stockDao.existeConCantidadPositiva(conn, id)) {
                    throw new ReglaNegocioException(
                            "No se puede dar de baja: tiene stock con cantidad mayor a 0.");
                }
                producto.darDeBaja();
                productoDao.actualizar(conn, producto);
                conn.commit();
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                deshacer(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e);
        }
    }

    public List<Producto> listarActivos() throws ReglaNegocioException {
        try (Connection conn = ConexionDB.obtenerConexion()) {
            return productoDao.listarActivos(conn);
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e);
        }
    }

    public List<Producto> listarTodos() throws ReglaNegocioException {
        try (Connection conn = ConexionDB.obtenerConexion()) {
            return productoDao.listarTodos(conn);
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e);
        }
    }

    private void verificarCategoriaActiva(Connection conn, int idCategoria)
            throws SQLException, ReglaNegocioException {
        Categoria categoria = categoriaDao.buscarPorIdConBloqueo(conn, idCategoria);
        if (categoria == null) {
            throw new ReglaNegocioException(
                    "No existe la categoría con id " + idCategoria + ".");
        }
        if (!categoria.estaActivo()) {
            throw new ReglaNegocioException(
                    "La categoría '" + categoria.getNombre() + "' está inactiva.");
        }
    }

    private String validarCodigo(String codigo) throws ReglaNegocioException {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new ReglaNegocioException("El código del producto es obligatorio.");
        }
        String limpio = codigo.trim();
        if (limpio.length() > LARGO_MAXIMO_CODIGO) {
            throw new ReglaNegocioException(
                    "El código no puede superar los " + LARGO_MAXIMO_CODIGO + " caracteres.");
        }
        return limpio;
    }

    private String validarNombre(String nombre) throws ReglaNegocioException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ReglaNegocioException("El nombre del producto es obligatorio.");
        }
        String limpio = nombre.trim();
        if (limpio.length() > LARGO_MAXIMO_NOMBRE) {
            throw new ReglaNegocioException(
                    "El nombre no puede superar los " + LARGO_MAXIMO_NOMBRE + " caracteres.");
        }
        return limpio;
    }

    private String validarDescripcion(String descripcion) throws ReglaNegocioException {
        if (descripcion == null || descripcion.trim().isEmpty()) {
            return null;
        }
        String limpia = descripcion.trim();
        if (limpia.length() > LARGO_MAXIMO_DESCRIPCION) {
            throw new ReglaNegocioException(
                    "La descripción no puede superar los " + LARGO_MAXIMO_DESCRIPCION
                    + " caracteres.");
        }
        return limpia;
    }

    private void validarPrecio(BigDecimal precio, String tipo) throws ReglaNegocioException {
        if (precio == null) {
            throw new ReglaNegocioException(
                    "El precio de referencia de " + tipo + " es obligatorio.");
        }
        if (precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException(
                    "El precio de referencia de " + tipo + " no puede ser negativo.");
        }
        // DECIMAL(12,2) redondea sin avisar: se rechaza en lugar de guardar otro valor.
        // stripTrailingZeros acepta los valores exactos con ceros de más (1.500).
        if (precio.stripTrailingZeros().scale() > DECIMALES_PRECIO) {
            throw new ReglaNegocioException(
                    "El precio de referencia de " + tipo + " no puede tener más de "
                    + DECIMALES_PRECIO + " decimales.");
        }
        if (precio.compareTo(PRECIO_MAXIMO) > 0) {
            throw new ReglaNegocioException(
                    "El precio de referencia de " + tipo + " no puede superar "
                    + PRECIO_MAXIMO.toPlainString() + ".");
        }
    }

    private void deshacer(Connection conn, Exception original) {
        try {
            conn.rollback();
        } catch (SQLException e) {
            original.addSuppressed(e);
        }
    }
}
