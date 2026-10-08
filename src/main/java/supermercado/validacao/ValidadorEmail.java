package supermercado.validacao;

import java.util.regex.Pattern;

/** Validação simples do formato de e-mail (usuario@dominio.ext). */
public final class ValidadorEmail {

    private static final Pattern FORMATO_EMAIL =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private ValidadorEmail() {
        // classe utilitária: não deve ser instanciada
    }

    public static boolean isValido(String email) {
        return email != null && FORMATO_EMAIL.matcher(email.trim()).matches();
    }
}
