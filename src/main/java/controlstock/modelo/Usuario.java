package controlstock.modelo;

public class Usuario {

    private int idUsuario;
    private String nombreUsuario;
    private String hashContrasena;
    private Rol rol;
    private Estado estadoUsuario;

    public Usuario() {
    }

    public Usuario(String nombreUsuario, String hashContrasena, Rol rol) {
        this.nombreUsuario = nombreUsuario;
        this.hashContrasena = hashContrasena;
        this.rol = rol;
        this.estadoUsuario = Estado.ACTIVO;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getHashContrasena() {
        return hashContrasena;
    }

    public void setHashContrasena(String hashContrasena) {
        this.hashContrasena = hashContrasena;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Estado getEstadoUsuario() {
        return estadoUsuario;
    }

    public void setEstadoUsuario(Estado estadoUsuario) {
        this.estadoUsuario = estadoUsuario;
    }

    @Override
    public String toString() {
        return nombreUsuario + " (" + rol.getValor() + ")";
    }
}
