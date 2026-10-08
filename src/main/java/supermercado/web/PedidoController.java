package supermercado.web;

import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import supermercado.modelo.CarrinhoDeCompras;
import supermercado.modelo.Pedido;
import supermercado.servico.DadosDoCliente;
import supermercado.servico.LojaService;
import supermercado.servico.ResumoDeVendas;
import supermercado.web.dto.PedidoDTO;
import supermercado.web.dto.PedidoResumoDTO;
import supermercado.web.dto.Requisicoes;

/** Fechamento de pedidos (cliente) e gestão de pedidos (operador). */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final LojaService loja;

    public PedidoController(LojaService loja) {
        this.loja = loja;
    }

    /** Fecha o pedido com o carrinho da sessão. Responde 201 com o pedido criado. */
    @PostMapping
    public ResponseEntity<PedidoDTO> fechar(@RequestBody Requisicoes.FecharPedido corpo, HttpSession sessao) {
        CarrinhoDeCompras carrinho = SessaoDoVisitante.carrinho(sessao);
        Pedido pedido = loja.fecharPedido(carrinho, new DadosDoCliente(corpo.nome(), corpo.cpf(), corpo.email(),
                corpo.celular(), Boolean.TRUE.equals(corpo.consentimento())));
        SessaoDoVisitante.atualizar(sessao, carrinho);
        SessaoDoVisitante.registrarPedido(sessao, pedido.getNumero());
        return ResponseEntity.created(URI.create("/api/pedidos/" + pedido.getNumero())).body(PedidoDTO.de(pedido));
    }

    @GetMapping("/{numero}")
    public PedidoDTO buscar(@PathVariable int numero) {
        return PedidoDTO.de(loja.buscarPedido(numero));
    }

    /** GET /api/pedidos ou /api/pedidos?situacao=CANCELADO */
    @GetMapping
    public List<PedidoResumoDTO> listar(@RequestParam(required = false) String situacao) {
        return loja.listarPedidos().stream()
                .filter(p -> situacao == null || situacao.isBlank() || p.getStatus().name().equalsIgnoreCase(situacao))
                .map(PedidoResumoDTO::de)
                .toList();
    }

    @GetMapping("/resumo")
    public ResumoDeVendas resumo() {
        return loja.resumirVendas();
    }

    @PostMapping("/{numero}/cancelamento")
    public PedidoDTO cancelar(@PathVariable int numero) {
        return PedidoDTO.de(loja.cancelarPedido(numero));
    }

    @PostMapping("/exemplos")
    @ResponseStatus(HttpStatus.CREATED)
    public List<PedidoResumoDTO> criarExemplos() {
        return loja.criarPedidosDeExemplo().stream().map(PedidoResumoDTO::de).toList();
    }
}
