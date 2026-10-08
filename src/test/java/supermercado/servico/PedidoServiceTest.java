package supermercado.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static supermercado.Valores.assertReais;
import static supermercado.Valores.reais;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import supermercado.dao.PedidoDAOMemoria;
import supermercado.desconto.DescontoPercentual;
import supermercado.desconto.SemDesconto;
import supermercado.excecao.RegraNegocioException;
import supermercado.modelo.CarrinhoDeCompras;
import supermercado.modelo.Cliente;
import supermercado.modelo.Pedido;
import supermercado.modelo.Produto;
import supermercado.modelo.StatusPedido;

/**
 * Testes dos casos de uso de pedidos (CT-U33 a CT-U38).
 *
 * <p>Usa o {@link PedidoDAOMemoria}, então não precisa de banco de dados.</p>
 */
public class PedidoServiceTest {

    private PedidoService servico;
    private Cliente cliente;

    @BeforeEach
    public void prepararServico() {
        servico = new PedidoService(new PedidoDAOMemoria(), new SemDesconto());
        cliente = new Cliente("Joao Silva", "529.982.247-25", "joao@email.com");
    }

    private CarrinhoDeCompras carrinhoDoCenarioOriginal() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.adicionarProduto(new Produto(1, "Arroz", reais("3.99")), 1);
        carrinho.adicionarProduto(new Produto(2, "File de frango sassami", reais("21.99")), 2);
        return carrinho;
    }

    @Test
    public void fecharPedido_geraNumerosSequenciais() {
        Pedido primeiro = servico.fecharPedido(cliente, carrinhoDoCenarioOriginal());
        Pedido segundo = servico.fecharPedido(cliente, carrinhoDoCenarioOriginal());

        assertEquals(1, primeiro.getNumero());
        assertEquals(2, segundo.getNumero());
    }

    @Test
    public void fecharPedido_calculaTotalEEsvaziaOCarrinho() {
        CarrinhoDeCompras carrinho = carrinhoDoCenarioOriginal();

        Pedido pedido = servico.fecharPedido(cliente, carrinho);

        assertReais("47.97", pedido.getTotal());
        assertTrue(carrinho.estaVazio());
    }

    @Test
    public void fecharPedido_aplicaAPoliticaDeDescontoDoServico() {
        PedidoService servicoComPromocao =
                new PedidoService(new PedidoDAOMemoria(), new DescontoPercentual(reais("10")));

        Pedido pedido = servicoComPromocao.fecharPedido(cliente, carrinhoDoCenarioOriginal());

        assertReais("43.17", pedido.getTotal());
    }

    @Test
    public void fecharPedidoComCarrinhoVazio_lancaRegraNegocioException() {
        assertThrows(RegraNegocioException.class,
                () -> servico.fecharPedido(cliente, new CarrinhoDeCompras()));
    }

    @Test
    public void buscarPedido_devolveOPedidoGravado_eFalhaSeNaoExistir() {
        Pedido pedido = servico.fecharPedido(cliente, carrinhoDoCenarioOriginal());

        assertSame(pedido, servico.buscarPedido(1));
        assertThrows(RegraNegocioException.class, () -> servico.buscarPedido(99));
    }

    @Test
    public void cancelarPedido_mudaStatus_eNaoPermiteCancelarDeNovo() {
        servico.fecharPedido(cliente, carrinhoDoCenarioOriginal());

        servico.cancelarPedido(1);

        assertEquals(StatusPedido.CANCELADO, servico.buscarPedido(1).getStatus());
        assertThrows(RegraNegocioException.class, () -> servico.cancelarPedido(1));
    }
}
