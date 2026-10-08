package supermercado.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Regras de acesso do sistema.
 *
 * <ul>
 *   <li>Vitrine, carrinho e fechamento de pedido: livres para qualquer visitante.</li>
 *   <li>Lista de pedidos, resumo de vendas, cancelamento, pedidos de exemplo,
 *       a página {@code pedidos.html} e o console do H2: só operador logado.</li>
 *   <li>Login em {@code POST /api/login} (campos {@code login} e {@code senha}),
 *       saída em {@code POST /api/logout}.</li>
 *   <li>Proteção contra CSRF: o servidor manda o token no cookie
 *       {@code XSRF-TOKEN} e o front-end devolve no cabeçalho
 *       {@code X-XSRF-TOKEN} em toda requisição que altera dados.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class SegurancaConfig {

    private static final String OPERADOR = "OPERADOR";

    @Bean
    public PasswordEncoder codificadorDeSenha() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain regrasDeAcesso(HttpSecurity http, OperadorDetailsService operadores) throws Exception {
        RequestMatcher chamadaDaApi = requisicao -> requisicao.getRequestURI()
                .startsWith(requisicao.getContextPath() + "/api/");

        http
            .authorizeHttpRequests(acesso -> acesso
                .requestMatchers(HttpMethod.GET, "/api/pedidos", "/api/pedidos/resumo").hasRole(OPERADOR)
                .requestMatchers(HttpMethod.POST, "/api/pedidos/*/cancelamento", "/api/pedidos/exemplos")
                    .hasRole(OPERADOR)
                .requestMatchers("/pedidos.html", "/h2-console/**").hasRole(OPERADOR)
                .anyRequest().permitAll())
            .userDetailsService(operadores)
            .formLogin(login -> login
                .loginPage("/login.html")
                .loginProcessingUrl("/api/login")
                .usernameParameter("login")
                .passwordParameter("senha")
                .successHandler((requisicao, resposta, autenticacao) -> {
                    carregarTokenCsrf(requisicao);
                    RespostasJson.escrever(resposta, 200, Map.of("login", autenticacao.getName(),
                            "nome", operadores.nomeDo(autenticacao.getName())));
                })
                .failureHandler((requisicao, resposta, erro) -> RespostasJson.escrever(resposta, 401,
                        Map.of("status", 401, "mensagem", "Login ou senha incorretos.")))
                .permitAll())
            .logout(saida -> saida
                .logoutUrl("/api/logout")
                .logoutSuccessHandler((requisicao, resposta, autenticacao) -> resposta.setStatus(204)))
            .exceptionHandling(erros -> erros
                .defaultAuthenticationEntryPointFor((requisicao, resposta, erro) -> RespostasJson.escrever(resposta,
                        401, Map.of("status", 401, "mensagem", "Entre com o login do operador.")), chamadaDaApi)
                .defaultAuthenticationEntryPointFor(irParaOLogin(), requisicao -> true)
                .accessDeniedHandler((requisicao, resposta, erro) -> RespostasJson.escrever(resposta, 403,
                        Map.of("status", 403, "mensagem", "Acesso negado. Atualize a página e tente de novo."))))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers("/h2-console/**"))
            .addFilterAfter(new FiltroDoCookieCsrf(), BasicAuthenticationFilter.class)
            // o console do H2 usa frames da mesma origem
            .headers(cabecalhos -> cabecalhos.frameOptions(frames -> frames.sameOrigin()));
        return http.build();
    }

    /** Páginas protegidas: manda para o login e depois volta para a página pedida. */
    private static AuthenticationEntryPoint irParaOLogin() {
        return (requisicao, resposta, erro) -> {
            String pagina = requisicao.getRequestURI().substring(requisicao.getContextPath().length() + 1);
            resposta.sendRedirect(requisicao.getContextPath() + "/login.html?voltar="
                    + URLEncoder.encode(pagina, StandardCharsets.UTF_8));
        };
    }

    /** Gera o token CSRF (e o cookie) já na resposta da requisição atual. */
    private static void carregarTokenCsrf(HttpServletRequest requisicao) {
        CsrfToken token = (CsrfToken) requisicao.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            token.getToken();
        }
    }

    /**
     * O Spring Security só cria o token CSRF quando alguém pede por ele.
     * Este filtro pede em toda requisição, para o cookie XSRF-TOKEN estar
     * sempre disponível para o JavaScript da página.
     */
    static final class FiltroDoCookieCsrf extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
                                        FilterChain cadeia) throws ServletException, IOException {
            carregarTokenCsrf(requisicao);
            cadeia.doFilter(requisicao, resposta);
        }
    }
}
