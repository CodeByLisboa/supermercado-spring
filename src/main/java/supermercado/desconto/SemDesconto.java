package supermercado.desconto;

import java.math.BigDecimal;

/**
 * Política padrão: nenhum desconto.
 *
 * <p>Evita espalhar {@code if (desconto != null)} pelo código: quando não há
 * promoção, usa-se esta implementação (ideia do padrão Null Object).</p>
 */
public class SemDesconto implements PoliticaDeDesconto {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }

    @Override
    public String getDescricao() {
        return "nenhum";
    }
}
