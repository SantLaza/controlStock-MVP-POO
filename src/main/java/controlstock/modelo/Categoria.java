package controlstock.modelo;

public class Categoria extends EntidadMaestra {

    private int idCategoria;
    private String nombreCategoria;

    public Categoria() {
    }

    public Categoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
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

    @Override
    public String getNombre() {
        return nombreCategoria;
    }

    @Override
    public String toString() {
        return "[" + idCategoria + "] " + nombreCategoria
                + " (" + getEstado().getValor() + ")";
    }
}
