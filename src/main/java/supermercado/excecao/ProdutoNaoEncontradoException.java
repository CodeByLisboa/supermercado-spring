package supermercado.excecao;

/** Lançada quando o código informado não existe no catálogo. */
public class ProdutoNaoEncontradoException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    public ProdutoNaoEncontradoException(int codigo) {
        super("Produto de código " + codigo + " não encontrado.");
    }
}
