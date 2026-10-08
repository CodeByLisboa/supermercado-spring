package supermercado.modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import supermercado.excecao.RegraNegocioException;

/**
 * Carrinho de compras do cliente.
 *
 * <p>Responsabilidade única: guardar os itens escolhidos e informar o total.
 * Não cria produtos (recebe o {@link Produto} pronto) e não imprime nada.</p>
 *
 * <p>Na versão web cada visitante tem um carrinho guardado na sessão do
 * servidor, por isso a classe é {@link Serializable}.</p>
 */
public class CarrinhoDeCompras implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Limite de unidades de um mesmo produto (mesma regra da tela). */
    public static final int LIMITE_POR_PRODUTO = 99;

    /** Itens indexados pelo código do produto, mantendo a ordem de inclusão. */
    private final Map<Integer, ItemCarrinho> itens = new LinkedHashMap<>();

    /**
     * Adiciona um produto ao carrinho. Se o produto já estiver no carrinho,
     * a quantidade é somada à existente em vez de criar uma linha duplicada.
     *
     * @throws RegraNegocioException se a soma passar de {@value #LIMITE_POR_PRODUTO} unidades
     */
    public void adicionarProduto(Produto produto, int quantidade) {
        Objects.requireNonNull(produto, "O produto é obrigatório.");
        ItemCarrinho itemExistente = itens.get(produto.getCodigo());
        ItemCarrinho itemAtualizado = (itemExistente == null)
                ? new ItemCarrinho(produto, quantidade)
                : itemExistente.comQuantidadeSomada(quantidade);
        if (itemAtualizado.getQuantidade() > LIMITE_POR_PRODUTO) {
            int jaNoCarrinho = itemExistente == null ? 0 : itemExistente.getQuantidade();
            throw new RegraNegocioException("Limite de " + LIMITE_POR_PRODUTO
                    + " unidades por produto. Você já tem " + jaNoCarrinho + " no carrinho.");
        }
        itens.put(produto.getCodigo(), itemAtualizado);
    }

    /**
     * Troca a quantidade de um produto que já está no carrinho.
     *
     * @throws RegraNegocioException se o produto não estiver no carrinho ou a quantidade
     *                               estiver fora do intervalo de 1 a {@value #LIMITE_POR_PRODUTO}
     */
    public void alterarQuantidade(Produto produto, int novaQuantidade) {
        Objects.requireNonNull(produto, "O produto é obrigatório.");
        ItemCarrinho itemExistente = itens.get(produto.getCodigo());
        if (itemExistente == null) {
            throw new RegraNegocioException("O produto " + produto.getNome() + " não está no carrinho.");
        }
        if (novaQuantidade < 1 || novaQuantidade > LIMITE_POR_PRODUTO) {
            throw new RegraNegocioException("A quantidade vai de 1 a " + LIMITE_POR_PRODUTO + ".");
        }
        itens.put(produto.getCodigo(), itemExistente.comQuantidade(novaQuantidade));
    }

    /** Remove o produto do carrinho. Retorna {@code false} se ele não estava lá. */
    public boolean removerProduto(Produto produto) {
        Objects.requireNonNull(produto, "O produto é obrigatório.");
        return itens.remove(produto.getCodigo()) != null;
    }

    public Optional<ItemCarrinho> buscarItem(int codigoDoProduto) {
        return Optional.ofNullable(itens.get(codigoDoProduto));
    }

    /** Cópia somente leitura dos itens: quem chama não consegue alterar o carrinho por fora. */
    public List<ItemCarrinho> getItens() {
        return List.copyOf(itens.values());
    }

    public BigDecimal calcularTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemCarrinho item : itens.values()) {
            total = total.add(item.calcularSubtotal());
        }
        return total;
    }

    /** Soma das quantidades (é o número que aparece no ícone do carrinho). */
    public int quantidadeDeUnidades() {
        int soma = 0;
        for (ItemCarrinho item : itens.values()) {
            soma += item.getQuantidade();
        }
        return soma;
    }

    public boolean estaVazio() {
        return itens.isEmpty();
    }

    public void esvaziar() {
        itens.clear();
    }
}
