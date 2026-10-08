/*
 * util.js — funções de apoio usadas por todas as páginas:
 * formatação de moeda e data, avisos e criação segura de elementos
 * (sempre com textContent, nunca innerHTML com dados digitados pelo
 * usuário, para evitar XSS).
 *
 * Na etapa 8 este arquivo também guardava o carrinho no localStorage.
 * Agora o carrinho e os pedidos ficam no servidor (ver api.js).
 */
window.Mercado = window.Mercado || {};

Mercado.util = (function () {
  "use strict";

  var formatoMoeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
  var formatoData = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

  /**
   * A API manda valores em reais (47.97). As funções de exibição trabalham
   * em centavos inteiros (4797), como na etapa 8, para não somar números
   * quebrados no navegador.
   */
  function centavos(reais) {
    return Math.round(Number(reais) * 100);
  }

  /** 4797 -> "R$ 47,97" */
  function moeda(centavos) {
    return formatoMoeda.format(centavos / 100);
  }

  /** 2199 -> { inteiro: "21", centavos: "99" } para montar a etiqueta de preço */
  function partesDoPreco(centavos) {
    var inteiro = Math.floor(centavos / 100);
    return {
      inteiro: inteiro.toLocaleString("pt-BR"),
      centavos: String(centavos % 100).padStart(2, "0")
    };
  }

  function data(iso) {
    return formatoData.format(new Date(iso));
  }

  /** número do pedido com 6 dígitos: 7 -> "000007" */
  function numeroPedido(numero) {
    return String(numero).padStart(6, "0");
  }

  function parametro(nome) {
    return new URLSearchParams(window.location.search).get(nome);
  }

  /** Cria um elemento: criar("p", { className: "x" }, "texto", outroElemento) */
  function criar(tag, propriedades) {
    var elemento = document.createElement(tag);
    var props = propriedades || {};
    Object.keys(props).forEach(function (chave) {
      if (chave === "dados") {
        Object.keys(props.dados).forEach(function (d) { elemento.dataset[d] = props.dados[d]; });
      } else if (chave.indexOf("aria-") === 0 || chave === "role" || chave === "for") {
        elemento.setAttribute(chave, props[chave]);
      } else {
        elemento[chave] = props[chave];
      }
    });
    for (var i = 2; i < arguments.length; i++) {
      var filho = arguments[i];
      if (filho === null || filho === undefined) continue;
      elemento.append(typeof filho === "string" ? document.createTextNode(filho) : filho);
    }
    return elemento;
  }

  /** Monta a etiqueta amarela de preço */
  function etiqueta(centavos, grande) {
    var partes = partesDoPreco(centavos);
    return criar("span", { className: "etiqueta" + (grande ? " etiqueta--grande" : ""), "aria-label": moeda(centavos) },
      criar("span", { className: "etiqueta__moeda", "aria-hidden": "true" }, "R$"),
      criar("span", { className: "etiqueta__inteiro", "aria-hidden": "true" }, partes.inteiro),
      criar("span", { className: "etiqueta__centavos", "aria-hidden": "true" }, "," + partes.centavos)
    );
  }

  /** Aviso rápido no canto da tela (lido por leitores de tela) */
  function avisar(mensagem, tipo) {
    var area = document.getElementById("avisos");
    if (!area) return;
    var aviso = criar("div", { className: "aviso" + (tipo === "erro" ? " aviso--erro" : "") }, mensagem);
    area.append(aviso);
    window.setTimeout(function () { aviso.remove(); }, 3500);
  }

  return {
    moeda: moeda,
    partesDoPreco: partesDoPreco,
    data: data,
    numeroPedido: numeroPedido,
    centavos: centavos,
    parametro: parametro,
    criar: criar,
    etiqueta: etiqueta,
    avisar: avisar
  };
})();
