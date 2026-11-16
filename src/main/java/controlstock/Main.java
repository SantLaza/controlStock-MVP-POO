package controlstock;

import controlstock.dao.CategoriaDao;
import controlstock.dao.DepositoDao;
import controlstock.dao.ProductoDao;
import controlstock.dao.StockDao;
import controlstock.dao.UsuarioDao;
import controlstock.dao.jdbc.CategoriaJdbcDao;
import controlstock.dao.jdbc.DepositoJdbcDao;
import controlstock.dao.jdbc.ProductoJdbcDao;
import controlstock.dao.jdbc.StockJdbcDao;
import controlstock.dao.jdbc.UsuarioJdbcDao;
import controlstock.servicio.CategoriaServicio;
import controlstock.servicio.DepositoServicio;
import controlstock.servicio.ProductoServicio;
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
        CategoriaDao categoriaDao = new CategoriaJdbcDao();
        ProductoDao productoDao = new ProductoJdbcDao();
        DepositoDao depositoDao = new DepositoJdbcDao();
        StockDao stockDao = new StockJdbcDao();

        SesionServicio sesionServicio = new SesionServicio(usuarioDao);
        CategoriaServicio categoriaServicio = new CategoriaServicio(categoriaDao, productoDao);
        ProductoServicio productoServicio = new ProductoServicio(productoDao, categoriaDao, stockDao);
        DepositoServicio depositoServicio = new DepositoServicio(depositoDao, stockDao);

        MenuMaestros menuMaestros = new MenuMaestros(scanner, categoriaServicio,
                productoServicio, depositoServicio);
        MenuStock menuStock = new MenuStock(scanner);
        MenuConsultas menuConsultas = new MenuConsultas(scanner);

        MenuPrincipal menuPrincipal = new MenuPrincipal(
                scanner, sesionServicio, menuMaestros, menuStock, menuConsultas);

        menuPrincipal.ejecutar();

        scanner.close();
    }
}
