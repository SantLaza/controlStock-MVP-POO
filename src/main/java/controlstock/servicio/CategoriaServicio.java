package controlstock.servicio;

import controlstock.dao.CategoriaDao;
import controlstock.dao.ProductoDao;
import controlstock.modelo.Categoria;
import controlstock.util.ConexionDB;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class CategoriaServicio {

    private static final int LARGO_MAXIMO_NOMBRE = 50;

    private final CategoriaDao categoriaDao;
    private final ProductoDao productoDao;

    public CategoriaServicio(CategoriaDao categoriaDao, ProductoDao productoDao) {
        this.categoriaDao = categoriaDao;
        this.productoDao = productoDao;
    }

    public Categoria crear(String nombre) throws ReglaNegocioException {
        String nombreLimpio = validarNombre(nombre);
        Categoria categoria = new Categoria(nombreLimpio);

        try (Connection conn = ConexionDB.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                categoriaDao.insertar(conn, categoria);
                conn.commit();
                return categoria;
            } catch (SQLException | RuntimeException e) {
                deshacer(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e, ReglaNegocioException.mensajeDuplicado(
                    "una categoría", "el nombre", nombreLimpio));
        }
    }

    public Categoria modificar(int id, String nuevoNombre) throws ReglaNegocioException {
        String nombreLimpio = validarNombre(nuevoNombre);

        try (Connection conn = ConexionDB.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Categoria categoria = categoriaDao.buscarPorIdConBloqueo(conn, id);
                if (categoria == null) {
                    throw new ReglaNegocioException(
                            "No existe la categoría con id " + id + ".");
                }
                categoria.setNombreCategoria(nombreLimpio);
                categoriaDao.actualizar(conn, categoria);
                conn.commit();
                return categoria;
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                deshacer(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e, ReglaNegocioException.mensajeDuplicado(
                    "una categoría", "el nombre", nombreLimpio));
        }
    }

    public void darDeBaja(int id) throws ReglaNegocioException {
        try (Connection conn = ConexionDB.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Categoria categoria = categoriaDao.buscarPorIdConBloqueo(conn, id);
                if (categoria == null) {
                    throw new ReglaNegocioException(
                            "No existe la categoría con id " + id + ".");
                }
                if (!categoria.estaActivo()) {
                    throw new ReglaNegocioException(
                            "La categoría '" + categoria.getNombre() + "' ya está inactiva.");
                }
                if (productoDao.existeActivoPorCategoria(conn, id)) {
                    throw new ReglaNegocioException(
                            "No se puede dar de baja: tiene productos activos asignados.");
                }
                categoria.darDeBaja();
                categoriaDao.actualizar(conn, categoria);
                conn.commit();
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                deshacer(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e);
        }
    }

    public List<Categoria> listarActivas() throws ReglaNegocioException {
        try (Connection conn = ConexionDB.obtenerConexion()) {
            return categoriaDao.listarActivas(conn);
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e);
        }
    }

    public List<Categoria> listarTodas() throws ReglaNegocioException {
        try (Connection conn = ConexionDB.obtenerConexion()) {
            return categoriaDao.listarTodas(conn);
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e);
        }
    }

    private String validarNombre(String nombre) throws ReglaNegocioException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ReglaNegocioException("El nombre de la categoría es obligatorio.");
        }
        String limpio = nombre.trim();
        if (limpio.length() > LARGO_MAXIMO_NOMBRE) {
            throw new ReglaNegocioException(
                    "El nombre no puede superar los " + LARGO_MAXIMO_NOMBRE + " caracteres.");
        }
        return limpio;
    }

    private void deshacer(Connection conn, Exception original) {
        try {
            conn.rollback();
        } catch (SQLException e) {
            original.addSuppressed(e);
        }
    }
}
