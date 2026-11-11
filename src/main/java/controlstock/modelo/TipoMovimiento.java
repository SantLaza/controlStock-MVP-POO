package controlstock.modelo;

public enum TipoMovimiento {
    AJUSTE_POSITIVO("ajuste_positivo"),
    AJUSTE_NEGATIVO("ajuste_negativo");

    private final String valor;

    TipoMovimiento(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static TipoMovimiento desdeValor(String valor) {
        for (TipoMovimiento t : values()) {
            if (t.valor.equalsIgnoreCase(valor)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Tipo de movimiento no válido: " + valor);
    }
}
