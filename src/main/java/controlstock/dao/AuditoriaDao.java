package controlstock.dao;

import controlstock.modelo.Auditoria;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface AuditoriaDao {

    void insertar(Connection conn, Auditoria auditoria) throws SQLException;

    List<Auditoria> listarTodas(Connection conn) throws SQLException;
}
