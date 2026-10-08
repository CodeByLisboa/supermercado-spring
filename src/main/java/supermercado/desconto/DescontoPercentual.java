package supermercado.desconto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Desconto de um percentual fixo sobre o subtotal (ex.: 10% em dia de promoção). */
public class DescontoPercentual implements PoliticaDeDesconto {

    private static final BigDecimal CEM = new BigDecimal("100");

    private final BigDecimal percentual;

    public DescontoPercentual(BigDecimal percentual) {
        if (percentual == null || percentual.signum() <= 0 || percentual.compareTo(CEM) > 0) {
            throw new IllegalArgumentException("O percentual de desconto deve estar entre 0 e 100.");
        }
        this.percentual = percentual;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal) {
        return subtotal.multiply(percentual).divide(CEM, 2, RoundingMode.HALF_UP);
    }

    @Override
    public String getDescricao() {
        return percentual.stripTrailingZeros().toPlainString() + "%";
    }
}
