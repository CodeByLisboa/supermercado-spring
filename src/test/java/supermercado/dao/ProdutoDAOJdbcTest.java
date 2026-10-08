package supermercado.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import supermercado.modelo.Produto;

/** Catálogo lido do banco H2 com JDBC (CT-I01 a CT-I03). */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProdutoDAOJdbc.class)
@ActiveProfiles("teste")
class ProdutoDAOJdbcTest {

    @Autowired
    ProdutoDAO produtos;

    @Test
    @DisplayName("CT-I01 - o catálogo tem os 14 produtos, com acentos e ícones preservados")
    void listarTodos_trazOCatalogoCompleto() {
        List<Produto> catalogo = produtos.listarTodos();

        assertThat(catalogo).hasSize(14);
        Produto frango = catalogo.get(1);
        assertThat(frango.getNome()).isEqualTo("Filé de frango sassami");
        assertThat(frango.getIcone()).isEqualTo("🍗");
        assertThat(frango.getUnidade()).isEqualTo("Bandeja 1 kg");
        assertThat(frango.getCategoria()).isEqualTo("Carnes");
    }

    @Test
    @DisplayName("CT-I02 - busca por código traz o preço exato; código inexistente volta vazio")
    void buscarPorCodigo() {
        assertThat(produtos.buscarPorCodigo(2)).get()
                .extracting(Produto::getPrecoUnitario).hasToString("21.99");
        assertThat(produtos.buscarPorCodigo(14)).get()
                .extracting(Produto::getPrecoUnitario).hasToString("0.10");
        assertThat(produtos.buscarPorCodigo(999)).isEmpty();
    }

    @Test
    @DisplayName("CT-I03 - categorias vêm na ordem da vitrine")
    void listarCategorias_naOrdemDaVitrine() {
        assertThat(produtos.listarCategorias()).containsExactly("Mercearia", "Carnes", "Hortifrúti",
                "Frios e laticínios", "Padaria", "Bebidas", "Limpeza");
    }
}
