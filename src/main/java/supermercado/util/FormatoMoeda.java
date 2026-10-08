package supermercado.util;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Formata valores em reais no padrão brasileiro (R$ 1.234,56).
 *
 * <p>Na versão anterior o valor era formatado com {@code String.format("%.2f")},
 * que usa o idioma do computador: o mesmo programa mostrava "47,97" numa
 * máquina em português e "47.97" numa em inglês. Aqui o padrão é fixo.</p>
 */
public final class FormatoMoeda {

    private static final Locale PORTUGUES_BRASIL = Locale.forLanguageTag("pt-BR");

    private FormatoMoeda() {
        // classe utilitária: não deve ser instanciada
    }

    public static String formatar(BigDecimal valor) {
        return "R$ " + String.format(PORTUGUES_BRASIL, "%,.2f", valor);
    }
}
