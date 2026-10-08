/*
 * pagina-produtos.js — vitrine de produtos (index.html):
 * catálogo vindo do banco de dados pela API, busca enquanto digita,
 * filtro por categoria e "Adicionar" com validação da quantidade.
 */
(function () {
  "use strict";

  var u = Mercado.util;
  var api = Mercado.api;
  var validacao = Mercado.validacao;

  var gondola = document.getElementById("gondola");
  var campoBusca = document.getElementById("busca");
  var listaCategorias = document.getElementById("categorias");
  var contagem = document.getElementById("contagem");
  var semResultado = document.getElementById("sem-resultado");

  var produtos = [];
  var categorias = [];
  var categoriaAtual = "";

  /** "Café" e "cafe" encontram o mesmo produto */
  function normalizar(texto) {
    return texto.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().trim();
  }

  function criarBotaoCategoria(rotulo, valor) {
    var botao = u.criar("button", {
      type: "button",
      className: "categoria",
      "aria-pressed": String(valor === categoriaAtual),
      dados: { categoria: valor }
    }, rotulo);
    return u.criar("li", null, botao);
  }

  function montarCategorias() {
    listaCategorias.replaceChildren(criarBotaoCategoria("Todas", ""));
    categorias.forEach(function (categoria) {
      listaCategorias.append(criarBotaoCategoria(categoria, categoria));
    });
  }

  function mostrarErro(cartao, mensagem) {
    cartao.querySelector(".mensagem-erro").textContent = mensagem;
    cartao.querySelector(".quantidade").classList.toggle("quantidade--invalida", Boolean(mensagem));
    cartao.querySelector("input").setAttribute("aria-invalid", String(Boolean(mensagem)));
  }

  function criarCartao(produto) {
    var idQuantidade = "qtd-" + produto.codigo;
    var idErro = "erro-" + produto.codigo;

    var entrada = u.criar("input", {
      type: "number",
      id: idQuantidade,
      name: "quantidade",
      value: "1",
      min: "1",
      max: "99",
      inputMode: "numeric",
      "aria-describedby": idErro
    });

    var botaoAdicionar = u.criar("button", { type: "submit", className: "botao botao--principal" }, "Adicionar");
    var formulario = u.criar("form", { className: "produto__compra", noValidate: true },
      u.criar("label", { className: "somente-leitor", "for": idQuantidade }, "Quantidade de " + produto.nome),
      u.criar("div", { className: "quantidade" },
        u.criar("button", { type: "button", "aria-label": "Diminuir quantidade", dados: { passo: "-1" } }, "−"),
        entrada,
        u.criar("button", { type: "button", "aria-label": "Aumentar quantidade", dados: { passo: "1" } }, "+")
      ),
      botaoAdicionar
    );

    var cartao = u.criar("li", { className: "produto", dados: { codigo: produto.codigo } },
      u.criar("div", { className: "produto__vitrine", "aria-hidden": "true" }, produto.icone),
      u.criar("div", { className: "produto__info" },
        u.criar("h2", { className: "produto__nome" }, produto.nome),
        u.criar("p", { className: "produto__unidade" }, produto.unidade),
        u.etiqueta(u.centavos(produto.preco))
      ),
      formulario,
      u.criar("p", { className: "mensagem-erro", id: idErro, role: "alert" })
    );

    // botões − e +
    formulario.querySelectorAll("[data-passo]").forEach(function (botao) {
      botao.addEventListener("click", function () {
        var atual = parseInt(entrada.value, 10) || 0;
        var novo = Math.min(99, Math.max(1, atual + Number(botao.dataset.passo)));
        entrada.value = String(novo);
        mostrarErro(cartao, "");
      });
    });

    entrada.addEventListener("input", function () {
      mostrarErro(cartao, "");
    });

    formulario.addEventListener("submit", function (evento) {
      evento.preventDefault();
      var erro = validacao.quantidade(entrada.value);
      if (erro) {
        mostrarErro(cartao, erro);
        entrada.focus();
        return;
      }
      botaoAdicionar.disabled = true;
      api.carrinho.adicionar(produto.codigo, entrada.value)
        .then(function (carrinho) {
          var item = carrinho.itens.find(function (i) { return i.codigo === produto.codigo; });
          var noCarrinho = item ? item.quantidade : 0;
          mostrarErro(cartao, "");
          entrada.value = "1";
          Mercado.comum.atualizarContador(carrinho.quantidadeDeUnidades, true);
          u.avisar(produto.nome + " no carrinho: " + noCarrinho + (noCarrinho === 1 ? " unidade." : " unidades."));
        })
        .catch(function (falha) {
          mostrarErro(cartao, falha.message);
        })
        .finally(function () {
          botaoAdicionar.disabled = false;
        });
    });

    return cartao;
  }

  function filtrar() {
    var termo = normalizar(campoBusca.value);
    var visiveis = produtos.filter(function (p) {
      var naCategoria = !categoriaAtual || p.categoria === categoriaAtual;
      var noTermo = !termo || normalizar(p.nome + " " + p.categoria).indexOf(termo) !== -1;
      return naCategoria && noTermo;
    });

    gondola.replaceChildren.apply(gondola, visiveis.map(criarCartao));
    semResultado.hidden = visiveis.length > 0;
    contagem.textContent = visiveis.length === 1 ? "1 produto" : visiveis.length + " produtos";
  }

  listaCategorias.addEventListener("click", function (evento) {
    var botao = evento.target.closest("[data-categoria]");
    if (!botao) return;
    categoriaAtual = botao.dataset.categoria;
    listaCategorias.querySelectorAll("[data-categoria]").forEach(function (b) {
      b.setAttribute("aria-pressed", String(b === botao));
    });
    filtrar();
  });

  campoBusca.addEventListener("input", filtrar);

  document.getElementById("form-busca").addEventListener("submit", function (evento) {
    evento.preventDefault();
  });

  document.getElementById("limpar-filtros").addEventListener("click", function () {
    campoBusca.value = "";
    categoriaAtual = "";
    montarCategorias();
    filtrar();
    campoBusca.focus();
  });

  contagem.textContent = "Carregando produtos...";
  Promise.all([api.produtos(), api.categorias()])
    .then(function (respostas) {
      produtos = respostas[0];
      categorias = respostas[1];
      montarCategorias();
      filtrar();
    })
    .catch(function (falha) {
      contagem.textContent = "";
      gondola.replaceChildren(u.criar("li", { className: "vazio" },
        u.criar("h2", null, "Não foi possível carregar os produtos"),
        u.criar("p", null, falha.message)
      ));
    });
})();
