package supermercado.web.dto;

import java.math.BigDecimal;
import supermercado.desconto.DescontoPercentual;
import supermercado.desconto.DescontoPorValorMinimo;
import supermercado.desconto.PoliticaDeDesconto;
import supermercado.util.FormatoMoeda;

/**
 * Promoção vigente para a faixa de oferta do topo das páginas.
 *
 * @param chamada frase da faixa de oferta, ou {@code null} quando não há promoção
 */
public record PromocaoDTO(String tipo, String descricao, String chamada, BigDecimal valorMinimo) {

    public static PromocaoDTO de(PoliticaDeDesconto politica) {
        if (politica instanceof DescontoPorValorMinimo porValor) {
            BigDecimal minimo = porValor.getValorMinimo().orElseThrow();
            return new PromocaoDTO("valor-minimo", politica.getDescricao(),
                    FormatoMoeda.formatar(porValor.getValorDoDesconto()) + " de desconto nas compras a partir de "
                            + FormatoMoeda.formatar(minimo), minimo);
        }
        if (politica instanceof DescontoPercentual) {
            return new PromocaoDTO("percentual", politica.getDescricao(),
                    politica.getDescricao() + " de desconto em todas as compras", null);
        }
        return new PromocaoDTO("nenhuma", politica.getDescricao(), null, null);
    }
}
