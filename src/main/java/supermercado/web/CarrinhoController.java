package supermercado.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import supermercado.excecao.RegraNegocioException;
import supermercado.modelo.CarrinhoDeCompras;
import supermercado.servico.LojaService;
import supermercado.web.dto.CarrinhoDTO;
import supermercado.web.dto.Requisicoes;

/**
 * Carrinho do visitante, guardado na sessão do servidor.
 * Toda resposta traz o carrinho inteiro com os totais recalculados.
 */
@RestController
@RequestMapping("/api/carrinho")
public class CarrinhoController {

    private final LojaService loja;

    public CarrinhoController(LojaService loja) {
        this.loja = loja;
    }

    @GetMapping
    public CarrinhoDTO ver(HttpSession sessao) {
        return CarrinhoDTO.de(loja.resumir(SessaoDoVisitante.carrinho(sessao)));
    }

    /** Corpo: {"codigo": 2, "quantidade": 3}. Produto repetido soma na mesma linha. */
    @PostMapping("/itens")
    public CarrinhoDTO adicionar(@RequestBody Requisicoes.AdicionarItem corpo, HttpSession sessao) {
        if (corpo.codigo() == null) {
            throw new RegraNegocioException("Informe o código do produto.");
        }
        CarrinhoDeCompras carrinho = SessaoDoVisitante.carrinho(sessao);
        loja.adicionarAoCarrinho(carrinho, corpo.codigo(), quantidadeInformada(corpo.quantidade()));
        SessaoDoVisitante.atualizar(sessao, carrinho);
        return CarrinhoDTO.de(loja.resumir(carrinho));
    }

    /** Corpo: {"quantidade": 5} */
    @PutMapping("/itens/{codigo}")
    public CarrinhoDTO alterar(@PathVariable int codigo, @RequestBody Requisicoes.AlterarQuantidade corpo,
                               HttpSession sessao) {
        CarrinhoDeCompras carrinho = SessaoDoVisitante.carrinho(sessao);
        loja.alterarQuantidade(carrinho, codigo, quantidadeInformada(corpo.quantidade()));
        SessaoDoVisitante.atualizar(sessao, carrinho);
        return CarrinhoDTO.de(loja.resumir(carrinho));
    }

    @DeleteMapping("/itens/{codigo}")
    public CarrinhoDTO remover(@PathVariable int codigo, HttpSession sessao) {
        CarrinhoDeCompras carrinho = SessaoDoVisitante.carrinho(sessao);
        loja.removerDoCarrinho(carrinho, codigo);
        SessaoDoVisitante.atualizar(sessao, carrinho);
        return CarrinhoDTO.de(loja.resumir(carrinho));
    }

    private static int quantidadeInformada(Integer quantidade) {
        if (quantidade == null) {
            throw new RegraNegocioException("A quantidade vai de 1 a " + CarrinhoDeCompras.LIMITE_POR_PRODUTO + ".");
        }
        return quantidade;
    }
}
