package controlstock;

import controlstock.dao.CategoriaDao;
import controlstock.dao.DepositoDao;
import controlstock.dao.MovimientoStockDao;
import controlstock.dao.ProductoDao;
import controlstock.dao.StockDao;
import controlstock.dao.UsuarioDao;
import controlstock.dao.jdbc.CategoriaJdbcDao;
import controlstock.dao.jdbc.DepositoJdbcDao;
import controlstock.dao.jdbc.MovimientoStockJdbcDao;
import controlstock.dao.jdbc.ProductoJdbcDao;
import controlstock.dao.jdbc.StockJdbcDao;
import controlstock.dao.jdbc.UsuarioJdbcDao;
import controlstock.servicio.CategoriaServicio;
import controlstock.servicio.ConsultaServicio;
import controlstock.servicio.DepositoServicio;
import controlstock.servicio.ProductoServicio;
import controlstock.servicio.SesionServicio;
import controlstock.servicio.StockServicio;
import controlstock.ui.MenuConsultas;
import controlstock.ui.MenuMaestros;
import controlstock.ui.MenuPrincipal;
import controlstock.ui.MenuStock;
import controlstock.util.ConexionDB;

import java.sql.SQLException;
import java.util.NoSuchElementException;
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
        MovimientoStockDao movimientoStockDao = new MovimientoStockJdbcDao();

        SesionServicio sesionServicio = new SesionServicio(usuarioDao);
        CategoriaServicio categoriaServicio = new CategoriaServicio(categoriaDao, productoDao);
        ProductoServicio productoServicio = new ProductoServicio(productoDao, categoriaDao, stockDao);
        DepositoServicio depositoServicio = new DepositoServicio(depositoDao, stockDao);
        StockServicio stockServicio = new StockServicio(
                stockDao, movimientoStockDao, productoDao, depositoDao);
        ConsultaServicio consultaServicio = new ConsultaServicio(
                stockDao, productoDao, depositoDao);

        MenuMaestros menuMaestros = new MenuMaestros(scanner, categoriaServicio,
                productoServicio, depositoServicio);
        MenuStock menuStock = new MenuStock(scanner, stockServicio, sesionServicio);
        MenuConsultas menuConsultas = new MenuConsultas(scanner, consultaServicio,
                productoServicio, depositoServicio);

        MenuPrincipal menuPrincipal = new MenuPrincipal(
                scanner, sesionServicio, menuMaestros, menuStock, menuConsultas);

        try {
            menuPrincipal.ejecutar();
        } catch (NoSuchElementException e) {
            // Se cerró la entrada estándar (Ctrl+Z / Ctrl+D) en cualquier menú.
            // No se pierde nada: cada operación ya confirmó o deshizo su transacción.
            System.out.println();
            System.out.println("Se cerró la entrada. ¡Hasta luego!");
        } finally {
            scanner.close();
        }
    }
}
