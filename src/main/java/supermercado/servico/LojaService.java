package supermercado.servico;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import supermercado.dao.ProdutoDAO;
import supermercado.desconto.PoliticaDeDesconto;
import supermercado.excecao.DadosInvalidosException;
import supermercado.excecao.ProdutoNaoEncontradoException;
import supermercado.excecao.RegraNegocioException;
import supermercado.modelo.CarrinhoDeCompras;
import supermercado.modelo.Cliente;
import supermercado.modelo.ItemCarrinho;
import supermercado.modelo.Pedido;
import supermercado.modelo.Produto;
import supermercado.modelo.StatusPedido;
import supermercado.validacao.ValidadorCliente;

/**
 * Casos de uso da loja web: catálogo, carrinho e pedidos.
 *
 * <p>Junta o catálogo do banco ({@link ProdutoDAO}), o carrinho da sessão do
 * visitante e o {@link PedidoService} da etapa 6. As regras continuam nas
 * classes de domínio; aqui ficam a orquestração e as transações.</p>
 *
 * <p>O carrinho chega como parâmetro: quem guarda o carrinho de cada
 * visitante é a camada web (sessão HTTP). Assim esta classe pode ser testada
 * sem servidor.</p>
 */
@Service
public class LojaService {

    private final ProdutoDAO produtos;
    private final PedidoService pedidoService;
    private final TransactionTemplate transacao;

    public LojaService(ProdutoDAO produtos, PedidoService pedidoService, TransactionTemplate transacao) {
        this.produtos = produtos;
        this.pedidoService = pedidoService;
        this.transacao = transacao;
    }

    // ------------------------------------------------------------------ catálogo

    public List<Produto> listarProdutos() {
        return produtos.listarTodos();
    }

    public List<String> listarCategorias() {
        return produtos.listarCategorias();
    }

    public Produto buscarProduto(int codigo) {
        return produtos.buscarPorCodigo(codigo).orElseThrow(() -> new ProdutoNaoEncontradoException(codigo));
    }

    public PoliticaDeDesconto getPromocao() {
        return pedidoService.getPoliticaDeDesconto();
    }

    // ------------------------------------------------------------------ carrinho

    public void adicionarAoCarrinho(CarrinhoDeCompras carrinho, int codigo, int quantidade) {
        validarQuantidade(quantidade);
        carrinho.adicionarProduto(buscarProduto(codigo), quantidade);
    }

    public void alterarQuantidade(CarrinhoDeCompras carrinho, int codigo, int quantidade) {
        validarQuantidade(quantidade);
        carrinho.alterarQuantidade(produtoNoCarrinho(carrinho, codigo), quantidade);
    }

    public void removerDoCarrinho(CarrinhoDeCompras carrinho, int codigo) {
        carrinho.removerProduto(produtoNoCarrinho(carrinho, codigo));
    }

    private static Produto produtoNoCarrinho(CarrinhoDeCompras carrinho, int codigo) {
        return carrinho.buscarItem(codigo).map(ItemCarrinho::getProduto)
                .orElseThrow(() -> new RegraNegocioException("Este produto não está no carrinho."));
    }

    private static void validarQuantidade(int quantidade) {
        if (quantidade < 1 || quantidade > CarrinhoDeCompras.LIMITE_POR_PRODUTO) {
            throw new RegraNegocioException("A quantidade vai de 1 a " + CarrinhoDeCompras.LIMITE_POR_PRODUTO + ".");
        }
    }

    /** Subtotal, desconto, total e quanto falta para a promoção. */
    public ResumoDoCarrinho resumir(CarrinhoDeCompras carrinho) {
        PoliticaDeDesconto promocao = getPromocao();
        BigDecimal subtotal = carrinho.calcularTotal();
        BigDecimal desconto = carrinho.estaVazio() ? BigDecimal.ZERO : promocao.calcularDesconto(subtotal);
        BigDecimal valorMinimo = promocao.getValorMinimo().orElse(null);
        BigDecimal falta = valorMinimo == null ? null : valorMinimo.subtract(subtotal).max(BigDecimal.ZERO);
        return new ResumoDoCarrinho(carrinho.getItens(), carrinho.quantidadeDeUnidades(), subtotal, desconto,
                subtotal.subtract(desconto), promocao.getDescricao(), valorMinimo, falta);
    }

    // ------------------------------------------------------------------ pedidos

    /**
     * Valida os dados do cliente, fecha o pedido com os preços atuais do
     * catálogo e esvazia o carrinho. Tudo numa transação: ou o pedido é
     * gravado inteiro, ou nada é gravado.
     *
     * @throws DadosInvalidosException se algum campo estiver errado
     * @throws RegraNegocioException   se o carrinho estiver vazio
     */
    public Pedido fecharPedido(CarrinhoDeCompras carrinho, DadosDoCliente dados) {
        Map<String, String> erros = ValidadorCliente.validar(dados.nome(), dados.cpf(), dados.email(),
                dados.celular(), dados.consentimento());
        if (!erros.isEmpty()) {
            throw new DadosInvalidosException(erros);
        }
        if (carrinho.estaVazio()) {
            throw new RegraNegocioException("Não é possível fechar um pedido com o carrinho vazio.");
        }
        Cliente cliente = new Cliente(dados.nome(), dados.cpf(), dados.email(), dados.celular());
        CarrinhoDeCompras comPrecosAtuais = new CarrinhoDeCompras();
        for (ItemCarrinho item : carrinho.getItens()) {
            comPrecosAtuais.adicionarProduto(buscarProduto(item.getProduto().getCodigo()), item.getQuantidade());
        }
        Pedido pedido = transacao.execute(status -> pedidoService.fecharPedido(cliente, comPrecosAtuais));
        carrinho.esvaziar();
        return pedido;
    }

    public Pedido buscarPedido(int numero) {
        return pedidoService.buscarPedido(numero);
    }

    /** Pedidos do mais novo para o mais antigo. */
    public List<Pedido> listarPedidos() {
        List<Pedido> todos = new ArrayList<>(pedidoService.listarPedidos());
        todos.sort(Comparator.comparingInt(Pedido::getNumero).reversed());
        return todos;
    }

    public Pedido cancelarPedido(int numero) {
        return transacao.execute(status -> pedidoService.cancelarPedido(numero));
    }

    public ResumoDeVendas resumirVendas() {
        int fechados = 0;
        int cancelados = 0;
        BigDecimal totalVendido = BigDecimal.ZERO;
        for (Pedido pedido : pedidoService.listarPedidos()) {
            if (pedido.getStatus() == StatusPedido.FECHADO) {
                fechados++;
                totalVendido = totalVendido.add(pedido.getTotal());
            } else {
                cancelados++;
            }
        }
        return new ResumoDeVendas(fechados, cancelados, totalVendido);
    }

    /** Três pedidos de demonstração para a área do operador (mesmos da etapa 8). */
    public List<Pedido> criarPedidosDeExemplo() {
        List<Pedido> criados = new ArrayList<>();
        criados.add(exemplo(new DadosDoCliente("João Silva", "52998224725", "joao@email.com", "", true),
                new int[][] {{1, 1}, {2, 2}}));
        criados.add(exemplo(new DadosDoCliente("Maria Souza", "12345678909", "maria@email.com", "51999998888", true),
                new int[][] {{5, 2}, {9, 1}, {13, 2}, {11, 3}}));
        criados.add(exemplo(new DadosDoCliente("Carlos Pereira", "11144477735", "carlos@email.com", "", true),
                new int[][] {{6, 2}, {7, 1}, {8, 1}}));
        return criados;
    }

    private Pedido exemplo(DadosDoCliente cliente, int[][] itens) {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        for (int[] item : itens) {
            adicionarAoCarrinho(carrinho, item[0], item[1]);
        }
        return fecharPedido(carrinho, cliente);
    }
}
