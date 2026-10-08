-- ---------------------------------------------------------------------------
-- Mercado do Bairro - estrutura do banco de dados
-- Roda a cada inicialização (CREATE TABLE IF NOT EXISTS não apaga dados).
-- O mesmo script funciona no H2 (modo MySQL) e no MySQL 8 ou superior.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS categoria (
    nome   VARCHAR(40) NOT NULL,
    ordem  INT         NOT NULL,
    CONSTRAINT pk_categoria PRIMARY KEY (nome)
);

CREATE TABLE IF NOT EXISTS produto (
    codigo     INT           NOT NULL,
    nome       VARCHAR(100)  NOT NULL,
    unidade    VARCHAR(40)   NOT NULL,
    categoria  VARCHAR(40)   NOT NULL,
    preco      DECIMAL(10,2) NOT NULL,
    icone      VARCHAR(16)   NOT NULL,
    CONSTRAINT pk_produto PRIMARY KEY (codigo),
    CONSTRAINT fk_produto_categoria FOREIGN KEY (categoria) REFERENCES categoria (nome),
    CONSTRAINT ck_produto_preco CHECK (preco >= 0)
);

CREATE TABLE IF NOT EXISTS cliente (
    cpf      CHAR(11)     NOT NULL,
    nome     VARCHAR(120) NOT NULL,
    email    VARCHAR(120) NOT NULL,
    celular  VARCHAR(11),
    CONSTRAINT pk_cliente PRIMARY KEY (cpf)
);

CREATE TABLE IF NOT EXISTS pedido (
    numero              INT           NOT NULL,
    data_hora           DATETIME      NOT NULL,
    cliente_cpf         CHAR(11)      NOT NULL,
    -- cópia dos dados do cliente no momento da compra (o cadastro pode mudar depois)
    cliente_nome        VARCHAR(120)  NOT NULL,
    cliente_email       VARCHAR(120)  NOT NULL,
    cliente_celular     VARCHAR(11),
    subtotal            DECIMAL(10,2) NOT NULL,
    desconto            DECIMAL(10,2) NOT NULL,
    descricao_desconto  VARCHAR(80)   NOT NULL,
    total               DECIMAL(10,2) NOT NULL,
    situacao            VARCHAR(10)   NOT NULL,
    CONSTRAINT pk_pedido PRIMARY KEY (numero),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_cpf) REFERENCES cliente (cpf),
    CONSTRAINT ck_pedido_situacao CHECK (situacao IN ('FECHADO', 'CANCELADO'))
);

CREATE TABLE IF NOT EXISTS item_pedido (
    pedido_numero   INT           NOT NULL,
    linha           INT           NOT NULL,
    produto_codigo  INT           NOT NULL,
    nome_produto    VARCHAR(100)  NOT NULL,
    unidade         VARCHAR(40)   NOT NULL,
    preco_unitario  DECIMAL(10,2) NOT NULL,
    quantidade      INT           NOT NULL,
    CONSTRAINT pk_item_pedido PRIMARY KEY (pedido_numero, linha),
    CONSTRAINT fk_item_pedido FOREIGN KEY (pedido_numero) REFERENCES pedido (numero),
    CONSTRAINT fk_item_produto FOREIGN KEY (produto_codigo) REFERENCES produto (codigo),
    CONSTRAINT ck_item_quantidade CHECK (quantidade BETWEEN 1 AND 99)
);

-- Numeração dos pedidos (ver PedidoDAOJdbc.proximoNumero)
CREATE TABLE IF NOT EXISTS contador (
    nome   VARCHAR(30) NOT NULL,
    valor  INT         NOT NULL,
    CONSTRAINT pk_contador PRIMARY KEY (nome)
);
