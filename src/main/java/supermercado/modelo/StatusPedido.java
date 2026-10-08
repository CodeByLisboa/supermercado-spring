package supermercado.modelo;

/** Situações possíveis de um pedido. */
public enum StatusPedido {

    FECHADO("Fechado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
