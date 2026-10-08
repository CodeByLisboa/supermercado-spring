/*
 * pagina-identificacao.js — formulário do cliente com máscaras e
 * validação de nome, CPF (dígitos verificadores), e-mail e celular.
 * O navegador valida enquanto a pessoa digita; ao confirmar, o servidor
 * valida de novo, fecha o pedido no banco de dados e o cupom abre.
 */
(function () {
  "use strict";

  var u = Mercado.util;
  var api = Mercado.api;
  var validacao = Mercado.validacao;

  var formulario = document.getElementById("form-cliente");
  var area = document.getElementById("area-identificacao");
  var botaoConfirmar = formulario.querySelector("button[type=submit]");

  /* ---------- Resumo do pedido ao lado do formulário ---------- */

  function montarResumo(carrinho) {
    var lista = u.criar("ul", { className: "lista-resumo" });
    carrinho.itens.forEach(function (item) {
      lista.append(u.criar("li", null,
        u.criar("span", null, item.quantidade + " × " + item.nome),
        u.criar("span", null, u.moeda(u.centavos(item.subtotal)))
      ));
    });
    var desconto = u.centavos(carrinho.desconto);
    document.getElementById("resumo-pedido").append(
      lista,
      u.criar("dl", { className: "valores" },
        u.criar("dt", null, "Subtotal"), u.criar("dd", null, u.moeda(u.centavos(carrinho.subtotal))),
        u.criar("dt", null, "Desconto"), u.criar("dd", { className: "desconto" },
          desconto > 0 ? "− " + u.moeda(desconto) : u.moeda(0))
      ),
      u.criar("div", { className: "total" }, u.criar("span", null, "Total a pagar"),
        u.etiqueta(u.centavos(carrinho.total), true))
    );
  }

  /* ---------- Validação dos campos ---------- */

  var regrasDosCampos = {
    nome: validacao.nome,
    cpf: validacao.cpf,
    email: validacao.email,
    celular: validacao.celular,
    consentimento: function () {
      return formulario.consentimento.checked ? "" : "Marque a autorização para continuar.";
    }
  };

  function mostrarErro(nomeDoCampo, mensagem) {
    document.getElementById("erro-" + nomeDoCampo).textContent = mensagem;
    formulario.elements[nomeDoCampo].setAttribute("aria-invalid", String(Boolean(mensagem)));
  }

  function validarCampo(nomeDoCampo) {
    var campo = formulario.elements[nomeDoCampo];
    var mensagem = regrasDosCampos[nomeDoCampo](campo.value);
    mostrarErro(nomeDoCampo, mensagem);
    return mensagem === "";
  }

  var tentouEnviar = false;

  Object.keys(regrasDosCampos).forEach(function (nomeDoCampo) {
    var campo = formulario.elements[nomeDoCampo];

    if (nomeDoCampo === "consentimento") {
      // a caixa de marcar é conferida assim que muda
      campo.addEventListener("change", function () { validarCampo(nomeDoCampo); });
      return;
    }

    // texto: valida ao sair do campo e, depois disso, enquanto a pessoa corrige
    var tocado = false;
    campo.addEventListener("blur", function () {
      if (campo.value !== "") {
        tocado = true;
        validarCampo(nomeDoCampo);
      }
    });
    campo.addEventListener("input", function () {
      if (tocado || tentouEnviar) validarCampo(nomeDoCampo);
    });
  });

  /* Máscaras enquanto digita */
  formulario.cpf.addEventListener("input", function () {
    formulario.cpf.value = validacao.mascaraCpf(formulario.cpf.value);
  });
  formulario.celular.addEventListener("input", function () {
    formulario.celular.value = validacao.mascaraCelular(formulario.celular.value);
  });

  formulario.addEventListener("submit", function (evento) {
    evento.preventDefault();
    tentouEnviar = true;
    var invalidos = Object.keys(regrasDosCampos).filter(function (n) { return !validarCampo(n); });
    if (invalidos.length > 0) {
      formulario.elements[invalidos[0]].focus();
      u.avisar(invalidos.length === 1 ? "Corrija o campo destacado." : "Corrija os " + invalidos.length + " campos destacados.", "erro");
      return;
    }
    botaoConfirmar.disabled = true;
    api.pedidos.fechar({
      nome: formulario.nome.value,
      cpf: formulario.cpf.value,
      email: formulario.email.value,
      celular: formulario.celular.value,
      consentimento: formulario.consentimento.checked
    })
      .then(function (pedido) {
        window.location.href = "pedido.html?numero=" + pedido.numero;
      })
      .catch(function (falha) {
        botaoConfirmar.disabled = false;
        // erros de campo vindos do servidor aparecem embaixo de cada campo
        var campos = Object.keys(falha.campos || {});
        campos.forEach(function (nomeDoCampo) {
          if (formulario.elements[nomeDoCampo]) mostrarErro(nomeDoCampo, falha.campos[nomeDoCampo]);
        });
        if (campos.length > 0 && formulario.elements[campos[0]]) formulario.elements[campos[0]].focus();
        u.avisar(falha.message, "erro");
      });
  });

  Mercado.comum.carrinhoInicial
    .then(function (carrinho) {
      if (carrinho.itens.length === 0) {
        area.hidden = true;
        document.getElementById("carrinho-vazio").hidden = false;
        return;
      }
      montarResumo(carrinho);
    })
    .catch(function (falha) {
      u.avisar(falha.message, "erro");
    });
})();
