package supermercado.config;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import supermercado.dao.PedidoDAO;
import supermercado.desconto.DescontoPercentual;
import supermercado.desconto.DescontoPorValorMinimo;
import supermercado.desconto.PoliticaDeDesconto;
import supermercado.desconto.SemDesconto;
import supermercado.servico.PedidoService;

/**
 * Monta as classes de regra de negócio (que não conhecem o Spring) e as
 * entrega ao Spring como beans.
 *
 * <p>Na etapa de refatoração quem fazia isso era o {@code Main}, com
 * {@code new PedidoService(new PedidoDAOMemoria(), ...)}. Agora o Spring
 * injeta o {@code PedidoDAOJdbc} e a promoção vem do
 * {@code application.properties}: trocar a promoção não exige mudar código.</p>
 */
@Configuration(proxyBeanMethods = false)
public class RegrasDeNegocioConfig {

    @Bean
    public PoliticaDeDesconto politicaDeDesconto(
            @Value("${mercado.promocao.tipo:nenhuma}") String tipo,
            @Value("${mercado.promocao.valor-minimo:100.00}") BigDecimal valorMinimo,
            @Value("${mercado.promocao.valor-desconto:10.00}") BigDecimal valorDesconto,
            @Value("${mercado.promocao.percentual:10}") BigDecimal percentual) {
        switch (tipo.trim().toLowerCase()) {
            case "valor-minimo":
                return new DescontoPorValorMinimo(valorMinimo, valorDesconto);
            case "percentual":
                return new DescontoPercentual(percentual);
            case "nenhuma":
                return new SemDesconto();
            default:
                throw new IllegalArgumentException("mercado.promocao.tipo inválido: " + tipo
                        + " (use valor-minimo, percentual ou nenhuma)");
        }
    }

    @Bean
    public PedidoService pedidoService(PedidoDAO pedidoDAO, PoliticaDeDesconto politicaDeDesconto) {
        return new PedidoService(pedidoDAO, politicaDeDesconto);
    }
}
