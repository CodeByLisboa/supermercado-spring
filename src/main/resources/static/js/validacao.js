/*
 * validacao.js — regras de validação dos dados digitados.
 * Mesmas regras do ValidadorCpf e do ValidadorEmail do projeto Java.
 * Cada função devolve uma mensagem de erro, ou "" quando o valor está certo.
 */
window.Mercado = window.Mercado || {};

Mercado.validacao = (function () {
  "use strict";

  function somenteDigitos(texto) {
    return String(texto || "").replace(/\D/g, "");
  }

  function digitoVerificador(digitos, quantidade) {
    var soma = 0;
    for (var i = 0; i < quantidade; i++) {
      soma += Number(digitos[i]) * (quantidade + 1 - i);
    }
    var resto = (soma * 10) % 11;
    return resto === 10 ? 0 : resto;
  }

  function cpfValido(cpf) {
    var d = somenteDigitos(cpf);
    if (d.length !== 11 || /^(\d)\1{10}$/.test(d)) {
      return false;
    }
    return digitoVerificador(d, 9) === Number(d[9]) && digitoVerificador(d, 10) === Number(d[10]);
  }

  function cpf(valor) {
    var d = somenteDigitos(valor);
    if (d.length === 0) return "Informe o CPF.";
    if (d.length < 11) return "O CPF tem 11 números. Faltam " + (11 - d.length) + ".";
    if (!cpfValido(d)) return "CPF inválido. Confira os números digitados.";
    return "";
  }

  function nome(valor) {
    var texto = String(valor || "").trim().replace(/\s+/g, " ");
    if (texto.length === 0) return "Informe seu nome completo.";
    if (!/^[A-Za-zÀ-ÖØ-öø-ÿ' ]+$/.test(texto)) return "Use apenas letras e espaços no nome.";
    if (texto.split(" ").length < 2) return "Informe nome e sobrenome.";
    return "";
  }

  function email(valor) {
    var texto = String(valor || "").trim();
    if (texto.length === 0) return "Informe o e-mail.";
    if (!/^[\w.+-]+@[\w-]+(\.[\w-]+)+$/.test(texto)) return "E-mail inválido. Use o formato nome@dominio.com.";
    return "";
  }

  /** Celular é opcional: só valida se a pessoa digitou algo */
  function celular(valor) {
    var d = somenteDigitos(valor);
    if (d.length === 0) return "";
    if (d.length < 10 || d.length > 11) return "Celular incompleto. Use DDD e número, ou deixe em branco.";
    return "";
  }

  /** Quantidade de um produto: número inteiro de 1 a 99 */
  function quantidade(valor) {
    var texto = String(valor).trim();
    if (!/^-?\d+$/.test(texto)) return "Digite um número inteiro.";
    var numero = Number(texto);
    if (numero < 1 || numero > 99) return "A quantidade vai de 1 a 99.";
    return "";
  }

  /** Máscara enquanto digita: 52998224725 -> 529.982.247-25 */
  function mascaraCpf(valor) {
    var d = somenteDigitos(valor).slice(0, 11);
    return d
      .replace(/^(\d{3})(\d)/, "$1.$2")
      .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
      .replace(/\.(\d{3})(\d{1,2})$/, ".$1-$2");
  }

  /** Máscara do celular: 51999998888 -> (51) 99999-8888 */
  function mascaraCelular(valor) {
    var d = somenteDigitos(valor).slice(0, 11);
    if (d.length <= 2) return d.length ? "(" + d : "";
    var meio = d.length === 11 ? 5 : 4;
    var texto = "(" + d.slice(0, 2) + ") " + d.slice(2, 2 + meio);
    if (d.length > 2 + meio) texto += "-" + d.slice(2 + meio);
    return texto;
  }

  return {
    somenteDigitos: somenteDigitos,
    cpfValido: cpfValido,
    cpf: cpf,
    nome: nome,
    email: email,
    celular: celular,
    quantidade: quantidade,
    mascaraCpf: mascaraCpf,
    mascaraCelular: mascaraCelular
  };
})();
