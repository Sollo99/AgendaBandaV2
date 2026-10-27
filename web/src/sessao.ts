export const EVENTO_SESSAO_ENCERRADA = "agendabanda:sessao-encerrada";
export const CHAVE_MUDANCA_SESSAO = "agendabanda:mudanca-sessao";
let geracao = 0;
let encerrada = false;

export function geracaoSessao() { return geracao; }

export function sincronizarSessao(ativa: boolean) {
  ++geracao;
  encerrada = !ativa;
  if (!ativa) window.dispatchEvent(new CustomEvent(EVENTO_SESSAO_ENCERRADA, { detail: "" }));
}

export function iniciarSessao() {
  sincronizarSessao(true);
  // Remove credenciais persistidas por versoes anteriores.
  localStorage.removeItem("token");
  localStorage.removeItem("usuario");
  localStorage.setItem(CHAVE_MUDANCA_SESSAO, "login:" + crypto.randomUUID());
}

export function encerrarSessao(mensagem = "") {
  ++geracao;
  encerrada = true;
  localStorage.removeItem("token");
  localStorage.removeItem("usuario");
  localStorage.setItem(CHAVE_MUDANCA_SESSAO, "logout:" + crypto.randomUUID());
  window.dispatchEvent(new CustomEvent(EVENTO_SESSAO_ENCERRADA, { detail: mensagem }));
}

export function tratarNaoAutenticado(geracaoEnviada: number) {
  if (!encerrada && geracao === geracaoEnviada) {
    encerrarSessao("Sua sessao expirou ou foi encerrada. Entre novamente.");
  }
}
