package supermercado.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Login do operador, proteção das rotas e CSRF (CT-S01 a CT-S09).
 * O CT-S06 fica em {@link CsrfNoServidorRealTest}, com requisições HTTP de verdade.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("teste")
class SegurancaTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    private MvcResult entrar(MockHttpSession sessao, String senha) throws Exception {
        return mvc.perform(post("/api/login").session(sessao).with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("login", "operador").param("senha", senha)).andReturn();
    }

    @Test
    @DisplayName("CT-S01 - lista de pedidos sem login responde 401 em JSON")
    void listaSemLogin_401() throws Exception {
        mvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Entre com o login do operador."));
    }

    @Test
    @DisplayName("CT-S02 - página da área do operador sem login vai para o login")
    void paginaSemLogin_redirecionaParaOLogin() throws Exception {
        mvc.perform(get("/pedidos.html")).andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login.html?voltar=pedidos.html"));
    }

    @Test
    @DisplayName("CT-S03 - senha errada responde 401 com mensagem")
    void senhaErrada_401() throws Exception {
        MvcResult resultado = entrar(new MockHttpSession(), "errada");
        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(resultado.getResponse().getContentAsString()).contains("Login ou senha incorretos.");
    }

    @Test
    @DisplayName("CT-S04 - login certo libera a lista de pedidos e o resumo")
    void loginCerto_liberaAAreaDoOperador() throws Exception {
        MockHttpSession sessao = new MockHttpSession();
        MvcResult resultado = entrar(sessao, "mercado123");
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("Operador da loja");
        MockHttpSession logado = (MockHttpSession) resultado.getRequest().getSession();

        mvc.perform(get("/api/pedidos").session(logado)).andExpect(status().isOk());
        mvc.perform(get("/api/pedidos/resumo").session(logado)).andExpect(status().isOk());
        mvc.perform(get("/api/sessao").session(logado)).andExpect(jsonPath("$.operador").value(true));
    }

    @Test
    @DisplayName("CT-S05 - requisição que altera dados sem token CSRF é recusada (403)")
    void semTokenCsrf_403() throws Exception {
        mvc.perform(post("/api/carrinho/itens").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\": 1, \"quantidade\": 1}")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CT-S07 - o cliente vê o próprio cupom, mas não o pedido de outra pessoa")
    void cupom_soParaQuemFechouOPedido() throws Exception {
        MockHttpSession cliente = new MockHttpSession();
        mvc.perform(post("/api/carrinho/itens").session(cliente).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\": 1, \"quantidade\": 1}"));
        String local = mvc.perform(post("/api/pedidos").session(cliente).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Ana Lima\", \"cpf\": \"39053344705\", \"email\": \"ana@email.com\","
                                + " \"consentimento\": true}"))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(local).session(cliente)).andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.nome").value("Ana Lima"));
        mvc.perform(get(local).session(new MockHttpSession())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CT-S08 - a senha fica no banco só como hash BCrypt")
    void senhaGuardadaComoHash() {
        String hash = jdbc.queryForObject("SELECT senha_hash FROM operador WHERE login = 'operador'", String.class);
        assertThat(hash).startsWith("$2").doesNotContain("mercado123");
    }

    @Test
    @DisplayName("CT-S09 - depois de sair, a lista de pedidos volta a pedir login")
    void sair_encerraASessao() throws Exception {
        MvcResult login = entrar(new MockHttpSession(), "mercado123");
        MockHttpSession logado = (MockHttpSession) login.getRequest().getSession();

        mvc.perform(post("/api/logout").session(logado).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/pedidos").session(logado)).andExpect(status().isUnauthorized());
    }
}
