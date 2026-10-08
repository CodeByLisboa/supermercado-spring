package supermercado.web.dto;

import java.math.BigDecimal;
import supermercado.modelo.Produto;

/** Produto como a vitrine recebe em JSON. */
public record ProdutoDTO(int codigo, String nome, String unidade, String categoria, BigDecimal preco, String icone) {

    public static ProdutoDTO de(Produto produto) {
        return new ProdutoDTO(produto.getCodigo(), produto.getNome(), produto.getUnidade(),
                produto.getCategoria(), produto.getPrecoUnitario(), produto.getIcone());
    }
}
