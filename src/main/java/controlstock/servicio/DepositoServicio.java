package controlstock.servicio;

import controlstock.dao.DepositoDao;
import controlstock.dao.StockDao;
import controlstock.modelo.Deposito;
import controlstock.util.ConexionDB;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class DepositoServicio {

    private static final int LARGO_MAXIMO_NOMBRE = 50;
    private static final int LARGO_MAXIMO_DIRECCION = 150;

    private final DepositoDao depositoDao;
    private final StockDao stockDao;

    public DepositoServicio(DepositoDao depositoDao, StockDao stockDao) {
        this.depositoDao = depositoDao;
        this.stockDao = stockDao;
    }

    public Deposito crear(String nombre, String direccion) throws ReglaNegocioException {
        String nombreLimpio = validarNombre(nombre);
        String direccionLimpia = validarDireccion(direccion);
        Deposito deposito = new Deposito(nombreLimpio, direccionLimpia);

        try (Connection conn = ConexionDB.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                depositoDao.insertar(conn, deposito);
                conn.commit();
                return deposito;
            } catch (SQLException | RuntimeException e) {
                deshacer(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e);
        }
    }

    public Deposito modificar(int id, String nuevoNombre, String nuevaDireccion)
            throws ReglaNegocioException {
        String nombreLimpio = validarNombre(nuevoNombre);
        String direccionLimpia = validarDireccion(nuevaDireccion);

        try (Connection conn = ConexionDB.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Deposito deposito = depositoDao.buscarPorIdConBloqueo(conn, id);
                if (deposito == null) {
                    throw new ReglaNegocioException(
                            "No existe el depósito con id " + id + ".");
                }
                deposito.setNombreDeposito(nombreLimpio);
                deposito.setDireccion(direccionLimpia);
                depositoDao.actualizar(conn, deposito);
                conn.commit();
                return deposito;
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                deshacer(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e);
        }
    }

    public void darDeBaja(int id) throws ReglaNegocioException {
        int intentos = 0;
        while (true) {
            try (Connection conn = ConexionDB.obtenerConexion()) {
                conn.setAutoCommit(false);
                try {
                    Deposito deposito = depositoDao.buscarPorIdConBloqueo(conn, id);
                    if (deposito == null) {
                        throw new ReglaNegocioException(
                                "No existe el depósito con id " + id + ".");
                    }
                    if (!deposito.estaActivo()) {
                        throw new ReglaNegocioException(
                                "El depósito '" + deposito.getNombre() + "' ya está inactivo.");
                    }
                    if (stockDao.existeConCantidadPositivaPorDeposito(conn, id)) {
                        throw new ReglaNegocioException(
                                "No se puede dar de baja: tiene stock con cantidad mayor a 0.");
                    }
                    if (depositoDao.contarActivos(conn) < 2) {
                        throw new ReglaNegocioException(
                                "No se puede dar de baja: debe quedar al menos un depósito activo.");
                    }
                    deposito.darDeBaja();
                    depositoDao.actualizar(conn, deposito);
                    conn.commit();
                    return;
                } catch (SQLException e) {
                    deshacer(conn, e);
                    if (ReglaNegocioException.esDeadlock(e) && intentos < 1) {
                        intentos++;
                        continue;
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

    public List<Deposito> listarActivos() throws ReglaNegocioException {
        try (Connection conn = ConexionDB.obtenerConexion()) {
            return depositoDao.listarActivos(conn);
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e);
        }
    }

    public List<Deposito> listarTodos() throws ReglaNegocioException {
        try (Connection conn = ConexionDB.obtenerConexion()) {
            return depositoDao.listarTodos(conn);
        } catch (SQLException e) {
            throw ReglaNegocioException.desdeSQL(e);
        }
    }

    private String validarNombre(String nombre) throws ReglaNegocioException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ReglaNegocioException("El nombre del depósito es obligatorio.");
        }
        String limpio = nombre.trim();
        if (limpio.length() > LARGO_MAXIMO_NOMBRE) {
            throw new ReglaNegocioException(
                    "El nombre no puede superar los " + LARGO_MAXIMO_NOMBRE + " caracteres.");
        }
        return limpio;
    }

    private String validarDireccion(String direccion) throws ReglaNegocioException {
        if (direccion == null || direccion.trim().isEmpty()) {
            return null;
        }
        String limpia = direccion.trim();
        if (limpia.length() > LARGO_MAXIMO_DIRECCION) {
            throw new ReglaNegocioException(
                    "La dirección no puede superar los " + LARGO_MAXIMO_DIRECCION + " caracteres.");
        }
        return limpia;
    }

    private void deshacer(Connection conn, Exception original) {
        try {
            conn.rollback();
        } catch (SQLException e) {
            original.addSuppressed(e);
        }
    }
}
