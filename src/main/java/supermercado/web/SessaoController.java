package supermercado.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import supermercado.seguranca.OperadorDetailsService;

/** Diz à página se há um operador logado (para mostrar o nome e o botão Sair). */
@RestController
public class SessaoController {

    private final OperadorDetailsService operadores;

    public SessaoController(OperadorDetailsService operadores) {
        this.operadores = operadores;
    }

    @GetMapping("/api/sessao")
    public Map<String, Object> sessao(HttpServletRequest requisicao) {
        if (requisicao.isUserInRole("OPERADOR") && requisicao.getUserPrincipal() != null) {
            String login = requisicao.getUserPrincipal().getName();
            return Map.of("operador", true, "login", login, "nome", operadores.nomeDo(login));
        }
        return Map.of("operador", false);
    }
}
