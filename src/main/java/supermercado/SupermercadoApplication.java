package supermercado;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada do Mercado do Bairro (versão web do Sistema de Supermercado).
 *
 * <p>Ao rodar (botão Run do NetBeans ou {@code mvn spring-boot:run}), o Spring
 * sobe um servidor Tomcat embutido em http://localhost:8080 que entrega as
 * páginas do front-end e a API REST.</p>
 *
 * <p>{@code proxyBeanMethods = false}: as classes de configuração não
 * precisam de proxies gerados em tempo de execução, o que deixa a
 * inicialização mais leve.</p>
 */
@SpringBootApplication(proxyBeanMethods = false)
public class SupermercadoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupermercadoApplication.class, args);
    }
}
