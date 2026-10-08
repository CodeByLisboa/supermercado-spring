package supermercado.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static supermercado.Valores.assertReais;
import static supermercado.Valores.reais;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Testes do carrinho de compras: total, produtos repetidos e remoção (CT-U06 a CT-U11). */
public class CarrinhoDeComprasTest {

    private final Produto arroz = new Produto(1, "Arroz", reais("3.99"));
    private final Produto fileDeFrango = new Produto(2, "File de frango sassami", reais("21.99"));

    private CarrinhoDeCompras carrinho;

    @BeforeEach
    public void criarCarrinhoVazio() {
        carrinho = new CarrinhoDeCompras();
    }

    @Test
    public void carrinhoNovo_estaVazioETotalZero() {
        assertTrue(carrinho.estaVazio());
        assertReais("0.00", carrinho.calcularTotal());
    }

    @Test
    public void calcularTotal_cenarioDaAtividade3_somaOsSubtotais() {
        carrinho.adicionarProduto(arroz, 1);         // 1 x 3,99  =  3,99
        carrinho.adicionarProduto(fileDeFrango, 2);  // 2 x 21,99 = 43,98

        assertReais("47.97", carrinho.calcularTotal());
    }

    @Test
    public void adicionarProdutoRepetido_somaQuantidadeNaMesmaLinha() {
        carrinho.adicionarProduto(arroz, 1);
        carrinho.adicionarProduto(arroz, 2);

        List<ItemCarrinho> itens = carrinho.getItens();
        assertEquals(1, itens.size());
        assertEquals(3, itens.get(0).getQuantidade());
        assertReais("11.97", carrinho.calcularTotal());
    }

    @Test
    public void removerProduto_retornaTrueERecalculaTotal() {
        carrinho.adicionarProduto(arroz, 1);
        carrinho.adicionarProduto(fileDeFrango, 2);

        assertTrue(carrinho.removerProduto(arroz));
        assertReais("43.98", carrinho.calcularTotal());
    }

    @Test
    public void removerProdutoQueNaoEstaNoCarrinho_retornaFalse() {
        carrinho.adicionarProduto(fileDeFrango, 1);

        assertFalse(carrinho.removerProduto(arroz));
    }

    @Test
    public void getItens_listaNaoPodeSerAlteradaPorFora() {
        carrinho.adicionarProduto(arroz, 1);

        List<ItemCarrinho> itens = carrinho.getItens();

        assertThrows(UnsupportedOperationException.class, itens::clear);
    }
}
