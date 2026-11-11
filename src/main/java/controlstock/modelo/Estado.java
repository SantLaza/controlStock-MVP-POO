package controlstock.modelo;

public enum Estado {
    ACTIVO("activo"),
    INACTIVO("inactivo");

    private final String valor;

    Estado(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static Estado desdeValor(String valor) {
        for (Estado e : values()) {
            if (e.valor.equalsIgnoreCase(valor)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Estado no válido: " + valor);
    }
}
