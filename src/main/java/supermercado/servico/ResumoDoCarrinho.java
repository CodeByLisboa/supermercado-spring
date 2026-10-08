package supermercado.servico;

import java.math.BigDecimal;
import java.util.List;
import supermercado.modelo.ItemCarrinho;

/**
 * Totais do carrinho calculados com a promoção vigente.
 *
 * @param valorMinimoDaPromocao valor mínimo para ganhar o desconto, ou {@code null} se a promoção não tiver
 * @param faltaParaPromocao     quanto falta para o desconto (zero se já ganhou), ou {@code null}
 */
public record ResumoDoCarrinho(List<ItemCarrinho> itens, int quantidadeDeUnidades, BigDecimal subtotal,
                               BigDecimal desconto, BigDecimal total, String descricaoDaPromocao,
                               BigDecimal valorMinimoDaPromocao, BigDecimal faltaParaPromocao) {
}
