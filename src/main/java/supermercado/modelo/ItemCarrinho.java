package supermercado.modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Linha do carrinho: um produto e a quantidade escolhida pelo cliente.
 *
 * <p>Classe imutável. O subtotal é calculado aqui (antes ficava em
 * {@code CarrinhoDeCompras.calcularTotal()}, que precisava "pedir" os dados
 * do produto para fazer a conta: o code smell <i>Feature Envy</i>).</p>
 *
 * <p>É {@link Serializable} porque, na versão web, o carrinho fica guardado
 * na sessão do usuário no servidor.</p>
 */
public final class ItemCarrinho implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Produto produto;
    private final int quantidade;

    public ItemCarrinho(Produto produto, int quantidade) {
        this.produto = Objects.requireNonNull(produto, "O produto é obrigatório.");
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        this.quantidade = quantidade;
    }

    public Produto getProduto() {
        return produto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    /** Preço unitário x quantidade, sem erros de arredondamento. */
    public BigDecimal calcularSubtotal() {
        return produto.getPrecoUnitario().multiply(BigDecimal.valueOf(quantidade));
    }

    /**
     * Devolve um novo item com a quantidade somada. Usado quando o cliente
     * adiciona de novo um produto que já está no carrinho.
     */
    public ItemCarrinho comQuantidadeSomada(int quantidadeAdicional) {
        if (quantidadeAdicional <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        return new ItemCarrinho(produto, quantidade + quantidadeAdicional);
    }

    /** Devolve um novo item com outra quantidade (tela do carrinho). */
    public ItemCarrinho comQuantidade(int novaQuantidade) {
        return new ItemCarrinho(produto, novaQuantidade);
    }
}
