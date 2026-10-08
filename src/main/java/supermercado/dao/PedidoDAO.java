package supermercado.dao;

import java.util.List;
import java.util.Optional;
import supermercado.modelo.Pedido;

/**
 * Acesso aos dados de pedidos (padrão DAO - Data Access Object).
 *
 * <p>As regras de negócio dependem só desta interface. Há duas
 * implementações: {@link PedidoDAOMemoria}, usada nos testes unitários, e
 * {@link PedidoDAOJdbc}, que grava no banco de dados no sistema web. A troca
 * de uma pela outra não exigiu nenhuma mudança no {@code PedidoService}
 * (princípio da inversão de dependência).</p>
 */
public interface PedidoDAO {

    /** Próximo número livre para um pedido (equivale ao auto incremento do banco). */
    int proximoNumero();

    /** Inclui o pedido ou atualiza, se já existir um com o mesmo número. */
    void salvar(Pedido pedido);

    Optional<Pedido> buscarPorNumero(int numero);

    List<Pedido> listarTodos();
}
