# Mercado do Bairro: Sistema de Supermercado com Spring

Projeto Integrador (Senac, Técnico em Desenvolvimento de Sistemas), etapa 9:
versão web completa do Sistema de Supermercado, com back-end em **Spring REST**,
banco de dados acessado por **JDBC** e o front-end criado na etapa 8.

**Aluno:** Gabriel Ramos Lisboa

| Camada | Tecnologia |
|---|---|
| Back-end | Java 17, Spring Boot 3.5 (Spring MVC REST, Spring JDBC, Spring Security) |
| Banco de dados | H2 em arquivo (padrão, não precisa instalar) ou MySQL (perfil `mysql`) |
| Front-end | HTML, CSS e JavaScript da etapa 8, servidos pelo próprio Spring |
| Testes | JUnit 5, MockMvc e testes no navegador com Playwright |
| Bugs | GitHub Issues deste repositório |

## Como executar no NetBeans

1. **File > Open Project** e escolha a pasta `SupermercadoSpring` (o NetBeans reconhece o `pom.xml`).
2. Na primeira vez o NetBeans baixa as bibliotecas do Maven (precisa de internet).
3. Clique em **Run Project** (F6). O servidor sobe em <http://localhost:8080>.
4. Área do operador: login `operador`, senha `mercado123` (configurável no `application.properties`).

Pela linha de comando: `mvn spring-boot:run`.

O banco H2 fica na pasta `dados/` do projeto. Para começar do zero, pare o servidor e apague essa pasta.
Com o operador logado, o console do banco abre em <http://localhost:8080/h2-console>
(JDBC URL `jdbc:h2:file:./dados/mercado;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE`, usuário `sa`, senha vazia).

### Usando MySQL

O banco `mercado_do_bairro` e as tabelas são criados sozinhos na primeira execução.

- Linha de comando: `MYSQL_SENHA=suaSenha mvn spring-boot:run -Dspring-boot.run.profiles=mysql`
- NetBeans: botão direito no projeto > **Properties > Actions > Run Project**, em
  **Set Properties** escreva `spring-boot.run.profiles=mysql` e, em **Env**, `MYSQL_SENHA=suaSenha`.

Endereço, usuário e senha ficam em `src/main/resources/application-mysql.properties`.

### Trocando a promoção

Em `application.properties`, `mercado.promocao.tipo` aceita `valor-minimo` (R$ 10,00 a partir de R$ 100,00),
`percentual` (10%) ou `nenhuma`. É o padrão Strategy da etapa 6 (`PoliticaDeDesconto`) escolhido por configuração.

## Testes

- **NetBeans:** botão direito no projeto > **Test** (Alt+F6). Pela linha de comando: `mvn test`.
- 92 testes JUnit 5:
  - `CT-U01` a `CT-U54`: regras de negócio (os 38 testes da etapa 7 migrados para JUnit 5, com os mesmos nomes, e 16 novos);
  - `CT-I01` a `CT-I11`: banco de dados com JDBC, pedidos simultâneos e carrinho simultâneo;
  - `CT-A01` a `CT-A14`: API REST com MockMvc;
  - `CT-S01` a `CT-S09`: login, permissões e proteção contra CSRF.
- `testes-navegador/`: casos de teste manuais do plano (CT-M01 a CT-M21) executados num navegador
  de verdade contra o servidor rodando (instruções no início de `casos_de_teste.py`).

## API REST

| Método e caminho | O que faz | Acesso |
|---|---|---|
| `GET /api/produtos` (`?categoria=`) | Catálogo | livre |
| `GET /api/categorias` | Categorias na ordem da vitrine | livre |
| `GET /api/promocao` | Promoção vigente | livre |
| `GET /api/carrinho` | Carrinho do visitante com os totais | livre |
| `POST /api/carrinho/itens` | Adiciona `{codigo, quantidade}` | livre |
| `PUT /api/carrinho/itens/{codigo}` | Troca a quantidade `{quantidade}` | livre |
| `DELETE /api/carrinho/itens/{codigo}` | Remove o produto | livre |
| `POST /api/pedidos` | Fecha o pedido `{nome, cpf, email, celular, consentimento}` | livre |
| `GET /api/pedidos/{numero}` | Cupom do pedido | quem fechou o pedido ou operador |
| `GET /api/pedidos` (`?situacao=`) | Lista de pedidos | operador |
| `GET /api/pedidos/resumo` | Pedidos fechados, cancelados e total vendido | operador |
| `POST /api/pedidos/{numero}/cancelamento` | Cancela o pedido | operador |
| `POST /api/pedidos/exemplos` | Cria três pedidos de demonstração | operador |
| `POST /api/login`, `POST /api/logout`, `GET /api/sessao` | Login do operador | livre |

Erros voltam em JSON com `status` e `mensagem` (e `campos` nos erros de formulário):
400 dados inválidos, 401 sem login, 403 sem permissão ou sem token CSRF, 404 não encontrado,
422 regra de negócio (ex.: carrinho vazio, pedido já cancelado).

## Organização do código

```
src/main/java/supermercado
├── SupermercadoApplication.java   -> inicia o Spring Boot (Tomcat embutido)
├── modelo/       -> Produto, ItemCarrinho, CarrinhoDeCompras, Cliente, Pedido (etapa 6)
├── desconto/     -> PoliticaDeDesconto e as três estratégias (etapa 6)
├── validacao/    -> ValidadorCpf, ValidadorEmail, ValidadorCliente
├── excecao/      -> RegraNegocioException e exceções específicas
├── servico/      -> PedidoService (etapa 6) e LojaService (casos de uso da loja web)
├── dao/          -> PedidoDAO, PedidoDAOMemoria (testes), PedidoDAOJdbc e ProdutoDAOJdbc
├── config/       -> monta as regras de negócio como beans do Spring
├── seguranca/    -> Spring Security: login do operador, permissões e CSRF
└── web/          -> controladores REST, DTOs e tratamento de erros
src/main/resources
├── application.properties, application-mysql.properties
├── schema.sql, data.sql            -> tabelas e catálogo
└── static/                          -> páginas, CSS e JavaScript (front-end da etapa 8)
```

O `PedidoService` da etapa 6 não mudou de comportamento: ele recebe um `PedidoDAO` pelo
construtor e passou a gravar no banco só porque o Spring injeta o `PedidoDAOJdbc` no lugar do
`PedidoDAOMemoria` (princípio da inversão de dependência).

## Banco de dados

`categoria`, `produto`, `cliente`, `pedido` (com cópia do nome e e-mail do cliente no momento da
compra), `item_pedido` (com nome e preço do produto no momento da compra), `contador` (numeração
dos pedidos, segura com pedidos simultâneos) e `operador` (senha em hash BCrypt).
