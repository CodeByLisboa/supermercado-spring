package supermercado.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static supermercado.Valores.assertReais;
import static supermercado.Valores.reais;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import supermercado.desconto.DescontoPercentual;
import supermercado.desconto.PoliticaDeDesconto;
import supermercado.desconto.SemDesconto;
import supermercado.excecao.RegraNegocioException;

/** Testes do cálculo do total do pedido e das regras de cancelamento (CT-U12 a CT-U17). */
public class PedidoTest {

    private Cliente cliente;
    private List<ItemCarrinho> itensDoCenarioOriginal;

    @BeforeEach
    public void prepararDados() {
        cliente = new Cliente("Joao Silva", "529.982.247-25", "joao@email.com");
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.adicionarProduto(new Produto(1, "Arroz", reais("3.99")), 1);
        carrinho.adicionarProduto(new Produto(2, "File de frango sassami", reais("21.99")), 2);
        itensDoCenarioOriginal = carrinho.getItens();
    }

    @Test
    public void getTotal_semDesconto_igualAoSubtotal() {
        Pedido pedido = new Pedido(1, cliente, itensDoCenarioOriginal, new SemDesconto());

        assertReais("47.97", pedido.getSubtotal());
        assertReais("0.00", pedido.getDesconto());
        assertReais("47.97", pedido.getTotal());
    }

    @Test
    public void getTotal_comDescontoDe10PorCento_subtraiODescontoDoSubtotal() {
        Pedido pedido = new Pedido(1, cliente, itensDoCenarioOriginal, new DescontoPercentual(reais("10")));

        assertReais("4.80", pedido.getDesconto());   // 10% de 47,97 = 4,797 -> 4,80
        assertReais("43.17", pedido.getTotal());
    }

    @Test
    public void pedidoFechado_naoMudaQuandoOCarrinhoMudaDepois() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.adicionarProduto(new Produto(1, "Arroz", reais("3.99")), 1);
        Pedido pedido = new Pedido(1, cliente, carrinho.getItens(), new SemDesconto());

        carrinho.adicionarProduto(new Produto(5, "Feijao", reais("8.50")), 1);

        assertReais("3.99", pedido.getTotal());
        assertEquals(1, pedido.getItens().size());
    }

    @Test
    public void cancelar_mudaStatusParaCancelado() {
        Pedido pedido = new Pedido(1, cliente, itensDoCenarioOriginal, new SemDesconto());

        pedido.cancelar();

        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
    }

    @Test
    public void cancelarDuasVezes_lancaRegraNegocioException() {
        Pedido pedido = new Pedido(1, cliente, itensDoCenarioOriginal, new SemDesconto());
        pedido.cancelar();

        assertThrows(RegraNegocioException.class, pedido::cancelar);
    }

    @Test
    public void politicaQueDescontaMaisQueOSubtotal_eRecusada() {
        PoliticaDeDesconto politicaInvalida = new PoliticaDeDesconto() {
            @Override
            public BigDecimal calcularDesconto(BigDecimal subtotal) {
                return subtotal.add(BigDecimal.ONE);
            }

            @Override
            public String getDescricao() {
                return "invalida";
            }
        };

        assertThrows(RegraNegocioException.class,
                () -> new Pedido(1, cliente, itensDoCenarioOriginal, politicaInvalida));
    }
}
