package supermercado.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static supermercado.Valores.assertReais;
import static supermercado.Valores.reais;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import supermercado.desconto.DescontoPorValorMinimo;
import supermercado.desconto.SemDesconto;
import supermercado.modelo.CarrinhoDeCompras;
import supermercado.modelo.Cliente;
import supermercado.modelo.ItemCarrinho;
import supermercado.modelo.Pedido;
import supermercado.modelo.StatusPedido;
import supermercado.servico.PedidoService;

/**
 * Pedidos gravados e lidos do banco H2 com JDBC (CT-I04 a CT-I08).
 * Cada teste roda numa transação desfeita no final ({@code @JdbcTest}).
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PedidoDAOJdbc.class, ProdutoDAOJdbc.class})
@ActiveProfiles("teste")
class PedidoDAOJdbcTest {

    @Autowired
    PedidoDAO pedidos;

    @Autowired
    ProdutoDAO produtos;

    @Autowired
    JdbcTemplate jdbc;

    private Cliente joao;

    @BeforeEach
    void preparar() {
        joao = new Cliente("João Silva", "529.982.247-25", "Joao@Email.com", "(51) 99999-8888");
    }

    private List<ItemCarrinho> itensDoCenarioOriginal() {
        return List.of(new ItemCarrinho(produtos.buscarPorCodigo(1).orElseThrow(), 1),
                new ItemCarrinho(produtos.buscarPorCodigo(2).orElseThrow(), 2));
    }

    @Test
    @DisplayName("CT-I04 - pedido gravado volta igual: cliente, itens, data, valores e situação")
    void salvarEBuscar_devolveOMesmoPedido() {
        Pedido original = new Pedido(pedidos.proximoNumero(), joao, itensDoCenarioOriginal(), new SemDesconto());
        pedidos.salvar(original);

        Pedido lido = pedidos.buscarPorNumero(original.getNumero()).orElseThrow();

        assertThat(lido.getCliente().getNome()).isEqualTo("João Silva");
        assertThat(lido.getCliente().getEmail()).isEqualTo("joao@email.com");
        assertThat(lido.getCliente().getCelular()).isEqualTo("51999998888");
        assertThat(lido.getItens()).extracting(i -> i.getProduto().getNome(), ItemCarrinho::getQuantidade)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Arroz branco tipo 1", 1),
                        org.assertj.core.groups.Tuple.tuple("Filé de frango sassami", 2));
        assertThat(lido.getDataHora()).isEqualTo(original.getDataHora());
        assertReais("47.97", lido.getTotal());
        assertThat(lido.getStatus()).isEqualTo(StatusPedido.FECHADO);
    }

    @Test
    @DisplayName("CT-I05 - cancelar e salvar de novo muda só a situação no banco")
    void cancelar_atualizaASituacao() {
        Pedido pedido = new Pedido(pedidos.proximoNumero(), joao, itensDoCenarioOriginal(), new SemDesconto());
        pedidos.salvar(pedido);

        pedido.cancelar();
        pedidos.salvar(pedido);

        assertThat(pedidos.buscarPorNumero(pedido.getNumero()).orElseThrow().getStatus())
                .isEqualTo(StatusPedido.CANCELADO);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM item_pedido WHERE pedido_numero = ?",
                Integer.class, pedido.getNumero())).isEqualTo(2);
    }

    @Test
    @DisplayName("CT-I06 - números de pedido são sequenciais")
    void proximoNumero_eSequencial() {
        int primeiro = pedidos.proximoNumero();
        int segundo = pedidos.proximoNumero();

        assertThat(segundo).isEqualTo(primeiro + 1);
    }

    @Test
    @DisplayName("CT-I07 - listar traz todos os pedidos em ordem de número, cada um com seus itens")
    void listarTodos_trazPedidosComItens() {
        Pedido a = new Pedido(pedidos.proximoNumero(), joao, itensDoCenarioOriginal(), new SemDesconto());
        Pedido b = new Pedido(pedidos.proximoNumero(), joao,
                List.of(new ItemCarrinho(produtos.buscarPorCodigo(5).orElseThrow(), 3)), new SemDesconto());
        pedidos.salvar(a);
        pedidos.salvar(b);

        List<Pedido> todos = pedidos.listarTodos();

        assertThat(todos).extracting(Pedido::getNumero).containsSubsequence(a.getNumero(), b.getNumero());
        Pedido lidoB = todos.stream().filter(p -> p.getNumero() == b.getNumero()).findFirst().orElseThrow();
        assertThat(lidoB.getItens()).hasSize(1);
        assertReais("53.70", lidoB.getTotal());
    }

    @Test
    @DisplayName("CT-I08 - o PedidoService da etapa 6 funciona com o DAO JDBC sem mudanças")
    void pedidoService_comDaoJdbc_gravaNoBanco() {
        PedidoService servico = new PedidoService(pedidos,
                new DescontoPorValorMinimo(reais("100.00"), reais("10.00")));
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.adicionarProduto(produtos.buscarPorCodigo(2).orElseThrow(), 5);

        Pedido pedido = servico.fecharPedido(joao, carrinho);

        assertReais("99.95", pedido.getTotal());
        assertThat(carrinho.estaVazio()).isTrue();
        assertThat(jdbc.queryForObject("SELECT total FROM pedido WHERE numero = ?",
                java.math.BigDecimal.class, pedido.getNumero())).isEqualByComparingTo("99.95");
    }
}
