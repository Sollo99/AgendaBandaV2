export const EVENTO_SESSAO_ENCERRADA = "agendabanda:sessao-encerrada";

export function encerrarSessao(mensagem = "") {
  localStorage.removeItem("token");
  localStorage.removeItem("usuario");
  window.dispatchEvent(new CustomEvent(EVENTO_SESSAO_ENCERRADA, { detail: mensagem }));
}

export function tratarNaoAutenticado(tokenEnviado: string) {
  // Uma resposta atrasada da sessão anterior não pode encerrar um login novo.
  if (localStorage.getItem("token") === tokenEnviado) {
    encerrarSessao("Sua sessão expirou ou foi encerrada. Entre novamente.");
  }
}
