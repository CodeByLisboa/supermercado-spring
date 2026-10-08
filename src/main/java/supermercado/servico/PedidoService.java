package supermercado.servico;

import java.util.List;
import java.util.Objects;
import supermercado.dao.PedidoDAO;
import supermercado.desconto.PoliticaDeDesconto;
import supermercado.excecao.PedidoNaoEncontradoException;
import supermercado.excecao.RegraNegocioException;
import supermercado.modelo.CarrinhoDeCompras;
import supermercado.modelo.Cliente;
import supermercado.modelo.Pedido;

/**
 * Casos de uso de pedidos: fechar, consultar e cancelar.
 *
 * <p>É a "porta de entrada" das regras de negócio. Na etapa de refatoração
 * ele era chamado pelo {@code Main}; agora é chamado pelos controladores
 * REST do Spring, sem nenhuma mudança na regra.</p>
 *
 * <p>As dependências chegam pelo construtor (injeção de dependência) e são
 * interfaces, não classes concretas (princípio da inversão de dependência).
 * Por isso o mesmo serviço funciona com o {@code PedidoDAOMemoria} nos
 * testes unitários e com o {@code PedidoDAOJdbc} no sistema web.</p>
 */
public class PedidoService {

    private final PedidoDAO pedidoDAO;
    private final PoliticaDeDesconto politicaDeDesconto;

    public PedidoService(PedidoDAO pedidoDAO, PoliticaDeDesconto politicaDeDesconto) {
        this.pedidoDAO = Objects.requireNonNull(pedidoDAO, "O PedidoDAO é obrigatório.");
        this.politicaDeDesconto = Objects.requireNonNull(politicaDeDesconto, "A política de desconto é obrigatória.");
    }

    /**
     * Fecha o pedido com os itens do carrinho, aplica o desconto vigente,
     * grava o pedido e esvazia o carrinho.
     *
     * @throws RegraNegocioException se o carrinho estiver vazio
     */
    public Pedido fecharPedido(Cliente cliente, CarrinhoDeCompras carrinho) {
        Objects.requireNonNull(cliente, "O cliente é obrigatório.");
        Objects.requireNonNull(carrinho, "O carrinho é obrigatório.");
        if (carrinho.estaVazio()) {
            throw new RegraNegocioException("Não é possível fechar um pedido com o carrinho vazio.");
        }
        Pedido pedido = new Pedido(pedidoDAO.proximoNumero(), cliente, carrinho.getItens(), politicaDeDesconto);
        pedidoDAO.salvar(pedido);
        carrinho.esvaziar();
        return pedido;
    }

    /** @throws PedidoNaoEncontradoException se não existir pedido com esse número */
    public Pedido buscarPedido(int numero) {
        return pedidoDAO.buscarPorNumero(numero)
                .orElseThrow(() -> new PedidoNaoEncontradoException(numero));
    }

    public List<Pedido> listarPedidos() {
        return pedidoDAO.listarTodos();
    }

    /** @throws RegraNegocioException se o pedido não existir ou já estiver cancelado */
    public Pedido cancelarPedido(int numero) {
        Pedido pedido = buscarPedido(numero);
        pedido.cancelar();
        pedidoDAO.salvar(pedido);
        return pedido;
    }

    public PoliticaDeDesconto getPoliticaDeDesconto() {
        return politicaDeDesconto;
    }
}
