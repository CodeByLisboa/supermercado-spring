package supermercado.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import supermercado.modelo.Pedido;

/** Linha da tabela de pedidos da área do operador. */
public record PedidoResumoDTO(int numero, LocalDateTime dataHora, String cliente, int quantidadeDeUnidades,
                              BigDecimal total, String situacao) {

    public static PedidoResumoDTO de(Pedido pedido) {
        return new PedidoResumoDTO(pedido.getNumero(), pedido.getDataHora(), pedido.getCliente().getNome(),
                pedido.quantidadeDeUnidades(), pedido.getTotal(), pedido.getStatus().name());
    }
}
