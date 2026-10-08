package supermercado.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Dados do cliente usados no formulário web (CT-U44 a CT-U46). */
class ClienteTest {

    @Test
    @DisplayName("CT-U44 - celular com máscara é guardado só com os dígitos; vazio vira nulo")
    void celular_guardaSomenteDigitos() {
        Cliente comCelular = new Cliente("Maria Souza", "529.982.247-25", "maria@email.com", "(51) 99999-8888");
        Cliente semCelular = new Cliente("Maria Souza", "529.982.247-25", "maria@email.com", "  ");

        assertEquals("51999998888", comCelular.getCelular());
        assertNull(semCelular.getCelular());
    }

    @Test
    @DisplayName("CT-U45 - celular incompleto é recusado")
    void celularIncompleto_eRecusado() {
        assertThrows(IllegalArgumentException.class,
                () -> new Cliente("Maria Souza", "529.982.247-25", "maria@email.com", "(51) 9999"));
    }

    @Test
    @DisplayName("CT-U46 - CPF mascarado esconde o começo e os dígitos verificadores")
    void cpfMascarado_escondeOsDigitosDoComecoEDoFim() {
        Cliente cliente = new Cliente("Maria Souza", "52998224725", "maria@email.com");

        assertEquals("***.982.247-**", cliente.getCpfMascarado());
    }
}
