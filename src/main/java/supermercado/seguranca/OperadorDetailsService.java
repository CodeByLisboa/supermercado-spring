package supermercado.seguranca;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Busca o operador no banco (tabela {@code operador}) para o Spring Security
 * conferir a senha no login.
 */
@Service
public class OperadorDetailsService implements UserDetailsService {

    public static final String PAPEL_OPERADOR = "ROLE_OPERADOR";

    private final JdbcTemplate jdbc;

    public OperadorDetailsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserDetails loadUserByUsername(String login) {
        List<UserDetails> encontrados = jdbc.query(
                "SELECT login, senha_hash, ativo FROM operador WHERE login = ?",
                (linha, n) -> User.withUsername(linha.getString("login"))
                        .password(linha.getString("senha_hash"))
                        .disabled(!linha.getBoolean("ativo"))
                        .authorities(List.of(new SimpleGrantedAuthority(PAPEL_OPERADOR)))
                        .build(),
                login == null ? "" : login.trim());
        if (encontrados.isEmpty()) {
            throw new UsernameNotFoundException("Operador não encontrado.");
        }
        return encontrados.get(0);
    }

    /** Nome do operador para mostrar na tela ("Operador da loja"). */
    public String nomeDo(String login) {
        List<String> nomes = jdbc.queryForList("SELECT nome FROM operador WHERE login = ?", String.class, login);
        return nomes.isEmpty() ? login : nomes.get(0);
    }
}
