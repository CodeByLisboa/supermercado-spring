package supermercado.web.dto;

import java.math.BigDecimal;
import java.util.List;
import supermercado.servico.ResumoDoCarrinho;

/**
 * Carrinho com os totais já calculados pelo servidor.
 *
 * @param valorMinimoPromocao e {@code faltaParaPromocao} ficam {@code null} quando a promoção não tem valor mínimo
 */
public record CarrinhoDTO(List<ItemDTO> itens, int quantidadeDeUnidades, BigDecimal subtotal, BigDecimal desconto,
                          BigDecimal total, String descricaoPromocao, BigDecimal valorMinimoPromocao,
                          BigDecimal faltaParaPromocao) {

    public static CarrinhoDTO de(ResumoDoCarrinho resumo) {
        return new CarrinhoDTO(resumo.itens().stream().map(ItemDTO::de).toList(), resumo.quantidadeDeUnidades(),
                resumo.subtotal(), resumo.desconto(), resumo.total(), resumo.descricaoDaPromocao(),
                resumo.valorMinimoDaPromocao(), resumo.faltaParaPromocao());
    }
}
