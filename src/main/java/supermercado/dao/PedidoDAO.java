package supermercado.dao;

import java.util.List;
import java.util.Optional;
import supermercado.modelo.Pedido;

/**
 * Acesso aos dados de pedidos (padrão DAO - Data Access Object).
 *
 * <p>As regras de negócio dependem só desta interface. Hoje os pedidos ficam
 * em memória ({@link PedidoDAOMemoria}); na versão web basta criar um
 * {@code PedidoDAOJdbc} que grave no banco de dados, sem alterar o
 * {@code PedidoService}.</p>
 */
public interface PedidoDAO {

    /** Próximo número livre para um pedido (equivale ao auto incremento do banco). */
    int proximoNumero();

    /** Inclui o pedido ou atualiza, se já existir um com o mesmo número. */
    void salvar(Pedido pedido);

    Optional<Pedido> buscarPorNumero(int numero);

    List<Pedido> listarTodos();
}
