package supermercado.dao;

import java.util.List;
import java.util.Optional;
import supermercado.modelo.Produto;

/** Acesso ao catálogo de produtos (padrão DAO). */
public interface ProdutoDAO {

    /** Todos os produtos do catálogo, em ordem de código. */
    List<Produto> listarTodos();

    Optional<Produto> buscarPorCodigo(int codigo);

    /** Nomes das categorias na ordem em que aparecem na vitrine. */
    List<String> listarCategorias();
}
