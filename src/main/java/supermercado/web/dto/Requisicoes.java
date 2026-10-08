package supermercado.web.dto;

/** Corpos (JSON) que o front-end envia para a API. */
public final class Requisicoes {

    private Requisicoes() {
    }

    /** POST /api/carrinho/itens */
    public record AdicionarItem(Integer codigo, Integer quantidade) {
    }

    /** PUT /api/carrinho/itens/{codigo} */
    public record AlterarQuantidade(Integer quantidade) {
    }

    /** POST /api/pedidos */
    public record FecharPedido(String nome, String cpf, String email, String celular, Boolean consentimento) {
    }
}
