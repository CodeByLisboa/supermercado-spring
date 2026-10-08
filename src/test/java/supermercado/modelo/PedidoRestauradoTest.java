package supermercado.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static supermercado.Valores.assertReais;
import static supermercado.Valores.reais;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import supermercado.excecao.RegraNegocioException;

/** Pedido lido do banco de dados (CT-U47 e CT-U48). */
class PedidoRestauradoTest {

    private final Cliente cliente = new Cliente("João Silva", "529.982.247-25", "joao@email.com");
    private final List<ItemCarrinho> itens = List.of(
            new ItemCarrinho(new Produto(2, "Filé de frango sassami", reais("21.99")), 5));

    @Test
    @DisplayName("CT-U47 - restaurar mantém data, desconto gravado e situação")
    void restaurar_mantemOsDadosGravados() {
        LocalDateTime data = LocalDateTime.of(2026, 10, 1, 9, 30);

        Pedido pedido = Pedido.restaurar(7, cliente, itens, data, reais("10.00"),
                "R$ 10,00 a partir de R$ 100,00", StatusPedido.CANCELADO);

        assertEquals(7, pedido.getNumero());
        assertEquals(data, pedido.getDataHora());
        assertReais("109.95", pedido.getSubtotal());
        assertReais("99.95", pedido.getTotal());
        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
        assertEquals(5, pedido.quantidadeDeUnidades());
    }

    @Test
    @DisplayName("CT-U48 - restaurar com desconto maior que o subtotal é recusado")
    void restaurar_comDescontoInvalido_eRecusado() {
        assertThrows(RegraNegocioException.class, () -> Pedido.restaurar(8, cliente, itens,
                LocalDateTime.now(), reais("500.00"), "", StatusPedido.FECHADO));
    }
}
