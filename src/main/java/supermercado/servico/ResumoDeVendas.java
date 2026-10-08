package supermercado.servico;

import java.math.BigDecimal;

/** Números da área do operador: pedidos fechados, cancelados e total vendido (só fechados). */
public record ResumoDeVendas(int fechados, int cancelados, BigDecimal totalVendido) {
}
