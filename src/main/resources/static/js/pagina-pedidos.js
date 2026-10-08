/*
 * pagina-pedidos.js — área do operador (só com login): lista, busca,
 * filtro por situação, resumo das vendas e cancelamento com confirmação.
 * Pedido cancelado não pode ser cancelado de novo (regra do
 * Pedido.cancelar() em Java, conferida no servidor).
 */
(function () {
  "use strict";

  var u = Mercado.util;
  var api = Mercado.api;

  var area = document.getElementById("lista-pedidos");
  var resumoLinha = document.getElementById("resumo-linha");
  var filtroTexto = document.getElementById("filtro-texto");
  var filtroSituacao = document.getElementById("filtro-situacao");
  var dialogo = document.getElementById("dialogo-cancelar");
  var pedidoEmCancelamento = null;
  var todos = [];

  /** Sem login (ou sessão expirada): vai para a tela de login e volta para cá */
  function tratarFalha(falha) {
    if (falha.status === 401) {
      window.location.href = "login.html?voltar=pedidos.html";
      return;
    }
    u.avisar(falha.message, "erro");
  }

  function normalizar(texto) {
    return String(texto).normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().trim();
  }

  function mostrarResumo(resumo) {
    resumoLinha.textContent =
      resumo.fechados + (resumo.fechados === 1 ? " pedido fechado" : " pedidos fechados") +
      " e " + resumo.cancelados + (resumo.cancelados === 1 ? " cancelado" : " cancelados") +
      ". Total vendido: " + u.moeda(u.centavos(resumo.totalVendido)) + ".";
  }

  function telaSemPedidos() {
    var exemplos = u.criar("button", { type: "button", className: "botao" }, "Criar pedidos de exemplo");
    exemplos.addEventListener("click", function () {
      exemplos.disabled = true;
      api.pedidos.criarExemplos()
        .then(function () {
          u.avisar("Três pedidos de exemplo foram criados.");
          carregar();
        })
        .catch(function (falha) {
          exemplos.disabled = false;
          tratarFalha(falha);
        });
    });
    return u.criar("div", { className: "vazio" },
      u.criar("div", { className: "vazio__icone", "aria-hidden": "true" }, "🧾"),
      u.criar("h2", null, "Nenhum pedido ainda"),
      u.criar("p", null, "Os pedidos aparecem aqui assim que um cliente confirma a compra."),
      u.criar("div", { className: "acoes-centro" },
        u.criar("a", { className: "botao botao--principal", href: "index.html" }, "Ir para os produtos"),
        exemplos
      )
    );
  }

  function celula(rotulo, conteudo, classe) {
    return u.criar("td", { className: classe || "", dados: { rotulo: rotulo } }, conteudo);
  }

  function linhaDoPedido(p) {
    var cancelado = p.situacao === "CANCELADO";
    var acoes = u.criar("div", { className: "tabela__acoes" },
      u.criar("a", { className: "botao", href: "pedido.html?numero=" + p.numero + "&origem=operador" }, "Ver cupom")
    );
    if (!cancelado) {
      var cancelar = u.criar("button", { type: "button", className: "botao botao--perigo" }, "Cancelar");
      cancelar.addEventListener("click", function () { abrirDialogo(p); });
      acoes.append(cancelar);
    }
    return u.criar("tr", null,
      celula("Nº", u.numeroPedido(p.numero)),
      celula("Data", u.data(p.dataHora)),
      celula("Cliente", p.cliente),
      celula("Itens", String(p.quantidadeDeUnidades), "numero"),
      celula("Total", u.moeda(u.centavos(p.total)), "numero"),
      celula("Situação", u.criar("span", { className: "situacao situacao--" + (cancelado ? "cancelado" : "fechado") },
        cancelado ? "Cancelado" : "Fechado")),
      celula("Ações", acoes)
    );
  }

  function desenhar() {
    if (todos.length === 0) {
      resumoLinha.textContent = "";
      area.replaceChildren(telaSemPedidos());
      return;
    }

    var termo = normalizar(filtroTexto.value);
    var situacao = filtroSituacao.value;
    var visiveis = todos.filter(function (p) {
      var bateTexto = !termo || String(p.numero) === termo.replace(/^0+/, "") || normalizar(p.cliente).indexOf(termo) !== -1;
      return bateTexto && (!situacao || p.situacao === situacao);
    });

    if (visiveis.length === 0) {
      area.replaceChildren(u.criar("div", { className: "vazio" },
        u.criar("h2", null, "Nenhum pedido encontrado"),
        u.criar("p", null, "Confira o número ou o nome digitado, ou mude a situação escolhida.")
      ));
      return;
    }

    var corpo = u.criar("tbody");
    visiveis.forEach(function (p) { corpo.append(linhaDoPedido(p)); });
    area.replaceChildren(u.criar("table", { className: "tabela" },
      u.criar("caption", { className: "somente-leitor" }, "Pedidos dos clientes"),
      u.criar("thead", null, u.criar("tr", null,
        u.criar("th", { scope: "col" }, "Nº"),
        u.criar("th", { scope: "col" }, "Data"),
        u.criar("th", { scope: "col" }, "Cliente"),
        u.criar("th", { scope: "col", className: "numero" }, "Itens"),
        u.criar("th", { scope: "col", className: "numero" }, "Total"),
        u.criar("th", { scope: "col" }, "Situação"),
        u.criar("th", { scope: "col" }, "Ações")
      )),
      corpo
    ));
  }

  /** Busca pedidos e resumo no servidor e redesenha a tela */
  function carregar() {
    return Promise.all([api.pedidos.listar(), api.pedidos.resumo()])
      .then(function (respostas) {
        todos = respostas[0];
        if (todos.length > 0) mostrarResumo(respostas[1]);
        desenhar();
      })
      .catch(tratarFalha);
  }

  /* ---------- Operador logado e botão Sair ---------- */

  function mostrarOperador(sessao) {
    var barra = document.getElementById("operador-logado");
    if (!barra || !sessao.operador) return;
    var sair = u.criar("button", { type: "button", className: "link-simples" }, "Sair");
    sair.addEventListener("click", function () {
      api.sessao.sair().then(function () {
        window.location.href = "index.html";
      }).catch(tratarFalha);
    });
    barra.replaceChildren(u.criar("span", null, "Operador: " + sessao.nome), sair);
    barra.hidden = false;
  }

  /* ---------- Confirmação de cancelamento ---------- */

  function abrirDialogo(pedido) {
    pedidoEmCancelamento = pedido;
    document.getElementById("dialogo-titulo").textContent = "Cancelar o pedido nº " + u.numeroPedido(pedido.numero) + "?";
    document.getElementById("dialogo-texto").textContent =
      "O pedido de " + pedido.cliente + ", no valor de " + u.moeda(u.centavos(pedido.total)) +
      ", será marcado como cancelado. Não dá para desfazer.";
    dialogo.showModal();
    document.getElementById("manter-pedido").focus();
  }

  document.getElementById("manter-pedido").addEventListener("click", function () {
    dialogo.close();
  });

  document.getElementById("confirmar-cancelamento").addEventListener("click", function () {
    var numero = pedidoEmCancelamento.numero;
    dialogo.close();
    api.pedidos.cancelar(numero)
      .then(function () {
        u.avisar("Pedido nº " + u.numeroPedido(numero) + " cancelado.");
      })
      .catch(tratarFalha)
      .then(carregar);
  });

  filtroTexto.addEventListener("input", desenhar);
  filtroSituacao.addEventListener("change", desenhar);
  document.getElementById("form-filtros").addEventListener("submit", function (evento) {
    evento.preventDefault();
  });

  api.sessao.ver().then(mostrarOperador).catch(function () {});
  carregar();
})();
