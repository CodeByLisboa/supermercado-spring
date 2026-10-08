package supermercado.validacao;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Valida os campos do formulário de identificação, campo a campo.
 *
 * <p>As mensagens são exatamente as mesmas do {@code validacao.js} do
 * front-end. O navegador valida primeiro, para a pessoa ver o erro na hora;
 * o servidor valida de novo porque a validação do navegador pode ser
 * contornada (por exemplo, enviando a requisição direto para a API).</p>
 */
public final class ValidadorCliente {

    private static final Pattern SOMENTE_LETRAS = Pattern.compile("^[A-Za-zÀ-ÖØ-öø-ÿ' ]+$");

    private ValidadorCliente() {
        // classe utilitária: não deve ser instanciada
    }

    /**
     * @return mapa campo -> mensagem com os campos inválidos (vazio quando está tudo certo),
     *         na ordem em que aparecem no formulário
     */
    public static Map<String, String> validar(String nome, String cpf, String email,
                                              String celular, boolean consentimento) {
        Map<String, String> erros = new LinkedHashMap<>();
        adicionarSeHouver(erros, "nome", validarNome(nome));
        adicionarSeHouver(erros, "cpf", validarCpf(cpf));
        adicionarSeHouver(erros, "email", validarEmail(email));
        adicionarSeHouver(erros, "celular", validarCelular(celular));
        if (!consentimento) {
            erros.put("consentimento", "Marque a autorização para continuar.");
        }
        return erros;
    }

    private static void adicionarSeHouver(Map<String, String> erros, String campo, String mensagem) {
        if (!mensagem.isEmpty()) {
            erros.put(campo, mensagem);
        }
    }

    public static String validarNome(String valor) {
        String texto = valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
        if (texto.isEmpty()) {
            return "Informe seu nome completo.";
        }
        if (!SOMENTE_LETRAS.matcher(texto).matches()) {
            return "Use apenas letras e espaços no nome.";
        }
        if (texto.split(" ").length < 2) {
            return "Informe nome e sobrenome.";
        }
        return "";
    }

    public static String validarCpf(String valor) {
        String digitos = ValidadorCpf.somenteDigitos(valor);
        if (digitos.isEmpty()) {
            return "Informe o CPF.";
        }
        if (digitos.length() < 11) {
            return "O CPF tem 11 números. Faltam " + (11 - digitos.length()) + ".";
        }
        if (!ValidadorCpf.isValido(digitos)) {
            return "CPF inválido. Confira os números digitados.";
        }
        return "";
    }

    public static String validarEmail(String valor) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.isEmpty()) {
            return "Informe o e-mail.";
        }
        if (!ValidadorEmail.isValido(texto)) {
            return "E-mail inválido. Use o formato nome@dominio.com.";
        }
        return "";
    }

    /** Celular é opcional: só valida se a pessoa digitou algo. */
    public static String validarCelular(String valor) {
        String digitos = valor == null ? "" : valor.replaceAll("\\D", "");
        if (digitos.isEmpty()) {
            return "";
        }
        if (digitos.length() < 10 || digitos.length() > 11) {
            return "Celular incompleto. Use DDD e número, ou deixe em branco.";
        }
        return "";
    }
}
