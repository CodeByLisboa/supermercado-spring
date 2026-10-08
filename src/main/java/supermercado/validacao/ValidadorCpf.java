package supermercado.validacao;

/**
 * Validação de CPF pelo cálculo dos dígitos verificadores.
 *
 * <p>Classe utilitária com uma única responsabilidade. Não depende de tela
 * nem de console, então serve tanto para o sistema desktop quanto para o web.</p>
 */
public final class ValidadorCpf {

    private static final int TAMANHO_CPF = 11;

    private ValidadorCpf() {
        // classe utilitária: não deve ser instanciada
    }

    /** Remove pontos, traços e espaços, deixando apenas os dígitos. */
    public static String somenteDigitos(String cpf) {
        return (cpf == null) ? "" : cpf.replaceAll("\\D", "");
    }

    /** Aceita o CPF com ou sem máscara (ex.: "529.982.247-25" ou "52998224725"). */
    public static boolean isValido(String cpf) {
        String digitos = somenteDigitos(cpf);
        if (digitos.length() != TAMANHO_CPF || todosOsDigitosIguais(digitos)) {
            return false;
        }
        int primeiroDigito = calcularDigitoVerificador(digitos, 9);
        int segundoDigito = calcularDigitoVerificador(digitos, 10);
        return primeiroDigito == valorDoDigito(digitos, 9)
                && segundoDigito == valorDoDigito(digitos, 10);
    }

    /**
     * Calcula um dígito verificador usando os primeiros {@code quantidadeDeDigitos}
     * dígitos, com pesos decrescentes (10..2 para o 1º dígito e 11..2 para o 2º).
     */
    private static int calcularDigitoVerificador(String digitos, int quantidadeDeDigitos) {
        int soma = 0;
        int peso = quantidadeDeDigitos + 1;
        for (int i = 0; i < quantidadeDeDigitos; i++) {
            soma += valorDoDigito(digitos, i) * peso;
            peso--;
        }
        int resto = (soma * 10) % 11;
        return (resto == 10) ? 0 : resto;
    }

    private static int valorDoDigito(String digitos, int posicao) {
        return digitos.charAt(posicao) - '0';
    }

    /** CPFs como 111.111.111-11 passam na conta, mas são inválidos. */
    private static boolean todosOsDigitosIguais(String digitos) {
        for (int i = 1; i < digitos.length(); i++) {
            if (digitos.charAt(i) != digitos.charAt(0)) {
                return false;
            }
        }
        return true;
    }
}
