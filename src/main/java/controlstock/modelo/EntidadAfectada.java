package controlstock.modelo;

public enum EntidadAfectada {
    CATEGORIA("categoria"),
    PRODUCTO("producto"),
    DEPOSITO("deposito"),
    STOCK("stock"),
    USUARIO("usuario");

    private final String valor;

    EntidadAfectada(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static EntidadAfectada desdeValor(String valor) {
        for (EntidadAfectada e : values()) {
            if (e.valor.equalsIgnoreCase(valor)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Entidad no válida: " + valor);
    }
}
