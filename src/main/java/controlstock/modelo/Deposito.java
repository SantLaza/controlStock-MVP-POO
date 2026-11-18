package controlstock.modelo;

public class Deposito extends EntidadMaestra {

    private int idDeposito;
    private String nombreDeposito;
    private String direccion;

    public Deposito() {
    }

    public Deposito(String nombreDeposito, String direccion) {
        this.nombreDeposito = nombreDeposito;
        this.direccion = direccion;
    }

    public int getIdDeposito() {
        return idDeposito;
    }

    public void setIdDeposito(int idDeposito) {
        this.idDeposito = idDeposito;
    }

    public String getNombreDeposito() {
        return nombreDeposito;
    }

    public void setNombreDeposito(String nombreDeposito) {
        this.nombreDeposito = nombreDeposito;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    @Override
    public String getNombre() {
        return nombreDeposito;
    }

    @Override
    public String toString() {
        return "[" + idDeposito + "] " + nombreDeposito
                + (direccion != null ? " - " + direccion : "")
                + " (" + getEstado().getValor() + ")";
    }
}
