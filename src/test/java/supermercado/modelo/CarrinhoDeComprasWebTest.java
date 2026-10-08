package supermercado.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static supermercado.Valores.assertReais;
import static supermercado.Valores.reais;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import supermercado.excecao.RegraNegocioException;

/** Regras do carrinho criadas para a versão web (CT-U39 a CT-U43). */
class CarrinhoDeComprasWebTest {

    private final Produto arroz = new Produto(1, "Arroz", reais("3.99"));
    private final Produto frango = new Produto(2, "Filé de frango sassami", reais("21.99"));

    @Test
    @DisplayName("CT-U39 - alterar a quantidade troca a linha e recalcula o total")
    void alterarQuantidade_trocaAQuantidadeERecalcula() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.adicionarProduto(frango, 2);

        carrinho.alterarQuantidade(frango, 5);

        assertEquals(5, carrinho.getItens().get(0).getQuantidade());
        assertReais("109.95", carrinho.calcularTotal());
    }

    @Test
    @DisplayName("CT-U40 - alterar para 0 ou para 100 é recusado")
    void alterarQuantidade_foraDoIntervalo_eRecusado() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.adicionarProduto(arroz, 1);

        assertThrows(RegraNegocioException.class, () -> carrinho.alterarQuantidade(arroz, 0));
        assertThrows(RegraNegocioException.class, () -> carrinho.alterarQuantidade(arroz, 100));
        assertEquals(1, carrinho.getItens().get(0).getQuantidade());
    }

    @Test
    @DisplayName("CT-U41 - alterar um produto que não está no carrinho é recusado")
    void alterarQuantidade_produtoForaDoCarrinho_eRecusado() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();

        assertThrows(RegraNegocioException.class, () -> carrinho.alterarQuantidade(arroz, 2));
        assertTrue(carrinho.estaVazio());
    }

    @Test
    @DisplayName("CT-U42 - somar acima de 99 unidades do mesmo produto é recusado")
    void adicionar_acimaDoLimite_eRecusadoSemAlterarOCarrinho() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.adicionarProduto(arroz, 98);

        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> carrinho.adicionarProduto(arroz, 2));

        assertTrue(erro.getMessage().contains("Você já tem 98"));
        assertEquals(98, carrinho.quantidadeDeUnidades());
    }

    @Test
    @DisplayName("CT-U43 - quantidade de unidades soma todas as linhas")
    void quantidadeDeUnidades_somaAsLinhas() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.adicionarProduto(arroz, 1);
        carrinho.adicionarProduto(frango, 2);
        carrinho.adicionarProduto(arroz, 99 - 1 - 50);

        assertEquals(51, carrinho.quantidadeDeUnidades());
    }
}
