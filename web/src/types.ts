export type Membro = {
  codigo: number;
  nome: string;
  cpf: string;
  email: string;
  login: string;
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
};

export type LoginResponse = {
  token: string;
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
