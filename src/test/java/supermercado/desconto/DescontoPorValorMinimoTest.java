package supermercado.desconto;

import static supermercado.Valores.assertReais;
import static supermercado.Valores.reais;

import org.junit.jupiter.api.Test;

/**
 * Testes da promoção "R$ 10,00 de desconto em compras a partir de R$ 100,00"
 * (CT-U23 a CT-U26), incluindo os valores de fronteira.
 */
public class DescontoPorValorMinimoTest {

    private final DescontoPorValorMinimo dezReaisAPartirDeCem =
            new DescontoPorValorMinimo(reais("100.00"), reais("10.00"));

    @Test
    public void compraUmCentavoAbaixoDoMinimo_naoGanhaDesconto() {
        assertReais("0.00", dezReaisAPartirDeCem.calcularDesconto(reais("99.99")));
    }

    @Test
    public void compraExatamenteNoMinimo_ganhaDesconto() {
        assertReais("10.00", dezReaisAPartirDeCem.calcularDesconto(reais("100.00")));
    }

    @Test
    public void compraAcimaDoMinimo_ganhaODescontoFixo() {
        assertReais("10.00", dezReaisAPartirDeCem.calcularDesconto(reais("109.95")));
    }

    @Test
    public void descontoMaiorQueACompra_ficaLimitadoAoSubtotal() {
        DescontoPorValorMinimo cinquentaReaisAPartirDeUm =
                new DescontoPorValorMinimo(reais("1.00"), reais("50.00"));

        assertReais("4.99", cinquentaReaisAPartirDeUm.calcularDesconto(reais("4.99")));
    }
}
