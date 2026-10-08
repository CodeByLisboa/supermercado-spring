package supermercado.web;

import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashSet;
import java.util.Set;
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
        CarrinhoDeCompras carrinho = (CarrinhoDeCompras) sessao.getAttribute(CARRINHO);
        if (carrinho == null) {
            carrinho = new CarrinhoDeCompras();
            sessao.setAttribute(CARRINHO, carrinho);
        }
        return carrinho;
    }

    /** Grava de novo o carrinho na sessão depois de uma alteração. */
    public static void atualizar(HttpSession sessao, CarrinhoDeCompras carrinho) {
        sessao.setAttribute(CARRINHO, carrinho);
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
