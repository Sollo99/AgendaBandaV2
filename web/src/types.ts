export type Membro = {
  codigo: number;
  nome: string;
  cpf: string | null;
  email: string | null;
  login: string | null;
  administrador: boolean;
  ativo: boolean;
  statusCadastro: "PENDENTE" | "APROVADO" | "RECUSADO";
  perfil: "MEMBRO" | "MUSICO" | "PRODUTOR";
  instrumento?: string;
  biografia?: string;
  especialidade?: string;
};

export type MusicoResumo = {
  codigo: number;
  nome: string;
  instrumento?: string;
};

export type MembroResumo = Pick<Membro, "codigo" | "nome">;

export type Cidade = {
  codigoCidade: number;
  nome: string;
  uf: string;
};

export type LocalEvento = {
  codigoLocal: number;
  nome: string;
  endereco: string;
  complemento?: string;
  cidadeId: number;
  cidadeNome: string;
  uf: string;
};

export type TipoEvento = {
  codigoTipo: number;
  nome: string;
  descricao?: string;
  categoriaDetalhe: "EVENTO" | "ENSAIO" | "SHOW" | "REUNIAO";
  ativo: boolean;
};

export type ParticipacaoEvento = {
  codigoAgenda: number;
  musico: MusicoResumo;
  statusConfirmacao: "PENDENTE" | "CONFIRMADO" | "RECUSADO";
  presente?: boolean;
  dataResposta?: string;
  observacao?: string;
};

export type Agenda = {
  codigoAgenda: number;
  titulo: string;
  descricao?: string;
  dataHoraInicio: string;
  dataHoraFim: string;
  local: LocalEvento;
  criador: MembroResumo;
  tipoEvento: TipoEvento;
  status: "AGENDADO" | "CANCELADO" | "REALIZADO";
  tipoEspecifico: string;
  repertorio?: string;
  observacao?: string;
  pauta?: string;
  cidadeShow?: Cidade;
  musicos: MusicoResumo[];
  participacoes: ParticipacaoEvento[];
  totalParticipantes: number;
  totalConfirmados: number;
};

export type LoginResponse = {
  usuario: Membro;
};

export type SolicitacaoSenha = {
  codigoSolicitacao: number;
  codigoMembro: number;
  nomeMembro: string;
  loginMembro: string;
  emailMembro: string;
  solicitadoEm: string;
  status: "PENDENTE" | "APROVADA";
  expiraEm: string;
};

export type ResumoTipo = {
  codigoTipo: number; tipo: string; total: number; agendados: number;
  realizados: number; cancelados: number; minutosProgramados: number;
};
export type PresencaMusico = {
  codigoMusico: number; nome: string; instrumento: string; eventos: number;
  realizados: number; presencas: number; ausencias: number; semRegistro: number; taxaPresenca: number | null;
};
export type OcupacaoLocal = {
  codigoLocal: number; nome: string; cidade: string; uf: string; eventos: number;
  agendados: number; realizados: number; cancelados: number; minutosProgramados: number;
};
export type Relatorio = {
  inicio: string; fim: string; resumo: ResumoTipo[]; presencas: PresencaMusico[]; locais: OcupacaoLocal[];
};
export type HistoricoRegistro = {
  codigo: number; entidade: string; registro: string; operacao: string; dataHora: string;
  codigoResponsavel: number | null; responsavel: string | null; dadosAnteriores: string | null; dadosNovos: string | null;
};
export type Pagina<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number };
export type ParticipacaoAtualizacao = { statusConfirmacao?: ParticipacaoEvento["statusConfirmacao"]; presente?: boolean; observacao?: string };
