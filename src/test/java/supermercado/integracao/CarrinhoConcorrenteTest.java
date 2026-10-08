package supermercado.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * CT-I11: várias requisições do mesmo visitante alterando o carrinho ao mesmo
 * tempo (por exemplo, cliques rápidos com a internet lenta). Nenhuma
 * alteração pode se perder.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("teste")
class CarrinhoConcorrenteTest {

    private static final int REQUISICOES = 90;

    @LocalServerPort
    int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    private static String cookie(HttpResponse<?> resposta, String nome) {
        return resposta.headers().allValues("Set-Cookie").stream().filter(c -> c.startsWith(nome + "="))
                .map(c -> c.substring(nome.length() + 1, c.indexOf(';'))).findFirst().orElseThrow();
    }

    @RepeatedTest(5)
    @DisplayName("CT-I11 - 90 inclusões simultâneas no mesmo carrinho somam 90 unidades")
    void inclusoesSimultaneas_naoPerdemUnidades() throws Exception {
        HttpResponse<String> visita = http.send(HttpRequest.newBuilder(
                URI.create("http://localhost:" + porta + "/api/carrinho")).build(), HttpResponse.BodyHandlers.ofString());
        String sessao = cookie(visita, "JSESSIONID");
        String token = cookie(visita, "XSRF-TOKEN");

        List<CompletableFuture<HttpResponse<String>>> respostas = new ArrayList<>();
        for (int i = 0; i < REQUISICOES; i++) {
            respostas.add(http.sendAsync(HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api/carrinho/itens"))
                    .header("Content-Type", "application/json")
                    .header("Cookie", "JSESSIONID=" + sessao + "; XSRF-TOKEN=" + token)
                    .header("X-XSRF-TOKEN", token)
                    .POST(HttpRequest.BodyPublishers.ofString("{\"codigo\": 14, \"quantidade\": 1}")).build(),
                    HttpResponse.BodyHandlers.ofString()));
        }
        for (CompletableFuture<HttpResponse<String>> resposta : respostas) {
            assertThat(resposta.get().statusCode()).isEqualTo(200);
        }

        String carrinho = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api/carrinho"))
                .header("Cookie", "JSESSIONID=" + sessao).build(), HttpResponse.BodyHandlers.ofString()).body();
        assertThat(carrinho).contains("\"quantidadeDeUnidades\":" + REQUISICOES);
    }
}
