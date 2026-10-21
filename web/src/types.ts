export type Membro = {
  codigo: number;
  nome: string;
  cpf: string;
  email: string;
  login: string;
  administrador: boolean;
  ativo: boolean;
  perfil: "MEMBRO" | "MUSICO" | "PRODUTOR";
  instrumento?: string;
  biografia?: string;
  especialidade?: string;
};

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
  ativo: boolean;
};

export type ParticipacaoEvento = {
  codigoAgenda: number;
  musico: Membro;
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
  criador: Membro;
  tipoEvento: TipoEvento;
  status: "AGENDADO" | "CANCELADO" | "REALIZADO";
  tipoEspecifico: string;
  repertorio?: string;
  observacao?: string;
  pauta?: string;
  cidadeShow?: Cidade;
  musicos: Membro[];
  participacoes: ParticipacaoEvento[];
};

export type LoginResponse = {
  token: string;
  usuario: Membro;
};
