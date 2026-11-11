package controlstock;

import controlstock.dao.UsuarioDao;
import controlstock.dao.jdbc.UsuarioJdbcDao;
import controlstock.servicio.SesionServicio;
import controlstock.ui.MenuConsultas;
import controlstock.ui.MenuMaestros;
import controlstock.ui.MenuPrincipal;
import controlstock.ui.MenuStock;
import controlstock.util.ConexionDB;

import java.sql.SQLException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try {
            ConexionDB.inicializar();
        } catch (SQLException e) {
            System.err.println("No se pudo conectar a la base de datos: " + e.getMessage());
            System.exit(1);
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        Scanner scanner = new Scanner(System.in);

        UsuarioDao usuarioDao = new UsuarioJdbcDao();
        SesionServicio sesionServicio = new SesionServicio(usuarioDao);

        MenuMaestros menuMaestros = new MenuMaestros(scanner);
        MenuStock menuStock = new MenuStock(scanner);
        MenuConsultas menuConsultas = new MenuConsultas(scanner);

        MenuPrincipal menuPrincipal = new MenuPrincipal(
                scanner, sesionServicio, menuMaestros, menuStock, menuConsultas);

        menuPrincipal.ejecutar();

        scanner.close();
    }
}
