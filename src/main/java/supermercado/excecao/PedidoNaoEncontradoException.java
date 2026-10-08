package supermercado.excecao;

/**
 * Lançada quando se procura um pedido que não existe.
 *
 * <p>É uma {@link RegraNegocioException} (quem já tratava a exceção mais
 * geral continua funcionando), mas tem tipo próprio para a API web
 * responder "404 - não encontrado" em vez de um erro de regra.</p>
 */
public class PedidoNaoEncontradoException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    public PedidoNaoEncontradoException(int numero) {
        super("Pedido nº " + numero + " não encontrado.");
    }
}
