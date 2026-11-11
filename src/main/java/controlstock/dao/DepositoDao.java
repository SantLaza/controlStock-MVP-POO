package controlstock.dao;

import controlstock.modelo.Deposito;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface DepositoDao {

    void insertar(Connection conn, Deposito deposito) throws SQLException;

    void actualizar(Connection conn, Deposito deposito) throws SQLException;

    Deposito buscarPorId(Connection conn, int idDeposito) throws SQLException;

    Deposito buscarPorIdConBloqueo(Connection conn, int idDeposito) throws SQLException;

    List<Deposito> listarActivos(Connection conn) throws SQLException;

    List<Deposito> listarTodos(Connection conn) throws SQLException;

    int contarActivos(Connection conn) throws SQLException;
}
