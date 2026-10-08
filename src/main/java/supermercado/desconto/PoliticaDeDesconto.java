package supermercado.desconto;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Estratégia de desconto aplicada ao fechar um pedido (padrão Strategy).
 *
 * <p>Contrato que toda implementação deve respeitar (princípio de Liskov):
 * o desconto devolvido nunca é negativo e nunca é maior que o subtotal.
 * Assim, qualquer política pode substituir outra sem quebrar o cálculo do
 * total do pedido.</p>
 */
public interface PoliticaDeDesconto {

    /**
     * @param subtotal soma dos itens do pedido (maior ou igual a zero)
     * @return valor do desconto em reais, entre zero e o subtotal
     */
    BigDecimal calcularDesconto(BigDecimal subtotal);

    /** Texto curto para mostrar ao cliente, ex.: "10%" ou "nenhum". */
    String getDescricao();

    /**
     * Valor mínimo de compra para ganhar o desconto, quando a política tiver um.
     * A tela do carrinho usa este valor para mostrar quanto falta.
     *
     * <p>Método {@code default}: as políticas que não têm valor mínimo não
     * precisam mudar (princípio aberto/fechado).</p>
     */
    default Optional<BigDecimal> getValorMinimo() {
        return Optional.empty();
    }
}
