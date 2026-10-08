package supermercado.desconto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static supermercado.Valores.assertReais;
import static supermercado.Valores.reais;

import org.junit.jupiter.api.Test;

/** Testes do cálculo de desconto percentual (CT-U18 a CT-U22). */
public class DescontoPercentualTest {

    @Test
    public void dezPorCentoDeCemReais_eDezReais() {
        DescontoPercentual desconto = new DescontoPercentual(reais("10"));

        assertReais("10.00", desconto.calcularDesconto(reais("100.00")));
    }

    @Test
    public void arredondaParaCentavos_meioParaCima() {
        DescontoPercentual desconto = new DescontoPercentual(reais("10"));

        // 10% de 47,97 = 4,797 -> arredonda para 4,80
        assertReais("4.80", desconto.calcularDesconto(reais("47.97")));
    }

    @Test
    public void cemPorCento_descontaOValorInteiro() {
        DescontoPercentual desconto = new DescontoPercentual(reais("100"));

        assertReais("47.97", desconto.calcularDesconto(reais("47.97")));
    }

    @Test
    public void percentualZeroOuAcimaDeCem_eRejeitado() {
        assertThrows(IllegalArgumentException.class, () -> new DescontoPercentual(reais("0")));
        assertThrows(IllegalArgumentException.class, () -> new DescontoPercentual(reais("150")));
    }

    @Test
    public void descricao_mostraOPercentualSemCasasDecimaisDesnecessarias() {
        assertEquals("10%", new DescontoPercentual(reais("10.00")).getDescricao());
        assertEquals("12.5%", new DescontoPercentual(reais("12.5")).getDescricao());
    }
}
