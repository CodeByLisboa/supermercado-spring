package supermercado.modelo;

import supermercado.validacao.ValidadorCpf;
import supermercado.validacao.ValidadorEmail;

/**
 * Cliente do supermercado.
 *
 * <p>O objeto só é criado em estado válido: nome preenchido, CPF com dígitos
 * verificadores corretos e e-mail em formato válido. As regras ficam em
 * classes próprias ({@link ValidadorCpf} e {@link ValidadorEmail}) e são as
 * mesmas usadas pelo formulário web.</p>
 *
 * <p>O celular é opcional e foi incluído na versão web, que já tinha esse
 * campo no formulário de identificação.</p>
 */
public class Cliente {

    private final String nome;
    private final String cpf;
    private final String email;
    private final String celular;

    public Cliente(String nome, String cpf, String email) {
        this(nome, cpf, email, null);
    }

    public Cliente(String nome, String cpf, String email, String celular) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do cliente é obrigatório.");
        }
        if (!ValidadorCpf.isValido(cpf)) {
            throw new IllegalArgumentException("CPF inválido: " + cpf);
        }
        if (!ValidadorEmail.isValido(email)) {
            throw new IllegalArgumentException("E-mail inválido: " + email);
        }
        String digitosDoCelular = celular == null ? "" : celular.replaceAll("\\D", "");
        if (!digitosDoCelular.isEmpty() && (digitosDoCelular.length() < 10 || digitosDoCelular.length() > 11)) {
            throw new IllegalArgumentException("Celular inválido: " + celular);
        }
        this.nome = nome.trim().replaceAll("\\s+", " ");
        this.cpf = ValidadorCpf.somenteDigitos(cpf);
        this.email = email.trim().toLowerCase();
        this.celular = digitosDoCelular.isEmpty() ? null : digitosDoCelular;
    }

    public String getNome() {
        return nome;
    }

    /** CPF armazenado apenas com os 11 dígitos, sem pontos e traço. */
    public String getCpf() {
        return cpf;
    }

    /** CPF com os dígitos do meio escondidos, ex.: ***.982.247-**, para telas e cupons. */
    public String getCpfMascarado() {
        return "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
    }

    public String getEmail() {
        return email;
    }

    /** Somente dígitos (DDD + número) ou {@code null} quando não informado. */
    public String getCelular() {
        return celular;
    }
}
