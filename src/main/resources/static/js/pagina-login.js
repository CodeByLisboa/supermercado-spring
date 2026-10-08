/*
 * pagina-login.js — entrada do operador (login.html).
 * Depois do login volta para a página pedida (?voltar=pedidos.html).
 */
(function () {
  "use strict";

  var u = Mercado.util;
  var api = Mercado.api;

  var formulario = document.getElementById("form-login");
  var mensagem = document.getElementById("erro-login");
  var botao = formulario.querySelector("button[type=submit]");

  /** Só aceita voltar para uma página deste site (evita redirecionar para fora) */
  function destino() {
    var voltar = u.parametro("voltar") || "";
    return /^[a-z0-9_-]+\.html(\?[a-z0-9_=&-]*)?$/i.test(voltar) ? voltar : "pedidos.html";
  }

  formulario.addEventListener("submit", function (evento) {
    evento.preventDefault();
    var login = formulario.login.value.trim();
    var senha = formulario.senha.value;
    if (!login || !senha) {
      mensagem.textContent = "Informe o login e a senha.";
      (login ? formulario.senha : formulario.login).focus();
      return;
    }
    mensagem.textContent = "";
    botao.disabled = true;
    api.sessao.entrar(login, senha)
      .then(function () {
        window.location.href = destino();
      })
      .catch(function (falha) {
        botao.disabled = false;
        mensagem.textContent = falha.status === 401 ? "Login ou senha incorretos." : falha.message;
        formulario.senha.value = "";
        formulario.senha.focus();
      });
  });

  // Se já estiver logado, não precisa entrar de novo
  api.sessao.ver().then(function (sessao) {
    if (sessao.operador) window.location.replace(destino());
  }).catch(function () {});
})();
