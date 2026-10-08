/*
 * pagina-pedido.js — mostra o pedido gravado no banco como um cupom de
 * supermercado. O CPF não aparece no cupom (proteção de dados pessoais,
 * LGPD); a API já manda o CPF mascarado.
 */
(function () {
  "use strict";

  var u = Mercado.util;
  var api = Mercado.api;
  var area = document.getElementById("confirmacao");
  var numero = u.parametro("numero");
  var doOperador = u.parametro("origem") === "operador";

  if (doOperador) {
    document.getElementById("etapas").hidden = true;
  }

  function naoEncontrado() {
    document.getElementById("etapas").hidden = true;
    area.replaceChildren(u.criar("div", { className: "vazio" },
      u.criar("div", { className: "vazio__icone", "aria-hidden": "true" }, "🧾"),
      u.criar("h1", null, "Pedido não encontrado"),
      u.criar("p", null, "Confira o número do pedido. O cupom só abre no navegador em que a compra foi feita ou na área do operador."),
      u.criar("a", { className: "botao botao--principal", href: "index.html" }, "Ir para os produtos")
    ));
  }

  function linha(esquerda, direita, classe) {
    return u.criar("div", { className: "cupom__linha" + (classe ? " " + classe : "") },
      u.criar("span", null, esquerda), u.criar("span", null, direita));
  }

  function mostrar(pedido) {
    var cancelado = pedido.situacao === "CANCELADO";
    var desconto = u.centavos(pedido.desconto);
    var cupom = u.criar("article", { className: "cupom", "aria-label": "Cupom do pedido " + u.numeroPedido(pedido.numero) },
      u.criar("p", { className: "cupom__loja" }, "Mercado do Bairro"),
      u.criar("p", { className: "cupom__numero" }, "Pedido nº " + u.numeroPedido(pedido.numero)),
      u.criar("p", null, u.data(pedido.dataHora)),
      u.criar("p", null, "Cliente: " + pedido.cliente.nome),
      u.criar("p", null, "E-mail: " + pedido.cliente.email),
      u.criar("hr")
    );

    pedido.itens.forEach(function (item) {
      cupom.append(
        linha(item.quantidade + " x " + item.nome, u.moeda(u.centavos(item.subtotal)).replace("R$", "").trim()),
        u.criar("p", { className: "cupom__detalhe" }, item.unidade + ", " + u.moeda(u.centavos(item.precoUnitario)) + " cada")
      );
    });

    cupom.append(
      u.criar("hr"),
      linha("Subtotal", u.moeda(u.centavos(pedido.subtotal))),
      linha("Desconto", desconto > 0 ? "-" + u.moeda(desconto) : u.moeda(0))
    );
    if (desconto > 0) {
      cupom.append(u.criar("p", { className: "cupom__detalhe" }, "Promoção: " + pedido.descricaoDesconto));
    }
    cupom.append(
      linha("TOTAL", u.moeda(u.centavos(pedido.total)), "cupom__total"),
      u.criar("hr"),
      u.criar("p", null, "Situação: " + (cancelado ? "Cancelado" : "Fechado"))
    );

    var acoes = u.criar("div", { className: "acoes-centro" });
    var imprimir = u.criar("button", { type: "button", className: "botao" }, "Imprimir cupom");
    imprimir.addEventListener("click", function () { window.print(); });
    acoes.append(imprimir);
    acoes.append(doOperador
      ? u.criar("a", { className: "botao botao--principal", href: "pedidos.html" }, "Voltar para os pedidos")
      : u.criar("a", { className: "botao botao--principal", href: "index.html" }, "Fazer nova compra"));

    area.replaceChildren(
      u.criar("h1", null, cancelado ? "Pedido cancelado" : "Pedido confirmado"),
      u.criar("p", null, cancelado
        ? "Este pedido foi cancelado e não será separado."
        : "Guarde o número do pedido para consultar depois."),
      cupom,
      acoes
    );
    document.title = "Pedido " + u.numeroPedido(pedido.numero) + " | Mercado do Bairro";
  }

  if (!numero || !/^\d+$/.test(numero)) {
    naoEncontrado();
    return;
  }

  api.pedidos.buscar(numero)
    .then(mostrar)
    .catch(function (falha) {
      if (falha.status === 404) {
        naoEncontrado();
      } else {
        u.avisar(falha.message, "erro");
      }
    });
})();
