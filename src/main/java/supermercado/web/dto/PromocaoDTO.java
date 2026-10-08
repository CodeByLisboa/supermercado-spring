package supermercado.web.dto;

import java.math.BigDecimal;
import supermercado.desconto.DescontoPercentual;
import supermercado.desconto.DescontoPorValorMinimo;
import supermercado.desconto.PoliticaDeDesconto;
import supermercado.util.FormatoMoeda;

/**
 * Promoção vigente para a faixa de oferta do topo das páginas.
 *
 * @param chamada       frase da faixa de oferta, ou {@code null} quando não há promoção
 * @param valorMinimo   compra mínima para ganhar o desconto (só na promoção por valor mínimo)
 * @param valorDesconto desconto em reais (só na promoção por valor mínimo)
 */
public record PromocaoDTO(String tipo, String descricao, String chamada, BigDecimal valorMinimo,
                          BigDecimal valorDesconto) {

    public static PromocaoDTO de(PoliticaDeDesconto politica) {
        if (politica instanceof DescontoPorValorMinimo porValor) {
            BigDecimal minimo = porValor.getValorMinimo().orElseThrow();
            return new PromocaoDTO("valor-minimo", politica.getDescricao(),
                    FormatoMoeda.formatar(porValor.getValorDoDesconto()) + " de desconto nas compras a partir de "
                            + FormatoMoeda.formatar(minimo), minimo, porValor.getValorDoDesconto());
        }
        if (politica instanceof DescontoPercentual) {
            return new PromocaoDTO("percentual", politica.getDescricao(),
                    politica.getDescricao() + " de desconto em todas as compras", null, null);
        }
        return new PromocaoDTO("nenhuma", politica.getDescricao(), null, null, null);
    }
}
