package supermercado.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import supermercado.modelo.Cliente;
import supermercado.modelo.Pedido;

/**
 * Pedido completo, usado no cupom.
 *
 * <p>O CPF sai sempre mascarado (***.982.247-**): o número completo fica só
 * no banco de dados (proteção de dados pessoais, LGPD).</p>
 */
public record PedidoDTO(int numero, LocalDateTime dataHora, ClienteDTO cliente, List<ItemDTO> itens,
                        int quantidadeDeUnidades, BigDecimal subtotal, BigDecimal desconto,
                        String descricaoDesconto, BigDecimal total, String situacao) {

    /** Dados do cliente que podem aparecer na tela. */
    public record ClienteDTO(String nome, String email, String cpf) {

        static ClienteDTO de(Cliente cliente) {
            return new ClienteDTO(cliente.getNome(), cliente.getEmail(), cliente.getCpfMascarado());
        }
    }

    public static PedidoDTO de(Pedido pedido) {
        return new PedidoDTO(pedido.getNumero(), pedido.getDataHora(), ClienteDTO.de(pedido.getCliente()),
                pedido.getItens().stream().map(ItemDTO::de).toList(), pedido.quantidadeDeUnidades(),
                pedido.getSubtotal(), pedido.getDesconto(), pedido.getDescricaoDesconto(), pedido.getTotal(),
                pedido.getStatus().name());
    }
}
