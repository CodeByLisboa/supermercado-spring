/*
 * comum.js — comportamento presente em todas as páginas:
 * contador de itens do carrinho no topo e faixa da promoção vigente,
 * os dois vindos do servidor.
 */
window.Mercado = window.Mercado || {};

Mercado.comum = (function () {
  "use strict";

  var api = Mercado.api;

  /** Mostra a quantidade de unidades no ícone do carrinho */
  function atualizarContador(quantidade, animar) {
    document.querySelectorAll("[data-contador-carrinho]").forEach(function (elemento) {
      elemento.textContent = String(quantidade);
      if (animar) {
        elemento.classList.remove("contador--pulo");
        void elemento.offsetWidth; // reinicia a animação
        elemento.classList.add("contador--pulo");
      }
    });
  }

  /** Faixa vermelha: "R$ 10,00 de desconto nas compras a partir de R$ 100,00" */
  function mostrarPromocao(promocao) {
    var faixa = document.querySelector(".faixa-oferta");
    if (!faixa) return;
    if (!promocao || !promocao.chamada) {
      faixa.hidden = true;
      return;
    }
    var corte = promocao.chamada.indexOf(" de desconto");
    if (corte > 0) {
      corte += " de desconto".length;
      faixa.replaceChildren(
        Mercado.util.criar("strong", null, promocao.chamada.substring(0, corte)),
        promocao.chamada.substring(corte)
      );
    } else {
      faixa.textContent = promocao.chamada;
    }
  }

  /** Promoção carregada uma vez e compartilhada pelas páginas */
  var promocao = api.promocao().catch(function () { return null; });
  promocao.then(mostrarPromocao);

  var carrinhoInicial = api.carrinho.ver();
  carrinhoInicial.then(function (carrinho) {
    atualizarContador(carrinho.quantidadeDeUnidades, false);
  }, function () {
    // cada página mostra o erro do jeito dela; aqui só o contador fica como está
  });

  return {
    atualizarContador: atualizarContador,
    promocao: promocao,
    /** Carrinho lido quando a página abriu (evita buscar duas vezes) */
    carrinhoInicial: carrinhoInicial
  };
})();
