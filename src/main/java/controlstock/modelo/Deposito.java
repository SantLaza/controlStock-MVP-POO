package controlstock.modelo;

public class Deposito {

    private int idDeposito;
    private String nombreDeposito;
    private String direccion;
    private Estado estadoDeposito;

    public Deposito() {
    }

    public Deposito(String nombreDeposito, String direccion) {
        this.nombreDeposito = nombreDeposito;
        this.direccion = direccion;
        this.estadoDeposito = Estado.ACTIVO;
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

    public Estado getEstadoDeposito() {
        return estadoDeposito;
    }

    public void setEstadoDeposito(Estado estadoDeposito) {
        this.estadoDeposito = estadoDeposito;
    }

    @Override
    public String toString() {
        return "[" + idDeposito + "] " + nombreDeposito
                + (direccion != null ? " — " + direccion : "")
                + " (" + estadoDeposito.getValor() + ")";
    }
}
