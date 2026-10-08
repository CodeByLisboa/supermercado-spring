/*
 * pagina-carrinho.js — lista os itens do carrinho (guardado no servidor),
 * permite mudar a quantidade e remover, e mostra quanto falta para ganhar
 * o desconto da promoção. Subtotal, desconto e total vêm calculados pelo
 * servidor, com as mesmas classes Java dos testes.
 */
(function () {
  "use strict";

  var u = Mercado.util;
  var api = Mercado.api;
  var validacao = Mercado.validacao;
  var area = document.getElementById("conteudo-carrinho");
  var promocao = null;

  function telaVazia() {
    return u.criar("div", { className: "vazio" },
      u.criar("div", { className: "vazio__icone", "aria-hidden": "true" }, "🛒"),
      u.criar("h2", null, "Seu carrinho está vazio"),
      u.criar("p", null, "Escolha produtos na vitrine para começar a compra."),
      u.criar("a", { className: "botao botao--principal", href: "index.html" }, "Ver produtos")
    );
  }

  function telaDeErro(mensagem) {
    return u.criar("div", { className: "vazio" },
      u.criar("h2", null, "Não foi possível abrir o carrinho"),
      u.criar("p", null, mensagem),
      u.criar("a", { className: "botao", href: "carrinho.html" }, "Tentar de novo")
    );
  }

  function linhaDoItem(item) {
    var idQuantidade = "qtd-" + item.codigo;
    var idErro = "erro-" + item.codigo;

    var entrada = u.criar("input", {
      type: "number", id: idQuantidade, value: String(item.quantidade),
      min: "1", max: "99", inputMode: "numeric", "aria-describedby": idErro
    });
    var subtotal = u.criar("p", { className: "item__subtotal" }, u.moeda(u.centavos(item.subtotal)));
    var erro = u.criar("p", { className: "mensagem-erro", id: idErro, role: "alert" });
    var seletor = u.criar("div", { className: "quantidade" },
      u.criar("button", { type: "button", "aria-label": "Diminuir quantidade de " + item.nome, dados: { passo: "-1" } }, "−"),
      entrada,
      u.criar("button", { type: "button", "aria-label": "Aumentar quantidade de " + item.nome, dados: { passo: "1" } }, "+")
    );
    var remover = u.criar("button", { type: "button", className: "link-simples" }, "Remover");

    var linha = u.criar("li", { className: "item", dados: { codigo: item.codigo } },
      u.criar("div", { className: "item__icone", "aria-hidden": "true" }, item.icone),
      u.criar("div", { className: "item__nome" },
        u.criar("h2", null, item.nome),
        u.criar("p", null, item.unidade + ", " + u.moeda(u.centavos(item.precoUnitario)) + " cada")
      ),
      subtotal,
      u.criar("div", { className: "item__acoes" },
        u.criar("label", { className: "somente-leitor", "for": idQuantidade }, "Quantidade de " + item.nome),
        seletor,
        remover,
        erro
      )
    );

    function mostrarErro(mensagem) {
      erro.textContent = mensagem;
      seletor.classList.toggle("quantidade--invalida", Boolean(mensagem));
      entrada.setAttribute("aria-invalid", String(Boolean(mensagem)));
    }

    /*
     * Uma alteração de cada vez: se a pessoa clicar no + várias vezes
     * enquanto a primeira requisição ainda está a caminho, os cliques do
     * meio são juntados e só a última quantidade é enviada depois. Assim as
     * requisições não chegam fora de ordem ao servidor e a tela e o
     * carrinho do servidor terminam com o mesmo valor.
     */
    var enviando = false;
    var pendente = null;

    function enviar(valor) {
      enviando = true;
      api.carrinho.alterar(item.codigo, valor)
        .then(function (carrinho) {
          if (pendente !== null && pendente !== valor) {
            var proximo = pendente;
            pendente = null;
            enviar(proximo);
            return;
          }
          pendente = null;
          enviando = false;
          var atualizado = carrinho.itens.find(function (i) { return i.codigo === item.codigo; });
          if (atualizado) {
            subtotal.textContent = u.moeda(u.centavos(atualizado.subtotal));
            entrada.value = String(atualizado.quantidade);
          }
          atualizarResumo(carrinho);
        })
        .catch(function (falha) {
          pendente = null;
          enviando = false;
          mostrarErro(falha.message);
        });
    }

    function aplicar(valor) {
      var mensagem = validacao.quantidade(valor);
      mostrarErro(mensagem);
      if (mensagem) return;
      if (enviando) {
        pendente = valor;
        return;
      }
      enviar(valor);
    }

    seletor.querySelectorAll("[data-passo]").forEach(function (botao) {
      botao.addEventListener("click", function () {
        var atual = parseInt(entrada.value, 10) || 1;
        entrada.value = String(Math.min(99, Math.max(1, atual + Number(botao.dataset.passo))));
        aplicar(entrada.value);
      });
    });

    entrada.addEventListener("change", function () { aplicar(entrada.value); });

    remover.addEventListener("click", function () {
      remover.disabled = true;
      api.carrinho.remover(item.codigo)
        .then(function (carrinho) {
          u.avisar(item.nome + " saiu do carrinho.");
          desenhar(carrinho);
        })
        .catch(function (falha) {
          remover.disabled = false;
          u.avisar(falha.message, "erro");
        });
    });

    return linha;
  }

  /* Resumo com subtotal, desconto, meta da promoção e total */
  var resumo;

  function montarResumo() {
    resumo = {
      subtotal: u.criar("dd"),
      desconto: u.criar("dd", { className: "desconto" }),
      meta: u.criar("div", { className: "meta-oferta" }),
      textoMeta: u.criar("p"),
      barra: u.criar("div", { className: "barra__preenchimento" }),
      total: u.criar("div")
    };
    resumo.meta.append(resumo.textoMeta, u.criar("div", { className: "barra", "aria-hidden": "true" }, resumo.barra));

    return u.criar("aside", { className: "resumo", "aria-labelledby": "titulo-resumo" },
      u.criar("h2", { id: "titulo-resumo" }, "Resumo"),
      u.criar("dl", { className: "valores" },
        u.criar("dt", null, "Subtotal"), resumo.subtotal,
        u.criar("dt", null, "Desconto"), resumo.desconto
      ),
      resumo.meta,
      u.criar("div", { className: "total" }, u.criar("span", null, "Total a pagar"), resumo.total),
      u.criar("a", { className: "botao botao--principal botao--largo", href: "identificacao.html" }, "Fechar pedido"),
      u.criar("a", { href: "index.html" }, "Continuar comprando")
    );
  }

  function atualizarResumo(carrinho) {
    var subtotal = u.centavos(carrinho.subtotal);
    var desconto = u.centavos(carrinho.desconto);

    resumo.subtotal.textContent = u.moeda(subtotal);
    resumo.desconto.textContent = desconto > 0 ? "− " + u.moeda(desconto) : u.moeda(0);
    resumo.total.replaceChildren(u.etiqueta(u.centavos(carrinho.total), true));
    Mercado.comum.atualizarContador(carrinho.quantidadeDeUnidades, false);

    // a meta só aparece quando a promoção tem valor mínimo
    if (carrinho.valorMinimoPromocao === null || carrinho.valorMinimoPromocao === undefined) {
      resumo.meta.hidden = true;
      return;
    }
    var minimo = u.centavos(carrinho.valorMinimoPromocao);
    var falta = u.centavos(carrinho.faltaParaPromocao);
    var oDesconto = promocao && promocao.valorDesconto
      ? u.moeda(u.centavos(promocao.valorDesconto)) + " de desconto"
      : "o desconto";
    var atingiu = falta === 0;
    resumo.meta.hidden = false;
    resumo.meta.classList.toggle("meta-oferta--atingida", atingiu);
    resumo.textoMeta.textContent = atingiu
      ? "Você ganhou " + oDesconto + "."
      : "Faltam " + u.moeda(falta) + " para ganhar " + oDesconto + ".";
    resumo.barra.style.width = Math.min(100, (subtotal / minimo) * 100) + "%";
  }

  function desenhar(carrinho) {
    if (carrinho.itens.length === 0) {
      area.replaceChildren(telaVazia());
      Mercado.comum.atualizarContador(0, false);
      return;
    }
    var lista = u.criar("ul", { className: "itens", "aria-label": "Produtos no carrinho" });
    carrinho.itens.forEach(function (item) { lista.append(linhaDoItem(item)); });
    area.replaceChildren(u.criar("div", { className: "duas-colunas" }, lista, montarResumo()));
    atualizarResumo(carrinho);
  }

  Promise.all([Mercado.comum.carrinhoInicial, Mercado.comum.promocao])
    .then(function (respostas) {
      promocao = respostas[1];
      desenhar(respostas[0]);
    })
    .catch(function (falha) {
      area.replaceChildren(telaDeErro(falha.message));
    });
})();
