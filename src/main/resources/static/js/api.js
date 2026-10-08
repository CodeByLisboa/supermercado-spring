/*
 * api.js — cliente da API REST do servidor Spring.
 *
 * Substitui o regras.js e o dados.js da etapa 8: o catálogo vem do banco de
 * dados, o carrinho fica na sessão do servidor e os pedidos são gravados
 * pelo PedidoService (Java). Cada função devolve uma Promise.
 *
 * Toda requisição que altera dados leva o token CSRF: o servidor manda o
 * token no cookie XSRF-TOKEN e espera de volta no cabeçalho X-XSRF-TOKEN.
 * Erros viram Mercado.api.ApiErro, com o código HTTP, a mensagem do
 * servidor e, nos erros de formulário, a mensagem de cada campo.
 */
window.Mercado = window.Mercado || {};

Mercado.api = (function () {
  "use strict";

  function ApiErro(status, corpo) {
    this.name = "ApiErro";
    this.status = status;
    this.message = (corpo && corpo.mensagem) ||
      (status === 0
        ? "Não foi possível falar com o servidor. Confira se o sistema está rodando."
        : "Algo deu errado no servidor. Tente de novo.");
    this.campos = (corpo && corpo.campos) || {};
  }
  ApiErro.prototype = Object.create(Error.prototype);

  function lerCookie(nome) {
    var achado = document.cookie.split("; ").find(function (par) { return par.indexOf(nome + "=") === 0; });
    return achado ? decodeURIComponent(achado.substring(nome.length + 1)) : null;
  }

  /**
   * Faz a requisição e devolve o JSON da resposta.
   * comoFormulario: envia os campos como formulário (usado no login).
   */
  function chamar(metodo, caminho, corpo, comoFormulario) {
    var opcoes = { method: metodo, credentials: "same-origin", headers: { "Accept": "application/json" } };
    if (metodo !== "GET") {
      var token = lerCookie("XSRF-TOKEN");
      if (token) opcoes.headers["X-XSRF-TOKEN"] = token;
    }
    if (corpo !== undefined) {
      if (comoFormulario) {
        opcoes.body = new URLSearchParams(corpo);
      } else {
        opcoes.headers["Content-Type"] = "application/json";
        opcoes.body = JSON.stringify(corpo);
      }
    }
    return fetch(caminho, opcoes).then(function (resposta) {
      return resposta.text().then(function (texto) {
        var dados = null;
        try {
          dados = texto ? JSON.parse(texto) : null;
        } catch (erroDeLeitura) {
          dados = null;
        }
        if (!resposta.ok) throw new ApiErro(resposta.status, dados);
        return dados;
      });
    }, function () {
      throw new ApiErro(0, null);
    });
  }

  return {
    ApiErro: ApiErro,

    produtos: function () { return chamar("GET", "api/produtos"); },
    categorias: function () { return chamar("GET", "api/categorias"); },
    promocao: function () { return chamar("GET", "api/promocao"); },

    carrinho: {
      ver: function () { return chamar("GET", "api/carrinho"); },
      adicionar: function (codigo, quantidade) {
        return chamar("POST", "api/carrinho/itens", { codigo: Number(codigo), quantidade: Number(quantidade) });
      },
      alterar: function (codigo, quantidade) {
        return chamar("PUT", "api/carrinho/itens/" + encodeURIComponent(codigo), { quantidade: Number(quantidade) });
      },
      remover: function (codigo) {
        return chamar("DELETE", "api/carrinho/itens/" + encodeURIComponent(codigo));
      }
    },

    pedidos: {
      fechar: function (cliente) { return chamar("POST", "api/pedidos", cliente); },
      buscar: function (numero) { return chamar("GET", "api/pedidos/" + encodeURIComponent(numero)); },
      listar: function () { return chamar("GET", "api/pedidos"); },
      resumo: function () { return chamar("GET", "api/pedidos/resumo"); },
      cancelar: function (numero) { return chamar("POST", "api/pedidos/" + encodeURIComponent(numero) + "/cancelamento"); },
      criarExemplos: function () { return chamar("POST", "api/pedidos/exemplos"); }
    },

    sessao: {
      ver: function () { return chamar("GET", "api/sessao"); },
      entrar: function (login, senha) { return chamar("POST", "api/login", { login: login, senha: senha }, true); },
      sair: function () { return chamar("POST", "api/logout"); }
    }
  };
})();
