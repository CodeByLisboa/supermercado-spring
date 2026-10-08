package supermercado.validacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validação do formulário de identificação no servidor (CT-U49 a CT-U54).
 * As mensagens precisam ser iguais às do validacao.js do front-end.
 */
class ValidadorClienteTest {

    @Test
    @DisplayName("CT-U49 - dados corretos não geram erros")
    void dadosCorretos_semErros() {
        assertTrue(ValidadorCliente.validar("João da Silva", "529.982.247-25", "joao@email.com",
                "(51) 99999-8888", true).isEmpty());
    }

    @Test
    @DisplayName("CT-U50 - formulário vazio aponta nome, CPF, e-mail e autorização, nessa ordem")
    void formularioVazio_apontaOsQuatroCampos() {
        Map<String, String> erros = ValidadorCliente.validar("", "", "", "", false);

        assertEquals(List.of("nome", "cpf", "email", "consentimento"), List.copyOf(erros.keySet()));
    }

    @Test
    @DisplayName("CT-U51 - nome sem sobrenome ou com números é recusado")
    void nome_regras() {
        assertEquals("Informe nome e sobrenome.", ValidadorCliente.validarNome("João"));
        assertEquals("Use apenas letras e espaços no nome.", ValidadorCliente.validarNome("João 2"));
        assertEquals("Use apenas letras e espaços no nome.", ValidadorCliente.validarNome("<script>alert(1)</script>"));
    }

    @Test
    @DisplayName("CT-U52 - CPF incompleto diz quantos números faltam; dígito errado é inválido")
    void cpf_regras() {
        assertEquals("O CPF tem 11 números. Faltam 2.", ValidadorCliente.validarCpf("529.982.247"));
        assertEquals("CPF inválido. Confira os números digitados.", ValidadorCliente.validarCpf("123.456.789-00"));
    }

    @Test
    @DisplayName("CT-U53 - e-mail sem @ é recusado")
    void email_semArroba() {
        assertEquals("E-mail inválido. Use o formato nome@dominio.com.", ValidadorCliente.validarEmail("joao.email.com"));
    }

    @Test
    @DisplayName("CT-U54 - celular é opcional, mas incompleto é recusado")
    void celular_opcional() {
        assertEquals("", ValidadorCliente.validarCelular(""));
        assertEquals("Celular incompleto. Use DDD e número, ou deixe em branco.",
                ValidadorCliente.validarCelular("(51) 9999"));
    }
}
