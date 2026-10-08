package supermercado.excecao;

/**
 * Lançada quando uma operação viola uma regra de negócio do supermercado
 * (por exemplo: fechar pedido com carrinho vazio ou cancelar um pedido já
 * cancelado).
 *
 * <p>Separar esta exceção de {@link IllegalArgumentException} permite que a
 * interface (console, desktop ou web) trate cada caso de um jeito: erro de
 * digitação do usuário x operação não permitida pelo negócio.</p>
 */
public class RegraNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
