package controlstock.modelo;

public enum Rol {
    ADMINISTRADOR("administrador"),
    ENCARGADO_DEPOSITO("encargado_deposito"),
    OPERADOR("operador");

    private final String valor;

    Rol(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static Rol desdeValor(String valor) {
        for (Rol r : values()) {
            if (r.valor.equalsIgnoreCase(valor)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Rol no válido: " + valor);
    }
}
