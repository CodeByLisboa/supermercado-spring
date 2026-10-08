package supermercado.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Testes da API REST da loja (CT-A01 a CT-A14), com o servidor simulado
 * pelo MockMvc e o banco H2 em memória. Cada teste usa uma sessão nova,
 * como se fosse um visitante diferente. Requisições que alteram dados levam
 * o token CSRF ({@code csrf()}), como o front-end faz.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("teste")
class ApiDaLojaTest {

    private static final String CLIENTE_VALIDO = """
            {"nome": "João da Silva", "cpf": "529.982.247-25", "email": "joao@email.com",
             "celular": "(51) 99999-8888", "consentimento": true}""";

    @Autowired
    MockMvc mvc;

    MockHttpSession sessao;

    /** Operador logado, para as rotas da área do operador. */
    private static RequestPostProcessor operador() {
        return user("operador").roles("OPERADOR");
    }

    @BeforeEach
    void novoVisitante() {
        sessao = new MockHttpSession();
    }

    private ResultActions adicionar(int codigo, int quantidade) throws Exception {
        return mvc.perform(post("/api/carrinho/itens").session(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\": " + codigo + ", \"quantidade\": " + quantidade + "}"));
    }

    private ResultActions fecharPedido(String corpo) throws Exception {
        return mvc.perform(post("/api/pedidos").session(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    @Test
    @DisplayName("CT-A01 - a vitrine recebe os 14 produtos em JSON UTF-8, com acentos")
    void produtos() throws Exception {
        mvc.perform(get("/api/produtos"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(14)))
                .andExpect(jsonPath("$[1].nome").value("Filé de frango sassami"))
                .andExpect(jsonPath("$[1].preco").value(21.99));
        mvc.perform(get("/api/produtos").param("categoria", "Hortifrúti"))
                .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/promocao"))
                .andExpect(jsonPath("$.chamada").value("R$ 10,00 de desconto nas compras a partir de R$ 100,00"));
    }

    @Test
    @DisplayName("CT-A02 - adicionar o mesmo produto duas vezes soma na mesma linha (CT-M01 e CT-M02)")
    void adicionarDuasVezes_somaNaMesmaLinha() throws Exception {
        adicionar(2, 2).andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].subtotal").value(43.98));
        adicionar(2, 1)
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].quantidade").value(3))
                .andExpect(jsonPath("$.quantidadeDeUnidades").value(3));
    }

    @Test
    @DisplayName("CT-A03 - quantidade 0, -1 ou 100 é recusada com mensagem (CT-M03)")
    void quantidadeInvalida_eRecusada() throws Exception {
        for (int quantidade : new int[] {0, -1, 100}) {
            adicionar(1, quantidade).andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.mensagem").value("A quantidade vai de 1 a 99."));
        }
        mvc.perform(get("/api/carrinho").session(sessao)).andExpect(jsonPath("$.itens", hasSize(0)));
    }

    @Test
    @DisplayName("CT-A04 - produto inexistente responde 404")
    void produtoInexistente_404() throws Exception {
        adicionar(999, 1).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Produto de código 999 não encontrado."));
    }

    @Test
    @DisplayName("CT-A05 - alterar quantidade e remover recalculam o total (CT-M04)")
    void alterarERemover() throws Exception {
        adicionar(1, 1);
        adicionar(2, 2);
        mvc.perform(put("/api/carrinho/itens/2").session(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\": 5}"))
                .andExpect(jsonPath("$.subtotal").value(113.94))
                .andExpect(jsonPath("$.desconto").value(10.00))
                .andExpect(jsonPath("$.total").value(103.94));
        mvc.perform(delete("/api/carrinho/itens/1").session(sessao).with(csrf()))
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.total").value(99.95));
    }

    @Test
    @DisplayName("CT-A06 - o carrinho diz quanto falta para a promoção (R$ 99,99 x R$ 100,00 - CT-M11)")
    void metaDaPromocao() throws Exception {
        adicionar(2, 1);
        adicionar(5, 4);
        adicionar(14, 64);
        mvc.perform(get("/api/carrinho").session(sessao))
                .andExpect(jsonPath("$.subtotal").value(99.99))
                .andExpect(jsonPath("$.desconto").value(0))
                .andExpect(jsonPath("$.faltaParaPromocao").value(0.01));
        adicionar(14, 1);
        mvc.perform(get("/api/carrinho").session(sessao))
                .andExpect(jsonPath("$.subtotal").value(100.09))
                .andExpect(jsonPath("$.desconto").value(10.00))
                .andExpect(jsonPath("$.faltaParaPromocao").value(0));
    }

    @Test
    @DisplayName("CT-A07 - dados inválidos voltam 400 com a mensagem de cada campo (CT-M06 e CT-M07)")
    void dadosInvalidos_400ComCampos() throws Exception {
        adicionar(1, 1);
        fecharPedido("""
                {"nome": "João", "cpf": "123.456.789-00", "email": "joao.email.com", "consentimento": false}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Corrija os 4 campos destacados."))
                .andExpect(jsonPath("$.campos.nome").value("Informe nome e sobrenome."))
                .andExpect(jsonPath("$.campos.cpf").value("CPF inválido. Confira os números digitados."))
                .andExpect(jsonPath("$.campos.email").value("E-mail inválido. Use o formato nome@dominio.com."))
                .andExpect(jsonPath("$.campos.consentimento").value("Marque a autorização para continuar."));
        mvc.perform(get("/api/carrinho").session(sessao)).andExpect(jsonPath("$.itens", hasSize(1)));
    }

    @Test
    @DisplayName("CT-A08 - carrinho vazio não fecha pedido (CT-M08)")
    void carrinhoVazio_422() throws Exception {
        fecharPedido(CLIENTE_VALIDO).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("Não é possível fechar um pedido com o carrinho vazio."));
    }

    @Test
    @DisplayName("CT-A09 - pedido fechado: 201, total certo, carrinho vazio e CPF mascarado (CT-M09 e CT-M17)")
    void fecharPedido_201() throws Exception {
        adicionar(1, 1);
        adicionar(2, 2);
        fecharPedido(CLIENTE_VALIDO)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/pedidos/")))
                .andExpect(jsonPath("$.total").value(47.97))
                .andExpect(jsonPath("$.desconto").value(0))
                .andExpect(jsonPath("$.situacao").value("FECHADO"))
                .andExpect(jsonPath("$.cliente.cpf").value("***.982.247-**"))
                .andExpect(content().string(not(containsString("52998224725"))));
        mvc.perform(get("/api/carrinho").session(sessao))
                .andExpect(jsonPath("$.itens", hasSize(0)))
                .andExpect(jsonPath("$.quantidadeDeUnidades").value(0));
    }

    @Test
    @DisplayName("CT-A10 - cancelar duas vezes: a segunda é recusada (CT-M13)")
    void cancelarDuasVezes() throws Exception {
        adicionar(1, 1);
        String local = fecharPedido(CLIENTE_VALIDO).andReturn().getResponse().getHeader("Location");
        mvc.perform(post(local + "/cancelamento").with(csrf()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CANCELADO"));
        mvc.perform(post(local + "/cancelamento").with(csrf()).with(operador()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("já está cancelado")));
    }

    @Test
    @DisplayName("CT-A11 - pedido ou endereço inexistente responde 404, não 500")
    void inexistente_404() throws Exception {
        mvc.perform(get("/api/pedidos/999999").with(operador())).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Pedido nº 999999 não encontrado."));
        mvc.perform(get("/api/nao-existe")).andExpect(status().isNotFound());
        mvc.perform(get("/api/pedidos/abc")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CT-A12 - pedidos de exemplo e resumo de vendas (CT-M12)")
    void exemplosEResumo() throws Exception {
        mvc.perform(post("/api/pedidos/exemplos").with(csrf()).with(operador())).andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].cliente").value("João Silva"))
                .andExpect(jsonPath("$[2].cliente").value("Carlos Pereira"));
        mvc.perform(get("/api/pedidos").param("situacao", "FECHADO").with(operador()))
                .andExpect(jsonPath("$[0].numero").isNumber());
        mvc.perform(get("/api/pedidos/resumo").with(operador()))
                .andExpect(jsonPath("$.fechados").isNumber())
                .andExpect(jsonPath("$.totalVendido").isNumber());
    }

    @Test
    @DisplayName("CT-A13 - nome com código HTML é recusado pela validação do servidor (CT-M15)")
    void nomeComHtml_recusado() throws Exception {
        adicionar(1, 1);
        fecharPedido("""
                {"nome": "<script>alert(1)</script>", "cpf": "529.982.247-25", "email": "a@b.com", "consentimento": true}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").value("Use apenas letras e espaços no nome."));
    }

    @Test
    @DisplayName("CT-A14 - JSON mal formado responde 400")
    void jsonMalFormado_400() throws Exception {
        mvc.perform(post("/api/carrinho/itens").session(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{codigo: 1"))
                .andExpect(status().isBadRequest());
    }
}
