package supermercado.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * Formato único de erro da API.
 *
 * @param campos campo do formulário -> mensagem (só nos erros de validação)
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErroDTO(int status, String erro, String mensagem, Map<String, String> campos) {

    public ErroDTO(int status, String erro, String mensagem) {
        this(status, erro, mensagem, null);
    }
}
