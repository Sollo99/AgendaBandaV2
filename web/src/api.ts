import type { Agenda, Cidade, LocalEvento, LoginResponse, Membro, MusicoResumo, ParticipacaoEvento, SolicitacaoSenha, TipoEvento, Relatorio, HistoricoRegistro } from "./types";
import { geracaoSessao, tratarNaoAutenticado } from "./sessao";

const API_URL = import.meta.env.VITE_API_URL ?? "/api";

export class ApiError extends Error {
  constructor(message: string, public status: number, public codigo?: string) { super(message); }
}

let csrf: { token: string; geracao: number } | null = null;
let buscandoCsrf: Promise<string> | null = null;

async function obterCsrf(): Promise<string> {
  const geracao = geracaoSessao();
  if (csrf?.geracao === geracao) return csrf.token;
  if (!buscandoCsrf) {
    buscandoCsrf = fetch(`${API_URL}/auth/csrf`, { credentials: "include", cache: "no-store" })
      .then(async (response) => {
        if (!response.ok) throw new ApiError("Nao foi possivel preparar a solicitacao.", response.status);
        const data = await response.json() as { token: string };
        csrf = { token: data.token, geracao };
        return data.token;
      }).finally(() => { buscandoCsrf = null; });
  }
  const token = await buscandoCsrf;
  return geracaoSessao() === geracao ? token : obterCsrf();
}

async function request<T>(path: string, options: RequestInit & { publica?: boolean } = {}, repetirCsrf = true): Promise<T> {
  const { publica = false, ...init } = options;
  const geracao = geracaoSessao();
  const mutacao = !["GET", "HEAD", "OPTIONS"].includes((init.method ?? "GET").toUpperCase());
  const csrfToken = mutacao ? await obterCsrf() : null;
  const response = await fetch(`${API_URL}${path}`, {
    ...init,
    credentials: "include",
    cache: "no-store",
    headers: {
      "Content-Type": "application/json",
      ...(csrfToken ? { "X-CSRF-Token": csrfToken } : {}),
      ...(options.headers ?? {})
    }
  });

  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    if (response.status === 403 && data.codigo === "CSRF_INVALIDO" && repetirCsrf && geracao === geracaoSessao()) {
      csrf = null;
      return request<T>(path, options, false);
    }
    if (response.status === 401 && !publica) {
      csrf = null;
      tratarNaoAutenticado(geracao);
    }
    throw new ApiError(data.erro ?? "Erro ao comunicar com o servidor.", response.status, data.codigo);
  }
  if (path === "/auth/login" || path === "/auth/logout") csrf = null;
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const api = {
  consultarRelatorios: (inicio: string, fim: string) => request<Relatorio>(`/relatorios?${new URLSearchParams({ inicio, fim })}`),
  consultarHistoricos: (inicio: string, fim: string) => request<HistoricoRegistro[]>(`/relatorios/historicos?${new URLSearchParams({ inicio, fim })}`),
  login: (login: string, senha: string) => request<LoginResponse>("/auth/login", {
    publica: true,
    method: "POST",
    body: JSON.stringify({ login, senha })
  }),
  cadastro: (data: unknown) => request<{ mensagem: string }>("/auth/cadastro", { publica: true, method: "POST", body: JSON.stringify(data) }),
  recuperarSenha: (identificador: string, chaveSolicitacao?: string) => request<{ podeRedefinir: boolean; codigoSolicitacao?: number; mensagem: string; codigoRecuperacao?: string }>("/auth/recuperar-senha", { publica: true, method: "POST", credentials: "include", body: JSON.stringify({ identificador, chaveSolicitacao }) }),
  redefinirSenha: (codigoSolicitacao: number, novaSenha: string, chaveSolicitacao?: string) => request<{ mensagem: string }>("/auth/redefinir-senha", { publica: true, method: "POST", credentials: "include", body: JSON.stringify({ codigoSolicitacao, chaveSolicitacao, novaSenha }) }),
  listarSolicitacoesSenha: () => request<SolicitacaoSenha[]>("/redefinicoes-senha/abertas"),
  decidirSolicitacaoSenha: (id: number, aprovada: boolean) => request<{ mensagem: string; codigoRecuperacao?: string; expiraEm?: string }>(`/redefinicoes-senha/${id}`, { method: "PATCH", body: JSON.stringify({ aprovada, identidadeVerificada: aprovada }) }),
  cancelarSolicitacaoSenha: (id: number) => request<{ mensagem: string }>(`/redefinicoes-senha/${id}/cancelar`, { method: "POST" }),
  logout: () => request<{ mensagem: string }>("/auth/logout", { method: "POST" }),
  listarAgenda: () => request<Agenda[]>("/agenda"),
  detalharAgenda: (id: number) => request<Agenda>(`/agenda/${id}`),
  criarAgenda: (data: unknown) => request<Agenda>("/agenda", { method: "POST", body: JSON.stringify(data) }),
  atualizarAgenda: (id: number, data: unknown) => request<Agenda>(`/agenda/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  excluirAgenda: (id: number) => request<{ mensagem: string }>(`/agenda/${id}`, { method: "DELETE" }),
  listarTiposEvento: () => request<TipoEvento[]>("/tipos-evento"),
  listarTiposEventoAdmin: () => request<TipoEvento[]>("/tipos-evento/admin"),
  criarTipoEvento: (data: unknown) => request<TipoEvento>("/tipos-evento", { method: "POST", body: JSON.stringify(data) }),
  atualizarTipoEvento: (id: number, data: unknown) => request<TipoEvento>(`/tipos-evento/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  excluirTipoEvento: (id: number) => request<{ mensagem: string }>(`/tipos-evento/${id}`, { method: "DELETE" }),
  listarParticipacoes: (id: number) => request<ParticipacaoEvento[]>(`/agenda/${id}/participacoes`),
  atualizarMinhaParticipacao: (id: number, data: unknown) => request<ParticipacaoEvento>(`/agenda/${id}/participacoes/minha`, { method: "PATCH", body: JSON.stringify(data) }),
  atualizarParticipacao: (id: number, musicoId: number, data: unknown) => request<ParticipacaoEvento>(`/agenda/${id}/participacoes/${musicoId}`, { method: "PATCH", body: JSON.stringify(data) }),
  listarMembros: () => request<Membro[]>("/membros"),
  listarMusicos: () => request<MusicoResumo[]>("/membros/musicos"),
  criarMembro: (data: unknown) => request<Membro>("/membros", { method: "POST", body: JSON.stringify(data) }),
  atualizarMembro: (id: number, data: unknown) => request<Membro>(`/membros/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  alterarStatusMembro: (id: number, ativo: boolean) => request<Membro>(`/membros/${id}/status`, { method: "PATCH", body: JSON.stringify({ ativo }) }),
  decidirCadastro: (id: number, aprovada: boolean) => request<Membro>(`/membros/${id}/cadastro`, { method: "PATCH", body: JSON.stringify({ aprovada }) }),
  liberarIdentificadores: (id: number) => request<Membro>(`/membros/${id}/cadastro/liberar-identificadores`, { method: "POST" }),
  obterMeuPerfil: () => request<Membro>("/membros/me"),
  atualizarMeuPerfil: (data: unknown) => request<Membro>("/membros/me", { method: "PUT", body: JSON.stringify(data) }),
  listarCidades: () => request<Cidade[]>("/cidades"),
  criarCidade: (data: unknown) => request<Cidade>("/cidades", { method: "POST", body: JSON.stringify(data) }),
  atualizarCidade: (id: number, data: unknown) => request<Cidade>(`/cidades/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  excluirCidade: (id: number) => request<{ mensagem: string }>(`/cidades/${id}`, { method: "DELETE" }),
  listarLocais: () => request<LocalEvento[]>("/locais"),
  criarLocal: (data: unknown) => request<LocalEvento>("/locais", { method: "POST", body: JSON.stringify(data) }),
  atualizarLocal: (id: number, data: unknown) => request<LocalEvento>(`/locais/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  excluirLocal: (id: number) => request<{ mensagem: string }>(`/locais/${id}`, { method: "DELETE" })
};
