package supermercado.desconto;

import java.math.BigDecimal;
import java.util.Optional;
import supermercado.util.FormatoMoeda;

/**
 * Desconto em reais para compras a partir de um valor mínimo
 * (ex.: R$ 10,00 de desconto em compras a partir de R$ 100,00).
 */
public class DescontoPorValorMinimo implements PoliticaDeDesconto {

    private final BigDecimal valorMinimo;
    private final BigDecimal valorDoDesconto;

    public DescontoPorValorMinimo(BigDecimal valorMinimo, BigDecimal valorDoDesconto) {
        if (valorMinimo == null || valorMinimo.signum() < 0) {
            throw new IllegalArgumentException("O valor mínimo não pode ser negativo.");
        }
        if (valorDoDesconto == null || valorDoDesconto.signum() <= 0) {
            throw new IllegalArgumentException("O valor do desconto deve ser maior que zero.");
        }
        this.valorMinimo = valorMinimo;
        this.valorDoDesconto = valorDoDesconto;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal) {
        if (subtotal.compareTo(valorMinimo) < 0) {
            return BigDecimal.ZERO;
        }
        // nunca desconta mais do que o próprio subtotal (contrato da interface)
        return valorDoDesconto.min(subtotal);
    }

    @Override
    public String getDescricao() {
        return FormatoMoeda.formatar(valorDoDesconto) + " a partir de " + FormatoMoeda.formatar(valorMinimo);
    }

    @Override
    public Optional<BigDecimal> getValorMinimo() {
        return Optional.of(valorMinimo);
    }

    public BigDecimal getValorDoDesconto() {
        return valorDoDesconto;
    }
}
