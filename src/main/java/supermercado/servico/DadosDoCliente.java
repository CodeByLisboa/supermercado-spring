package supermercado.servico;

/** O que a pessoa digitou no formulário de identificação, ainda sem validação. */
public record DadosDoCliente(String nome, String cpf, String email, String celular, boolean consentimento) {
}
