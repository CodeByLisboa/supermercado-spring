package supermercado.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * CT-S06 com o servidor Tomcat de verdade e um cliente HTTP comum, como o
 * navegador faz: o primeiro GET recebe os cookies JSESSIONID e XSRF-TOKEN, e
 * o POST devolve o token no cabeçalho X-XSRF-TOKEN.
 *
 * <p>Fica separado dos testes com MockMvc porque o {@code csrf()} da
 * biblioteca spring-security-test troca o repositório de tokens do filtro
 * CSRF do contexto de testes; depois disso o cookie deixa de ser enviado
 * naquele contexto.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("teste")
class CsrfNoServidorRealTest {

    @LocalServerPort
    int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    private static String cookie(HttpResponse<?> resposta, String nome) {
        List<String> cookies = resposta.headers().allValues("Set-Cookie");
        return cookies.stream().filter(c -> c.startsWith(nome + "="))
                .map(c -> c.substring(nome.length() + 1, c.indexOf(';')))
                .findFirst().orElse(null);
    }

    private HttpResponse<String> adicionarItem(String sessao, String token, boolean mandarCabecalho) throws Exception {
        HttpRequest.Builder requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api/carrinho/itens"))
                .header("Content-Type", "application/json")
                .header("Cookie", "JSESSIONID=" + sessao + "; XSRF-TOKEN=" + token)
                .POST(HttpRequest.BodyPublishers.ofString("{\"codigo\": 2, \"quantidade\": 1}"));
        if (mandarCabecalho) {
            requisicao.header("X-XSRF-TOKEN", token);
        }
        return http.send(requisicao.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("CT-S06 - o token CSRF vem no cookie XSRF-TOKEN e é aceito no cabeçalho X-XSRF-TOKEN")
    void tokenDoCookie_funcionaNoCabecalho() throws Exception {
        HttpResponse<String> primeiraVisita = http.send(HttpRequest.newBuilder(
                URI.create("http://localhost:" + porta + "/api/carrinho")).build(), HttpResponse.BodyHandlers.ofString());
        String sessao = cookie(primeiraVisita, "JSESSIONID");
        String token = cookie(primeiraVisita, "XSRF-TOKEN");
        assertThat(sessao).isNotBlank();
        assertThat(token).isNotBlank();

        assertThat(adicionarItem(sessao, token, false).statusCode()).isEqualTo(403);
        HttpResponse<String> comCabecalho = adicionarItem(sessao, token, true);
        assertThat(comCabecalho.statusCode()).isEqualTo(200);
        assertThat(comCabecalho.body()).contains("Filé de frango sassami");
    }
}
