import { beforeEach, test } from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import ts from "typescript";

// Usa o TypeScript já instalado; os módulos são carregados em memória, sem build adicional.
const transpilar = (source) => ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 }
}).outputText;
const urlModulo = (source) => `data:text/javascript;base64,${Buffer.from(source).toString("base64")}`;
const sessaoUrl = urlModulo(transpilar(await readFile(new URL("../src/sessao.ts", import.meta.url), "utf8")));
const apiSource = (await readFile(new URL("../src/api.ts", import.meta.url), "utf8"))
  .replace('from "./sessao"', `from "${sessaoUrl}"`)
  .replace("import.meta.env.VITE_API_URL", '"/api"');
const { api, ApiError } = await import(urlModulo(transpilar(apiSource)));
const { EVENTO_SESSAO_ENCERRADA, encerrarSessao } = await import(sessaoUrl);
let avisos;

beforeEach(() => {
  if (typeof globalThis.CustomEvent === "undefined") {
    globalThis.CustomEvent = class extends Event {
      constructor(type, options = {}) { super(type, options); this.detail = options.detail; }
    };
  }
  const dados = new Map([["token", "sessao-atual"], ["usuario", '{"nome":"Ana"}']]);
  globalThis.localStorage = {
    getItem: (key) => dados.get(key) ?? null,
    setItem: (key, value) => dados.set(key, String(value)),
    removeItem: (key) => dados.delete(key)
  };
  globalThis.window = new EventTarget();
  avisos = [];
  window.addEventListener(EVENTO_SESSAO_ENCERRADA, (event) => avisos.push(event.detail));
});

test("401 autenticado encerra sessão e avisa uma única vez para chamadas simultâneas", async () => {
  globalThis.fetch = async () => new Response(JSON.stringify({ erro: "Acesso não autenticado." }), { status: 401 });
  const resultados = await Promise.allSettled([api.listarAgenda(), api.obterMeuPerfil()]);
  assert.ok(resultados.every((item) => item.status === "rejected"));
  assert.equal(localStorage.getItem("token"), null);
  assert.equal(localStorage.getItem("usuario"), null);
  assert.equal(avisos.length, 1);
});

test("401 do login público mostra credenciais inválidas e não invalida outra sessão", async () => {
  globalThis.fetch = async (_url, options) => {
    assert.equal(options.headers.Authorization, undefined);
    return new Response(JSON.stringify({ erro: "Login ou senha inválidos." }), { status: 401 });
  };
  await assert.rejects(api.login("ana", "errada"), (err) => err instanceof ApiError && err.status === 401);
  assert.equal(localStorage.getItem("token"), "sessao-atual");
  assert.equal(avisos.length, 0);
});

test("403 mantém a sessão e expõe o status para a interface", async () => {
  globalThis.fetch = async () => new Response(JSON.stringify({ erro: "Sem permissão." }), { status: 403 });
  await assert.rejects(api.listarMembros(), (err) => err.status === 403);
  assert.equal(localStorage.getItem("token"), "sessao-atual");
  assert.equal(avisos.length, 0);
});

test("falha de rede mantém token e dados locais", async () => {
  globalThis.fetch = async () => { throw new TypeError("Sem conexão"); };
  await assert.rejects(api.obterMeuPerfil(), TypeError);
  assert.equal(localStorage.getItem("token"), "sessao-atual");
  assert.notEqual(localStorage.getItem("usuario"), null);
  assert.equal(avisos.length, 0);
});

test("401 atrasado de uma sessão antiga não encerra o login novo", async () => {
  let responder;
  globalThis.fetch = () => new Promise((resolve) => { responder = resolve; });
  const pedido = api.listarAgenda();
  localStorage.setItem("token", "sessao-nova");
  responder(new Response("{}", { status: 401 }));
  await assert.rejects(pedido, (err) => err.status === 401);
  assert.equal(localStorage.getItem("token"), "sessao-nova");
  assert.equal(avisos.length, 0);
});

test("cadastro público aceita resposta genérica 202 sem enviar token", async () => {
  globalThis.fetch = async (_url, options) => {
    assert.equal(options.headers.Authorization, undefined);
    return new Response(JSON.stringify({ mensagem: "Cadastro será analisado." }), { status: 202 });
  };
  assert.deepEqual(await api.cadastro({ login: "ana" }), { mensagem: "Cadastro será analisado." });
  assert.equal(avisos.length, 0);
});

test("recuperação expirada mantém código de erro sem disparar logout", async () => {
  globalThis.fetch = async () => new Response(JSON.stringify({ erro: "Expirou.", codigo: "RECUPERACAO_EXPIRADA" }), { status: 400 });
  await assert.rejects(api.redefinirSenha(1, "senha123"), (err) => err.codigo === "RECUPERACAO_EXPIRADA");
  assert.equal(avisos.length, 0);
});

test("troca de senha pode encerrar a sessão imediatamente com mensagem de novo login", () => {
  encerrarSessao("Senha alterada. Entre novamente.");
  assert.equal(localStorage.getItem("token"), null);
  assert.equal(localStorage.getItem("usuario"), null);
  assert.deepEqual(avisos, ["Senha alterada. Entre novamente."]);
});
