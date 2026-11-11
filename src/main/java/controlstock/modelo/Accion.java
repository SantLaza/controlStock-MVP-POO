package controlstock.modelo;

public enum Accion {
    ALTA("alta"),
    MODIFICACION("modificacion"),
    BAJA_LOGICA("baja_logica");

    private final String valor;

    Accion(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static Accion desdeValor(String valor) {
        for (Accion a : values()) {
            if (a.valor.equalsIgnoreCase(valor)) {
                return a;
            }
        }
        throw new IllegalArgumentException("Acción no válida: " + valor);
    }
}
