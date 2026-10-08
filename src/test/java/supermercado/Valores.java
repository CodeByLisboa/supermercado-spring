package supermercado;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Apoio aos testes: cria valores em reais e compara com duas casas decimais.
 *
 * <p>{@code BigDecimal.equals} diferencia 10 de 10.00; aqui os dois são
 * tratados como o mesmo valor em reais, e a mensagem de erro mostra os
 * valores de forma legível (ex.: esperado 47.97, obtido 47.98).</p>
 */
public final class Valores {

    private Valores() {
    }

    public static BigDecimal reais(String valor) {
        return new BigDecimal(valor);
    }

    public static void assertReais(String esperado, BigDecimal obtido) {
        assertEquals(new BigDecimal(esperado).setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                obtido.setScale(2, RoundingMode.UNNECESSARY).toPlainString());
    }
}
