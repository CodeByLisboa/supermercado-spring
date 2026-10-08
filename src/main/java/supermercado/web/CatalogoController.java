package supermercado.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import supermercado.servico.LojaService;
import supermercado.web.dto.ProdutoDTO;
import supermercado.web.dto.PromocaoDTO;

/** Catálogo da vitrine: produtos, categorias e promoção vigente. */
@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final LojaService loja;

    public CatalogoController(LojaService loja) {
        this.loja = loja;
    }

    /** GET /api/produtos ou /api/produtos?categoria=Carnes */
    @GetMapping("/produtos")
    public List<ProdutoDTO> produtos(@RequestParam(required = false) String categoria) {
        return loja.listarProdutos().stream()
                .filter(p -> categoria == null || categoria.isBlank() || p.getCategoria().equalsIgnoreCase(categoria))
                .map(ProdutoDTO::de)
                .toList();
    }

    @GetMapping("/categorias")
    public List<String> categorias() {
        return loja.listarCategorias();
    }

    @GetMapping("/promocao")
    public PromocaoDTO promocao() {
        return PromocaoDTO.de(loja.getPromocao());
    }
}
