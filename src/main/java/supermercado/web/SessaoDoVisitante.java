package supermercado.web;

import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import supermercado.modelo.CarrinhoDeCompras;

/**
 * O que o servidor guarda para cada visitante, na sessão HTTP (cookie
 * JSESSIONID): o carrinho e os números dos pedidos que ele fechou.
 *
 * <p>Na etapa 8 o carrinho ficava no localStorage do navegador; agora fica
 * no servidor e usa a classe {@link CarrinhoDeCompras} da etapa 6.</p>
 */
public final class SessaoDoVisitante {

    private static final String CARRINHO = "mercado.carrinho";
    private static final String PEDIDOS = "mercado.pedidos";

    private SessaoDoVisitante() {
    }

    public static CarrinhoDeCompras carrinho(HttpSession sessao) {
        synchronized (SessaoDoVisitante.class) {
            CarrinhoDeCompras carrinho = (CarrinhoDeCompras) sessao.getAttribute(CARRINHO);
            if (carrinho == null) {
                carrinho = new CarrinhoDeCompras();
                sessao.setAttribute(CARRINHO, carrinho);
            }
            return carrinho;
        }
    }

    /**
     * Executa uma operação no carrinho do visitante, uma de cada vez.
     *
     * <p>O {@link CarrinhoDeCompras} não foi feito para ser usado por duas
     * threads ao mesmo tempo, e o mesmo visitante pode mandar duas
     * requisições quase juntas (dois cliques, duas abas). A trava é o próprio
     * objeto do carrinho, então visitantes diferentes não esperam um pelo
     * outro. Depois da operação o carrinho é gravado de novo na sessão.</p>
     */
    public static <T> T comCarrinho(HttpSession sessao, Function<CarrinhoDeCompras, T> operacao) {
        CarrinhoDeCompras carrinho = carrinho(sessao);
        synchronized (carrinho) {
            T resultado = operacao.apply(carrinho);
            sessao.setAttribute(CARRINHO, carrinho);
            return resultado;
        }
    }

    public static void registrarPedido(HttpSession sessao, int numero) {
        Set<Integer> numeros = pedidos(sessao);
        numeros.add(numero);
        sessao.setAttribute(PEDIDOS, numeros);
    }

    public static boolean fechouOPedido(HttpSession sessao, int numero) {
        return pedidos(sessao).contains(numero);
    }

    @SuppressWarnings("unchecked")
    private static Set<Integer> pedidos(HttpSession sessao) {
        Object salvo = sessao.getAttribute(PEDIDOS);
        return salvo == null ? new LinkedHashSet<>() : (Set<Integer>) salvo;
    }
}
