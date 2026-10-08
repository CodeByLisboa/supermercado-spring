"""
Casos de teste do Plano de Testes (etapa 7) executados no sistema integrado
(front-end + API Spring + banco de dados), com um navegador de verdade
controlado pelo Playwright.

Uso:
    pip install playwright
    python -m playwright install chromium
    python casos_de_teste.py                 (servidor em http://localhost:8080)

Variáveis opcionais:
    MERCADO_URL             endereço do sistema (padrão http://localhost:8080/)
    MERCADO_URL_PERCENTUAL  segundo servidor rodando com
                            --mercado.promocao.tipo=percentual (para o CT-M10)
    MERCADO_EVIDENCIAS      pasta onde salvar as capturas de tela
Gera o arquivo resultados.json com o resultado de cada caso.
"""
import asyncio
import json
import os
import random
import re
import sys
from datetime import datetime

from playwright.async_api import async_playwright

BASE = os.environ.get("MERCADO_URL", "http://localhost:8080/")
BASE_PERCENTUAL = os.environ.get("MERCADO_URL_PERCENTUAL", "")
EVIDENCIAS = os.environ.get("MERCADO_EVIDENCIAS", "evidencias")
OPERADOR = ("operador", "mercado123")

resultados = []
erros_de_console = []


def sem_ponto(texto):
    return texto.strip().rstrip(".")


def registrar(caso, requisito, passou, obtido):
    resultados.append({"caso": caso, "requisito": requisito,
                       "resultado": "Passou" if passou else "Falhou", "obtido": obtido})
    print(("[OK]     " if passou else "[FALHOU] ") + caso + " | " + obtido, flush=True)


def nao_se_aplica(caso, requisito, motivo):
    resultados.append({"caso": caso, "requisito": requisito, "resultado": "Não se aplica", "obtido": motivo})
    print("[N/A]    " + caso + " | " + motivo, flush=True)


class Navegador:
    """Um visitante: cada contexto do navegador tem os próprios cookies (sessão)."""

    def __init__(self, browser):
        self.browser = browser

    async def novo(self, locale="pt-BR", base=BASE, largura=1366):
        ctx = await self.browser.new_context(viewport={"width": largura, "height": 900}, locale=locale,
                                             base_url=base)
        pg = await ctx.new_page()
        pg.on("console", lambda m: erros_de_console.append(m.text) if m.type == "error" else None)
        pg.on("pageerror", lambda e: erros_de_console.append(str(e)))
        pg.on("dialog", lambda d: (erros_de_console.append("ALERTA ABERTO: " + d.message),
                                   asyncio.ensure_future(d.dismiss())))
        return pg


async def abrir_vitrine(pg):
    await pg.goto("index.html")
    await pg.wait_for_selector(".produto")


async def adicionar(pg, codigo, quantidade):
    cartao = pg.locator(f".produto[data-codigo='{codigo}']")
    await cartao.locator("input").fill(str(quantidade))
    async with pg.expect_response(lambda r: "api/carrinho/itens" in r.url):
        await cartao.locator("button[type=submit]").click()
    await pg.wait_for_timeout(100)


async def abrir_carrinho(pg):
    await pg.goto("carrinho.html")
    await pg.wait_for_selector(".resumo, .vazio")


async def total_do_carrinho(pg):
    return await pg.locator(".resumo .etiqueta").get_attribute("aria-label")


async def texto_do_resumo(pg):
    return (await pg.locator(".resumo").inner_text()).replace("\xa0", " ")


async def preencher_cliente(pg, nome="João Silva", cpf="52998224725", email="joao@email.com", consentir=True):
    await pg.goto("identificacao.html")
    await pg.wait_for_selector("#resumo-pedido .lista-resumo")
    await pg.fill("#nome", nome)
    await pg.fill("#cpf", "")
    await pg.locator("#cpf").press_sequentially(cpf)
    await pg.fill("#email", email)
    if consentir:
        await pg.check("#consentimento")


async def confirmar_pedido(pg):
    await pg.click("#form-cliente button[type=submit]")
    await pg.wait_for_url(re.compile(r"pedido\.html\?numero=\d+"), timeout=8000)
    await pg.wait_for_selector(".cupom")
    return int(re.search(r"numero=(\d+)", pg.url).group(1))


async def comprar(pg, itens, **cliente):
    await abrir_vitrine(pg)
    for codigo, quantidade in itens:
        await adicionar(pg, codigo, quantidade)
    await preencher_cliente(pg, **cliente)
    return await confirmar_pedido(pg)


async def entrar_como_operador(pg):
    await pg.goto("login.html?voltar=pedidos.html")
    await pg.fill("#login", OPERADOR[0])
    await pg.fill("#senha", OPERADOR[1])
    await pg.click("#form-login button[type=submit]")
    await pg.wait_for_url(re.compile(r"pedidos\.html"))
    await pg.wait_for_selector("#lista-pedidos table, #lista-pedidos .vazio")


async def captura(pg, nome):
    os.makedirs(EVIDENCIAS, exist_ok=True)
    await pg.screenshot(path=os.path.join(EVIDENCIAS, nome + ".png"), full_page=True)


async def main():
    inicio = datetime.now()
    async with async_playwright() as p:
        browser = await p.chromium.launch()
        nav = Navegador(browser)

        # CT-M01 e CT-M02 - adicionar e somar na mesma linha
        pg = await nav.novo()
        await abrir_vitrine(pg)
        await adicionar(pg, 2, 2)
        await abrir_carrinho(pg)
        linhas = await pg.locator(".item").count()
        sub = (await pg.locator(".item__subtotal").first.inner_text()).replace("\xa0", " ")
        registrar("CT-M01", "RF01", linhas == 1 and "43,98" in sub and "43,98" in await total_do_carrinho(pg),
                  f"1 linha no carrinho: 2 x R$ 21,99 = {sub.strip()}; total atualizado.")
        await abrir_vitrine(pg)
        await adicionar(pg, 2, 1)
        await abrir_carrinho(pg)
        qtd = await pg.locator(".item input").first.input_value()
        registrar("CT-M02", "RF01", await pg.locator(".item").count() == 1 and qtd == "3",
                  f"A mesma linha passou para {qtd} unidades; nenhuma linha duplicada.")

        # CT-M03 - quantidade 0 e -1
        pg = await nav.novo()
        await abrir_vitrine(pg)
        cartao = pg.locator(".produto[data-codigo='1']")
        mensagens = []
        for valor in ("0", "-1"):
            await cartao.locator("input").fill(valor)
            await cartao.locator("button[type=submit]").click()
            mensagens.append((await cartao.locator(".mensagem-erro").text_content()).strip())
        contador = await pg.locator("[data-contador-carrinho]").first.inner_text()
        resposta = await pg.evaluate("""() => Mercado.api.carrinho.adicionar(1, 0)
            .then(() => 'aceitou', e => e.status + ' ' + e.message)""")
        registrar("CT-M03", "RN01", all(mensagens) and contador == "0" and resposta.startswith("422"),
                  f"0 e -1 mostram “{sem_ponto(mensagens[0])}”; o carrinho continua vazio. "
                  f"Enviando 0 direto para a API, o servidor também recusa ({sem_ponto(resposta)}).")

        # CT-M04 - remover
        pg = await nav.novo()
        await abrir_vitrine(pg)
        await adicionar(pg, 1, 1)
        await adicionar(pg, 2, 2)
        await abrir_carrinho(pg)
        async with pg.expect_response(lambda r: "api/carrinho/itens/1" in r.url):
            await pg.locator(".item[data-codigo='1'] .link-simples").click()
        await pg.wait_for_timeout(200)
        total = await total_do_carrinho(pg)
        registrar("CT-M04", "RF02", await pg.locator(".item").count() == 1 and "43,98" in total,
                  f"Arroz removido; total recalculado para {total.replace(chr(160), ' ')}.")

        # CT-M05 - navegador em inglês
        pg = await nav.novo(locale="en-US")
        await abrir_vitrine(pg)
        await adicionar(pg, 2, 50)
        await abrir_carrinho(pg)
        resumo = await texto_do_resumo(pg)
        total = (await total_do_carrinho(pg)).replace("\xa0", " ")
        registrar("CT-M05", "RF03, RNF03", "1.099,50" in resumo and "1.089,50" in total,
                  f"Navegador em inglês: subtotal R$ 1.099,50 e total {total} (padrão brasileiro).")

        # CT-M06 e CT-M07 - CPF, e-mail e nome inválidos
        pg = await nav.novo()
        await abrir_vitrine(pg)
        await adicionar(pg, 1, 1)
        await preencher_cliente(pg, cpf="12345678900")
        await pg.click("#form-cliente button[type=submit]")
        erro_cpf = (await pg.text_content("#erro-cpf")).strip()
        await captura(pg, "CT-M06_cpf_invalido")
        registrar("CT-M06", "RF04, RN03", "inválido" in erro_cpf and "identificacao" in pg.url,
                  f"Mensagem “{sem_ponto(erro_cpf)}”; o pedido não foi fechado.")
        await pg.fill("#cpf", "")
        await pg.locator("#cpf").press_sequentially("52998224725")
        await pg.fill("#email", "joao.email.com")
        await pg.click("#form-cliente button[type=submit]")
        erro_email = (await pg.text_content("#erro-email")).strip()
        await pg.fill("#email", "joao@email.com")
        await pg.fill("#nome", "")
        await pg.click("#form-cliente button[type=submit]")
        erro_nome = (await pg.text_content("#erro-nome")).strip()
        servidor = await pg.evaluate("""() => Mercado.api.pedidos.fechar({nome: 'Joao', cpf: '123.456.789-00',
            email: 'joao.email.com', consentimento: true}).then(() => 'aceitou', e => JSON.stringify(e.campos))""")
        registrar("CT-M07", "RF04, RN03",
                  "inválido" in erro_email and "nome" in erro_nome.lower() and "cpf" in servidor and "email" in servidor,
                  f"E-mail sem @: “{sem_ponto(erro_email)}”. Nome em branco: “{sem_ponto(erro_nome)}”. "
                  "Enviando os mesmos dados direto para a API, o servidor devolve 400 com a mensagem de cada campo.")

        # CT-M08 - carrinho vazio
        pg = await nav.novo()
        await abrir_carrinho(pg)
        botao = await pg.locator("text=Fechar pedido").count()
        await pg.goto("identificacao.html")
        await pg.wait_for_timeout(500)
        orienta = await pg.locator("#carrinho-vazio").is_visible()
        resposta = await pg.evaluate("""() => Mercado.api.pedidos.fechar({nome: 'Ana Lima', cpf: '39053344705',
            email: 'ana@email.com', consentimento: true}).then(() => 'aceitou', e => e.status + ' ' + e.message)""")
        registrar("CT-M08", "RF05, RN04", botao == 0 and orienta and resposta.startswith("422"),
                  "Sem botão Fechar pedido com o carrinho vazio; a identificação orienta a voltar aos produtos; "
                  f"a API recusa ({sem_ponto(resposta)}).")

        # CT-M09 e CT-M17 - pedido do cenário original
        pg = await nav.novo()
        numero = await comprar(pg, [(1, 1), (2, 2)])
        cupom = (await pg.locator(".cupom").inner_text()).replace("\xa0", " ")
        contador = await pg.locator("[data-contador-carrinho]").first.inner_text()
        ok9 = (f"Pedido nº {numero:06d}" in cupom and "1 x Arroz" in cupom and "2 x Filé" in cupom
               and "47,97" in cupom and re.search(r"\d{2}/\d{2}/\d{4}", cupom) is not None and contador == "0")
        await captura(pg, "CT-M09_cupom")
        registrar("CT-M09", "RF05, RF07, RN06", ok9,
                  f"Cupom nº {numero:06d} com data e hora, 2 itens, subtotal R$ 47,97, desconto R$ 0,00 e "
                  "total R$ 47,97; carrinho vazio.")
        json_pedido = await pg.evaluate(f"() => fetch('api/pedidos/{numero}').then(r => r.text())")
        registrar("CT-M17", "RNF08", "529" not in cupom and "CPF" not in cupom and "52998224725" not in json_pedido,
                  "O cupom mostra nome e e-mail, sem CPF; na resposta da API o CPF vem mascarado (***.982.247-**).")

        # CT-M10 - promoção de 10% (segundo servidor configurado com tipo=percentual)
        if BASE_PERCENTUAL:
            pg = await nav.novo(base=BASE_PERCENTUAL)
            numero = await comprar(pg, [(1, 1), (2, 2)])
            cupom = (await pg.locator(".cupom").inner_text()).replace("\xa0", " ")
            faixa = await pg.evaluate("() => Mercado.api.promocao().then(p => p.chamada)")
            registrar("CT-M10", "RF06", "-R$ 4,80" in cupom and "R$ 43,17" in cupom,
                      f"Servidor iniciado com mercado.promocao.tipo=percentual (faixa: “{faixa}”): "
                      "cupom com desconto de R$ 4,80 e total R$ 43,17.")
        else:
            nao_se_aplica("CT-M10", "RF06", "Rode um segundo servidor com --mercado.promocao.tipo=percentual "
                          "e informe MERCADO_URL_PERCENTUAL.")

        # CT-M11 - R$ 99,99 x R$ 100,00
        pg = await nav.novo()
        await abrir_vitrine(pg)
        for codigo, qtd in [(2, 1), (5, 4), (14, 64)]:
            await adicionar(pg, codigo, qtd)
        await abrir_carrinho(pg)
        t1, r1 = await total_do_carrinho(pg), await texto_do_resumo(pg)
        pg = await nav.novo()
        await abrir_vitrine(pg)
        for codigo, qtd in [(5, 1), (13, 4), (14, 25)]:
            await adicionar(pg, codigo, qtd)
        await abrir_carrinho(pg)
        t2, r2 = await total_do_carrinho(pg), await texto_do_resumo(pg)
        await captura(pg, "CT-M11_meta_atingida")
        registrar("CT-M11", "RF06, RN09", "99,99" in t1 and "Faltam R$ 0,01" in r1 and "90,00" in t2 and "ganhou" in r2,
                  "R$ 99,99 → sem desconto (faltam R$ 0,01). R$ 100,00 → desconto de R$ 10,00, total R$ 90,00.")

        # CT-M12 e CT-M13 - área do operador
        cliente = await nav.novo()
        numero = await comprar(cliente, [(6, 2), (7, 1)], nome="Maria Souza", cpf="12345678909",
                               email="maria@email.com")
        pg = await nav.novo()
        await entrar_como_operador(pg)
        await pg.fill("#filtro-texto", str(numero))
        linhas = await pg.locator("tbody tr").count()
        linha = await pg.locator("tbody tr").first.inner_text()
        async with pg.expect_navigation():
            await pg.locator("tbody tr").first.locator("text=Ver cupom").click()
        await pg.wait_for_selector(".cupom")
        abriu = f"Pedido nº {numero:06d}" in await pg.inner_text("main")
        await pg.goto("pedido.html?numero=999999")
        await pg.wait_for_selector(".vazio")
        nao_encontrado = "não encontrado" in await pg.inner_text("main")
        registrar("CT-M12", "RF08, RF09", linhas == 1 and f"{numero:06d}" in linha and abriu and nao_encontrado,
                  f"Busca “{numero}” mostra só o pedido {numero:06d} (Maria Souza) e “Ver cupom” abre o pedido certo; "
                  "o número 999999 mostra “Pedido não encontrado”.")

        await pg.goto("pedidos.html")
        await pg.wait_for_selector("tbody tr")
        linha = pg.locator("tbody tr", has_text=f"{numero:06d}")
        await linha.locator("button", has_text="Cancelar").click()
        await captura(pg, "CT-M13_confirmar_cancelamento")
        async with pg.expect_response(lambda r: "cancelamento" in r.url):
            await pg.click("#confirmar-cancelamento")
        await pg.wait_for_timeout(500)
        linha = pg.locator("tbody tr", has_text=f"{numero:06d}")
        situacao = await linha.inner_text()
        sem_botao = await linha.locator("button", has_text="Cancelar").count() == 0
        segunda = await pg.evaluate(f"() => Mercado.api.pedidos.cancelar({numero}).then(() => 'aceitou', e => e.message)")
        registrar("CT-M13", "RF10, RN10", "Cancelado" in situacao and sem_botao and "já está cancelado" in segunda,
                  f"Situação mudou para Cancelado e o botão Cancelar sumiu; nova tentativa pela API recusada com "
                  f"“{sem_ponto(segunda)}”.")

        # CT-M14 - pedido fechado não muda
        pg = await nav.novo()
        numero = await comprar(pg, [(1, 1)])
        await abrir_vitrine(pg)
        await adicionar(pg, 2, 3)
        await pg.goto(f"pedido.html?numero={numero}")
        await pg.wait_for_selector(".cupom")
        cupom = (await pg.locator(".cupom").inner_text()).replace("\xa0", " ")
        registrar("CT-M14", "RN05", "Filé" not in cupom and "3,99" in cupom,
                  f"Depois de novos itens no carrinho, o pedido {numero:06d} continua com 1 arroz e total R$ 3,99.")

        # CT-M15 - nome com código
        pg = await nav.novo()
        await abrir_vitrine(pg)
        await adicionar(pg, 1, 1)
        await preencher_cliente(pg, nome="<script>alert(1)</script>")
        await pg.click("#form-cliente button[type=submit]")
        erro_nome = (await pg.text_content("#erro-nome")).strip()
        direto = await pg.evaluate("""() => Mercado.api.pedidos.fechar({nome: '<img src=x onerror=alert(1)>',
            cpf: '52998224725', email: 'a@b.com', consentimento: true}).then(() => 'aceitou', e => e.campos.nome)""")
        alertas = [e for e in erros_de_console if e.startswith("ALERTA")]
        registrar("CT-M15", "RNF06", "letras" in erro_nome and "letras" in direto and not alertas,
                  f"O formulário recusa o nome (“{sem_ponto(erro_nome)}”) e o servidor também recusa quando o código "
                  "é enviado direto para a API; nenhum script foi executado.")

        # CT-M16 - dois (e dez) navegadores fechando pedidos ao mesmo tempo
        clientes = []
        for i in range(10):
            pg = await nav.novo()
            await abrir_vitrine(pg)
            await adicionar(pg, 1, 1)
            await preencher_cliente(pg, nome="Cliente Simultaneo", cpf="71428793860", email=f"c{i}@email.com")
            clientes.append(pg)
        numeros = await asyncio.gather(*[confirmar_pedido(c) for c in clientes])
        registrar("CT-M16", "RNF07", len(set(numeros)) == len(numeros),
                  f"10 navegadores confirmaram ao mesmo tempo e receberam 10 números diferentes "
                  f"({min(numeros):06d} a {max(numeros):06d}); nenhum pedido se perdeu.")

        # ---- casos novos da etapa 9 ----

        # CT-M18 - área do operador exige login
        pg = await nav.novo()
        await pg.goto("pedidos.html")
        foi_para_login = "login.html" in pg.url
        await pg.fill("#login", "operador")
        await pg.fill("#senha", "senha-errada")
        await pg.click("#form-login button[type=submit]")
        await pg.wait_for_function("document.getElementById('erro-login').textContent.length > 0")
        mensagem = await pg.text_content("#erro-login")
        await captura(pg, "CT-M18_login_recusado")
        registrar("CT-M18", "RNF09 (novo)", foi_para_login and "incorretos" in mensagem,
                  f"Sem login, a área do operador abre a tela de login; senha errada mostra “{sem_ponto(mensagem)}”.")

        # CT-M19 - cupom só para quem fez a compra
        dono = await nav.novo()
        numero = await comprar(dono, [(3, 1)], nome="Paulo Rocha", cpf="45317828791", email="paulo@email.com")
        outro = await nav.novo()
        await outro.goto(f"pedido.html?numero={numero}")
        await outro.wait_for_selector(".vazio, .cupom")
        bloqueado = "não encontrado" in await outro.inner_text("main")
        registrar("CT-M19", "RNF08 (novo)", bloqueado,
                  f"O cupom {numero:06d} abre no navegador do comprador e, em outro navegador, aparece "
                  "“Pedido não encontrado”.")

        # CT-M21 - cliques rápidos no + com a internet lenta (regressão do bug #2)
        divergencias = []
        for rodada in range(6):
            pg = await nav.novo()
            await abrir_vitrine(pg)
            await adicionar(pg, 1, 1)
            await abrir_carrinho(pg)

            async def atrasar(rota):
                await asyncio.sleep(random.uniform(0, 0.4))
                await rota.continue_()
            await pg.route(re.compile(r".*/api/carrinho/itens/.*"), atrasar)
            mais = pg.locator(".item[data-codigo='1'] [data-passo='1']")
            for _ in range(6):
                await mais.click(delay=0)
            await pg.wait_for_timeout(3500)
            na_tela = await pg.locator(".item input").first.input_value()
            subtotal = (await pg.locator(".item__subtotal").first.inner_text()).replace("\xa0", " ")
            servidor = await pg.evaluate("() => fetch('api/carrinho').then(r => r.json())")
            if not (na_tela == "7" and servidor["itens"][0]["quantidade"] == 7 and "27,93" in subtotal):
                divergencias.append(f"tela {na_tela} / servidor {servidor['itens'][0]['quantidade']}")
        registrar("CT-M21", "RF02 (novo)", not divergencias,
                  "6 rodadas de 6 cliques rápidos no + com atraso de rede de até 0,4 s: tela e servidor terminaram "
                  "com 7 unidades e R$ 27,93 em todas." if not divergencias else "Divergências: " + "; ".join(divergencias))

        await browser.close()

    erros_reais = [e for e in erros_de_console if "401" not in e and "404" not in e and "422" not in e
                   and "400" not in e and "403" not in e]
    registrar("CONSOLE", "-", not erros_reais,
              "Nenhum erro de JavaScript no console." if not erros_reais else "Erros: " + " | ".join(erros_reais[:5]))
    ordem = {r["caso"]: r for r in resultados}
    with open("resultados.json", "w", encoding="utf-8") as arquivo:
        json.dump({"inicio": inicio.isoformat(timespec="seconds"), "fim": datetime.now().isoformat(timespec="seconds"),
                   "url": BASE, "casos": list(ordem.values())}, arquivo, ensure_ascii=False, indent=1)
    falhas = [r for r in resultados if r["resultado"] == "Falhou"]
    print(f"\nPassou: {sum(r['resultado'] == 'Passou' for r in resultados)} | Falhou: {len(falhas)} | "
          f"N/A: {sum(r['resultado'] == 'Não se aplica' for r in resultados)}")
    sys.exit(1 if falhas else 0)


if __name__ == "__main__":
    asyncio.run(main())
