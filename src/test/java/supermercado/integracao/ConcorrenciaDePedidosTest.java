package supermercado.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import supermercado.dao.ProdutoDAO;
import supermercado.modelo.CarrinhoDeCompras;
import supermercado.modelo.Cliente;
import supermercado.modelo.Pedido;
import supermercado.servico.PedidoService;

/**
 * CT-I09 (equivale ao CT-M16 do plano de testes): vários clientes fecham
 * pedidos ao mesmo tempo. Cada pedido precisa receber um número diferente e
 * nenhum pode se perder.
 */
@SpringBootTest
@ActiveProfiles("teste")
class ConcorrenciaDePedidosTest {

    private static final int CLIENTES_AO_MESMO_TEMPO = 20;

    @Autowired
    PedidoService pedidoService;

    @Autowired
    ProdutoDAO produtos;

    @Autowired
    TransactionTemplate transacao;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("CT-I09 - 20 pedidos fechados ao mesmo tempo recebem 20 números diferentes")
    void pedidosSimultaneos_recebemNumerosDiferentes() throws Exception {
        int pedidosAntes = jdbc.queryForObject("SELECT COUNT(*) FROM pedido", Integer.class);
        ExecutorService threads = Executors.newFixedThreadPool(CLIENTES_AO_MESMO_TEMPO);
        List<Callable<Integer>> clientes = new ArrayList<>();
        for (int i = 0; i < CLIENTES_AO_MESMO_TEMPO; i++) {
            clientes.add(() -> transacao.execute(status -> {
                CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
                carrinho.adicionarProduto(produtos.buscarPorCodigo(1).orElseThrow(), 1);
                Cliente cliente = new Cliente("Cliente Simultâneo", "529.982.247-25", "cliente@email.com");
                Pedido pedido = pedidoService.fecharPedido(cliente, carrinho);
                return pedido.getNumero();
            }));
        }

        Set<Integer> numeros = new TreeSet<>();
        for (Future<Integer> resultado : threads.invokeAll(clientes)) {
            numeros.add(resultado.get());
        }
        threads.shutdown();

        assertThat(numeros).hasSize(CLIENTES_AO_MESMO_TEMPO);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pedido", Integer.class))
                .isEqualTo(pedidosAntes + CLIENTES_AO_MESMO_TEMPO);
    }
}
