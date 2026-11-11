package controlstock.modelo;

public class Categoria {

    private int idCategoria;
    private String nombreCategoria;
    private Estado estadoCategoria;

    public Categoria() {
    }

    public Categoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
        this.estadoCategoria = Estado.ACTIVO;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }

    public Estado getEstadoCategoria() {
        return estadoCategoria;
    }

    public void setEstadoCategoria(Estado estadoCategoria) {
        this.estadoCategoria = estadoCategoria;
    }

    @Override
    public String toString() {
        return "[" + idCategoria + "] " + nombreCategoria
                + " (" + estadoCategoria.getValor() + ")";
    }
}
