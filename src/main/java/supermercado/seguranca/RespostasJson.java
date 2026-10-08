package supermercado.seguranca;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;

/** Escreve respostas JSON curtas nos filtros do Spring Security (fora dos controladores). */
final class RespostasJson {

    private static final ObjectMapper JSON = new ObjectMapper();

    private RespostasJson() {
    }

    static void escrever(HttpServletResponse resposta, int status, Map<String, ?> corpo) throws IOException {
        resposta.setStatus(status);
        resposta.setCharacterEncoding("UTF-8");
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        JSON.writeValue(resposta.getOutputStream(), corpo);
    }
}
