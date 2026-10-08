package supermercado.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import supermercado.modelo.Pedido;

/**
 * Implementação de {@link PedidoDAO} que guarda os pedidos em memória.
 *
 * <p>Usada nos testes e enquanto o banco de dados não existe. Os métodos são
 * {@code synchronized} porque, no sistema web, várias requisições podem
 * fechar pedidos ao mesmo tempo.</p>
 */
public class PedidoDAOMemoria implements PedidoDAO {

    private final Map<Integer, Pedido> pedidos = new LinkedHashMap<>();
    private int ultimoNumero = 0;

    @Override
    public synchronized int proximoNumero() {
        ultimoNumero++;
        return ultimoNumero;
    }

    @Override
    public synchronized void salvar(Pedido pedido) {
        Objects.requireNonNull(pedido, "O pedido é obrigatório.");
        pedidos.put(pedido.getNumero(), pedido);
    }

    @Override
    public synchronized Optional<Pedido> buscarPorNumero(int numero) {
        return Optional.ofNullable(pedidos.get(numero));
    }

    @Override
    public synchronized List<Pedido> listarTodos() {
        return List.copyOf(pedidos.values());
    }
}
