package supermercado.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import supermercado.modelo.Cliente;
import supermercado.modelo.ItemCarrinho;
import supermercado.modelo.Pedido;
import supermercado.modelo.Produto;
import supermercado.modelo.StatusPedido;

/**
 * Pedidos gravados no banco de dados com JDBC.
 *
 * <p>Implementa a mesma interface {@link PedidoDAO} da etapa de refatoração;
 * o {@code PedidoService} não sabe se os pedidos estão na memória ou no
 * banco.</p>
 *
 * <p>Tabelas: {@code pedido} (cabeçalho, com nome e e-mail do cliente
 * copiados no momento da compra), {@code item_pedido} (uma linha por
 * produto, com nome e preço copiados no momento da compra), {@code cliente}
 * (cadastro atualizado) e {@code contador} (numeração). Os métodos que gravam devem ser chamados
 * dentro de uma transação (ver {@code LojaService}).</p>
 */
@Repository
public class PedidoDAOJdbc implements PedidoDAO {

    private static final String SELECT_PEDIDO =
            "SELECT numero, data_hora, desconto, descricao_desconto, situacao,"
            + " cliente_cpf, cliente_nome, cliente_email, cliente_celular FROM pedido";

    private static final String SELECT_ITENS =
            "SELECT pedido_numero, produto_codigo, nome_produto, unidade, preco_unitario, quantidade"
            + " FROM item_pedido";

    private final JdbcTemplate jdbc;

    public PedidoDAOJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Soma 1 ao contador e devolve o novo valor.
     *
     * <p>O {@code UPDATE} trava a linha do contador até o fim da transação.
     * Se dois clientes fecharem pedidos ao mesmo tempo, o segundo espera o
     * primeiro terminar, e os números nunca se repetem.</p>
     */
    @Override
    public int proximoNumero() {
        int linhas = jdbc.update("UPDATE contador SET valor = valor + 1 WHERE nome = 'pedido'");
        if (linhas != 1) {
            throw new IllegalStateException("Contador de pedidos não encontrado no banco de dados.");
        }
        return jdbc.queryForObject("SELECT valor FROM contador WHERE nome = 'pedido'", Integer.class);
    }

    /** Pedido novo: grava cliente, cabeçalho e itens. Pedido existente: atualiza a situação. */
    @Override
    public void salvar(Pedido pedido) {
        Objects.requireNonNull(pedido, "O pedido é obrigatório.");
        int atualizados = jdbc.update("UPDATE pedido SET situacao = ? WHERE numero = ?",
                pedido.getStatus().name(), pedido.getNumero());
        if (atualizados == 0) {
            inserir(pedido);
        }
    }

    private void inserir(Pedido pedido) {
        salvarCliente(pedido.getCliente());
        Cliente cliente = pedido.getCliente();
        jdbc.update("INSERT INTO pedido (numero, data_hora, cliente_cpf, cliente_nome, cliente_email,"
                        + " cliente_celular, subtotal, desconto, descricao_desconto, total, situacao)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                pedido.getNumero(), Timestamp.valueOf(pedido.getDataHora()), cliente.getCpf(),
                cliente.getNome(), cliente.getEmail(), cliente.getCelular(),
                pedido.getSubtotal(), pedido.getDesconto(), pedido.getDescricaoDesconto(),
                pedido.getTotal(), pedido.getStatus().name());

        List<Object[]> linhas = new ArrayList<>();
        int linha = 1;
        for (ItemCarrinho item : pedido.getItens()) {
            Produto produto = item.getProduto();
            linhas.add(new Object[] {pedido.getNumero(), linha++, produto.getCodigo(), produto.getNome(),
                produto.getUnidade(), produto.getPrecoUnitario(), item.getQuantidade()});
        }
        jdbc.batchUpdate("INSERT INTO item_pedido (pedido_numero, linha, produto_codigo, nome_produto,"
                + " unidade, preco_unitario, quantidade) VALUES (?, ?, ?, ?, ?, ?, ?)", linhas);
    }

    /** Cadastro do cliente pelo CPF: atualiza se já existe, inclui se é novo. */
    private void salvarCliente(Cliente cliente) {
        int atualizados = jdbc.update("UPDATE cliente SET nome = ?, email = ?, celular = ? WHERE cpf = ?",
                cliente.getNome(), cliente.getEmail(), cliente.getCelular(), cliente.getCpf());
        if (atualizados == 0) {
            jdbc.update("INSERT INTO cliente (cpf, nome, email, celular) VALUES (?, ?, ?, ?)",
                    cliente.getCpf(), cliente.getNome(), cliente.getEmail(), cliente.getCelular());
        }
    }

    @Override
    public Optional<Pedido> buscarPorNumero(int numero) {
        List<ItemCarrinho> itens = jdbc.query(SELECT_ITENS + " WHERE pedido_numero = ? ORDER BY linha",
                PedidoDAOJdbc::paraItem, numero);
        return jdbc.query(SELECT_PEDIDO + " WHERE numero = ?",
                (linha, n) -> paraPedido(linha, itens), numero).stream().findFirst();
    }

    @Override
    public List<Pedido> listarTodos() {
        Map<Integer, List<ItemCarrinho>> itensPorPedido = new HashMap<>();
        jdbc.query(SELECT_ITENS + " ORDER BY pedido_numero, linha", linha -> {
            itensPorPedido.computeIfAbsent(linha.getInt("pedido_numero"), n -> new ArrayList<>())
                    .add(paraItem(linha, 0));
        });
        return jdbc.query(SELECT_PEDIDO + " ORDER BY numero",
                (linha, n) -> paraPedido(linha, itensPorPedido.get(linha.getInt("numero"))));
    }

    private static ItemCarrinho paraItem(ResultSet linha, int numeroDaLinha) throws SQLException {
        Produto produto = new Produto(linha.getInt("produto_codigo"), linha.getString("nome_produto"),
                linha.getBigDecimal("preco_unitario"), linha.getString("unidade"), "", "");
        return new ItemCarrinho(produto, linha.getInt("quantidade"));
    }

    private static Pedido paraPedido(ResultSet linha, List<ItemCarrinho> itens) throws SQLException {
        Cliente cliente = new Cliente(linha.getString("cliente_nome"), linha.getString("cliente_cpf"),
                linha.getString("cliente_email"), linha.getString("cliente_celular"));
        return Pedido.restaurar(linha.getInt("numero"), cliente, itens,
                linha.getTimestamp("data_hora").toLocalDateTime(), linha.getBigDecimal("desconto"),
                linha.getString("descricao_desconto"), StatusPedido.valueOf(linha.getString("situacao")));
    }
}
