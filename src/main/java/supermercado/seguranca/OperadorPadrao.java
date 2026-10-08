package supermercado.seguranca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Na primeira execução, quando ainda não existe nenhum operador, cria o
 * operador configurado no {@code application.properties}, com a senha
 * guardada como hash BCrypt (nunca em texto puro).
 */
@Component
public class OperadorPadrao implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(OperadorPadrao.class);

    private final JdbcTemplate jdbc;
    private final PasswordEncoder codificador;
    private final String login;
    private final String nome;
    private final String senha;

    public OperadorPadrao(JdbcTemplate jdbc, PasswordEncoder codificador,
                          @Value("${mercado.operador.login:operador}") String login,
                          @Value("${mercado.operador.nome:Operador da loja}") String nome,
                          @Value("${mercado.operador.senha:mercado123}") String senha) {
        this.jdbc = jdbc;
        this.codificador = codificador;
        this.login = login;
        this.nome = nome;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        Integer existentes = jdbc.queryForObject("SELECT COUNT(*) FROM operador", Integer.class);
        if (existentes != null && existentes == 0) {
            jdbc.update("INSERT INTO operador (login, nome, senha_hash, ativo) VALUES (?, ?, ?, TRUE)",
                    login, nome, codificador.encode(senha));
            LOG.info("Operador '{}' criado para a área do operador.", login);
        }
    }
}
