package supermercado.web.dto;

import java.math.BigDecimal;
import supermercado.modelo.ItemCarrinho;
import supermercado.modelo.Produto;

/** Linha do carrinho ou do pedido. */
public record ItemDTO(int codigo, String nome, String unidade, String icone, BigDecimal precoUnitario,
                      int quantidade, BigDecimal subtotal) {

    public static ItemDTO de(ItemCarrinho item) {
        Produto produto = item.getProduto();
        return new ItemDTO(produto.getCodigo(), produto.getNome(), produto.getUnidade(), produto.getIcone(),
                produto.getPrecoUnitario(), item.getQuantidade(), item.calcularSubtotal());
    }
}
