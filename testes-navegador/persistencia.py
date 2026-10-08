"""
CT-M20 - os pedidos continuam no banco depois que o servidor é reiniciado.

Uso em duas etapas (reinicie o servidor entre elas, sem apagar a pasta dados/):
    python persistencia.py antes
    python persistencia.py depois
"""
import json
import re
import sys
import urllib.parse
import urllib.request
from http.cookiejar import CookieJar

BASE = "http://localhost:8080/"
ARQUIVO = "persistencia.json"


def sessao_do_operador():
    cookies = CookieJar()
    abrir = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies)).open
    abrir(BASE + "api/sessao")
    token = next(c.value for c in cookies if c.name == "XSRF-TOKEN")
    dados = urllib.parse.urlencode({"login": "operador", "senha": "mercado123"}).encode()
    abrir(urllib.request.Request(BASE + "api/login", data=dados, headers={"X-XSRF-TOKEN": token}))
    return abrir


def pedidos():
    abrir = sessao_do_operador()
    return json.load(abrir(BASE + "api/pedidos")), json.load(abrir(BASE + "api/pedidos/resumo"))


def comprar():
    cookies = CookieJar()
    abrir = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies)).open
    abrir(BASE + "api/carrinho")
    token = lambda: next(c.value for c in cookies if c.name == "XSRF-TOKEN")
    json_post = lambda caminho, corpo: abrir(urllib.request.Request(BASE + caminho, data=json.dumps(corpo).encode(),
        headers={"Content-Type": "application/json", "X-XSRF-TOKEN": token()}))
    json_post("api/carrinho/itens", {"codigo": 1, "quantidade": 1})
    resposta = json_post("api/pedidos", {"nome": "Teste Persistencia", "cpf": "52998224725",
                                          "email": "teste@email.com", "consentimento": True})
    return json.load(resposta)["numero"]


if sys.argv[1] == "antes":
    lista, resumo = pedidos()
    json.dump({"quantidade": len(lista), "maior": max(p["numero"] for p in lista), "resumo": resumo},
              open(ARQUIVO, "w"))
    print("Antes de reiniciar:", len(lista), "pedidos; resumo", resumo)
else:
    antes = json.load(open(ARQUIVO))
    lista, resumo = pedidos()
    novo = comprar()
    ok = len(lista) == antes["quantidade"] and resumo == antes["resumo"] and novo == antes["maior"] + 1
    print("Depois de reiniciar:", len(lista), "pedidos; resumo", resumo, "; novo pedido nº", novo)
    print("[OK] CT-M20" if ok else "[FALHOU] CT-M20")
    sys.exit(0 if ok else 1)
