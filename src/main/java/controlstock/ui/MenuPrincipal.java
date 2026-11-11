package controlstock.ui;

import controlstock.servicio.ReglaNegocioException;
import controlstock.servicio.SesionServicio;
import controlstock.modelo.Usuario;

import java.util.Scanner;

public class MenuPrincipal {

    private final Scanner scanner;
    private final SesionServicio sesionServicio;
    private final MenuMaestros menuMaestros;
    private final MenuStock menuStock;
    private final MenuConsultas menuConsultas;

    public MenuPrincipal(Scanner scanner, SesionServicio sesionServicio,
                         MenuMaestros menuMaestros, MenuStock menuStock,
                         MenuConsultas menuConsultas) {
        this.scanner = scanner;
        this.sesionServicio = sesionServicio;
        this.menuMaestros = menuMaestros;
        this.menuStock = menuStock;
        this.menuConsultas = menuConsultas;
    }

    public void ejecutar() {
        System.out.println("=== controlStock MVP ===\n");

        while (true) {
            if (!sesionServicio.haySesionActiva()) {
                if (!iniciarSesion()) {
                    break;
                }
            }

            mostrarMenu();
            int opcion = leerOpcion(0, 4);

            switch (opcion) {
                case 1 -> menuMaestros.ejecutar();
                case 2 -> menuStock.ejecutar();
                case 3 -> menuConsultas.ejecutar();
                case 4 -> {
                    sesionServicio.cerrarSesion();
                    System.out.println("Sesión cerrada.\n");
                }
                case 0 -> {
                    sesionServicio.cerrarSesion();
                    System.out.println("¡Hasta luego!");
                    return;
                }
            }
        }
    }

    private boolean iniciarSesion() {
        System.out.println("--- Inicio de sesión ---");
        System.out.println("(Escribí 'salir' como usuario para cerrar la aplicación)\n");

        while (true) {
            System.out.print("Usuario: ");
            String nombre = scanner.nextLine().trim();

            if (nombre.equalsIgnoreCase("salir")) {
                System.out.println("¡Hasta luego!");
                return false;
            }

            System.out.print("Contraseña: ");
            String contrasena = scanner.nextLine();

            try {
                Usuario usuario = sesionServicio.iniciarSesion(nombre, contrasena);
                System.out.println("\n¡Bienvenido, " + usuario.getNombreUsuario()
                        + " (" + usuario.getRol().getValor() + ")!\n");
                return true;
            } catch (ReglaNegocioException e) {
                System.out.println("Error: " + e.getMessage() + "\n");
            }
        }
    }

    private void mostrarMenu() {
        System.out.println("--- Menú Principal ---");
        System.out.println("1. Maestros (Categorías, Productos, Depósitos)");
        System.out.println("2. Stock (Habilitación, Mínimos, Ajustes)");
        System.out.println("3. Consultas");
        System.out.println("4. Cerrar sesión");
        System.out.println("0. Salir");
    }

    private int leerOpcion(int min, int max) {
        while (true) {
            System.out.print("Opción: ");
            String linea = scanner.nextLine().trim();
            try {
                int opcion = Integer.parseInt(linea);
                if (opcion >= min && opcion <= max) {
                    System.out.println();
                    return opcion;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Opción no válida. Ingresá un número entre "
                    + min + " y " + max + ".");
        }
    }
}
