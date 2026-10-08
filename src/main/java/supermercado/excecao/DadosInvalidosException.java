package supermercado.excecao;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dados digitados com erro, campo a campo (ex.: CPF inválido e e-mail sem @).
 * A API devolve o mapa de erros para a tela mostrar cada mensagem embaixo do
 * campo certo.
 */
public class DadosInvalidosException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> erros;

    public DadosInvalidosException(Map<String, String> erros) {
        super(erros.size() == 1 ? "Corrija o campo destacado." : "Corrija os " + erros.size() + " campos destacados.");
        this.erros = Collections.unmodifiableMap(new LinkedHashMap<>(erros));
    }

    /** Campo -> mensagem, na ordem do formulário. */
    public Map<String, String> getErros() {
        return erros;
    }
}
