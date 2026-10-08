package supermercado.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import supermercado.modelo.Produto;

/**
 * Catálogo de produtos lido do banco de dados com JDBC.
 *
 * <p>O {@link JdbcTemplate} do Spring abre e fecha a conexão, monta o
 * {@code PreparedStatement} (os parâmetros "?" protegem contra SQL
 * injection) e converte as {@code SQLException} em exceções do Spring.</p>
 */
@Repository
public class ProdutoDAOJdbc implements ProdutoDAO {

    private static final String SELECT_PRODUTO =
            "SELECT codigo, nome, preco, unidade, categoria, icone FROM produto";

    private final JdbcTemplate jdbc;

    public ProdutoDAOJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Produto> listarTodos() {
        return jdbc.query(SELECT_PRODUTO + " ORDER BY codigo", ProdutoDAOJdbc::paraProduto);
    }

    @Override
    public Optional<Produto> buscarPorCodigo(int codigo) {
        return jdbc.query(SELECT_PRODUTO + " WHERE codigo = ?", ProdutoDAOJdbc::paraProduto, codigo)
                .stream().findFirst();
    }

    @Override
    public List<String> listarCategorias() {
        return jdbc.queryForList("SELECT nome FROM categoria ORDER BY ordem", String.class);
    }

    private static Produto paraProduto(ResultSet linha, int numeroDaLinha) throws SQLException {
        return new Produto(linha.getInt("codigo"), linha.getString("nome"), linha.getBigDecimal("preco"),
                linha.getString("unidade"), linha.getString("categoria"), linha.getString("icone"));
    }
}
