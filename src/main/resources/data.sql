-- ---------------------------------------------------------------------------
-- Mercado do Bairro - dados iniciais (catálogo da etapa 8)
-- INSERT IGNORE: se o registro já existe, nada muda. Assim o script pode
-- rodar a cada inicialização sem duplicar dados.
-- ---------------------------------------------------------------------------

INSERT IGNORE INTO categoria (nome, ordem) VALUES
    ('Mercearia', 1),
    ('Carnes', 2),
    ('Hortifrúti', 3),
    ('Frios e laticínios', 4),
    ('Padaria', 5),
    ('Bebidas', 6),
    ('Limpeza', 7);

INSERT IGNORE INTO produto (codigo, nome, unidade, categoria, preco, icone) VALUES
    (1,  'Arroz branco tipo 1',         'Pacote 1 kg',         'Mercearia',          3.99,  '🍚'),
    (2,  'Filé de frango sassami',      'Bandeja 1 kg',        'Carnes',             21.99, '🍗'),
    (3,  'Feijão carioca',              'Pacote 1 kg',         'Mercearia',          8.50,  '🥫'),
    (4,  'Leite integral',              'Caixa 1 L',           'Frios e laticínios', 4.79,  '🥛'),
    (5,  'Café torrado e moído',        'Pacote 500 g',        'Mercearia',          17.90, '☕'),
    (6,  'Banana prata',                'Preço do kg',         'Hortifrúti',         6.99,  '🍌'),
    (7,  'Tomate italiano',             'Preço do kg',         'Hortifrúti',         8.49,  '🍅'),
    (8,  'Pão de forma integral',       'Pacote 400 g',        'Padaria',            8.99,  '🍞'),
    (9,  'Ovos brancos',                'Bandeja com 12',      'Frios e laticínios', 11.90, '🥚'),
    (10, 'Queijo muçarela fatiado',     'Pacote 200 g',        'Frios e laticínios', 9.49,  '🧀'),
    (11, 'Suco de uva integral',        'Garrafa 1 L',         'Bebidas',            12.90, '🧃'),
    (12, 'Detergente líquido',          'Frasco 500 ml',       'Limpeza',            2.79,  '🧴'),
    (13, 'Papel higiênico folha dupla', 'Pacote com 12 rolos', 'Limpeza',            19.90, '🧻'),
    (14, 'Bala de goma',                'Unidade',             'Mercearia',          0.10,  '🍬');

INSERT IGNORE INTO contador (nome, valor) VALUES ('pedido', 0);
