package supermercado.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static supermercado.Valores.assertReais;
import static supermercado.Valores.reais;

import org.junit.jupiter.api.Test;

/** Testes do cálculo de subtotal de uma linha do carrinho (CT-U01 a CT-U05). */
public class ItemCarrinhoTest {

    private final Produto fileDeFrango = new Produto(2, "File de frango sassami", reais("21.99"));

    @Test
    public void calcularSubtotal_multiplicaPrecoUnitarioPelaQuantidade() {
        ItemCarrinho item = new ItemCarrinho(fileDeFrango, 2);

        assertReais("43.98", item.calcularSubtotal());
    }

    @Test
    public void calcularSubtotal_naoTemErroDeArredondamentoComCentavos() {
        Produto bala = new Produto(3, "Bala", reais("0.10"));

        ItemCarrinho item = new ItemCarrinho(bala, 3);

        // com double, 0.1 * 3 resultaria em 0.30000000000000004
        assertReais("0.30", item.calcularSubtotal());
    }

    @Test
    public void construtor_quantidadeZero_lancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> new ItemCarrinho(fileDeFrango, 0));
    }

    @Test
    public void comQuantidadeSomada_devolveNovoItemSemAlterarOOriginal() {
        ItemCarrinho original = new ItemCarrinho(fileDeFrango, 1);

        ItemCarrinho somado = original.comQuantidadeSomada(2);

        assertEquals(3, somado.getQuantidade());
        assertEquals(1, original.getQuantidade());
    }

    @Test
    public void comQuantidadeSomada_quantidadeNegativa_lancaExcecao() {
        ItemCarrinho item = new ItemCarrinho(fileDeFrango, 5);

        assertThrows(IllegalArgumentException.class, () -> item.comQuantidadeSomada(-1));
    }
}
