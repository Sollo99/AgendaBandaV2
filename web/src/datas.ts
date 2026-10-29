export const FUSO_AGENDA = "America/Sao_Paulo";
const partes = new Intl.DateTimeFormat("en-CA", {
  timeZone: FUSO_AGENDA, year: "numeric", month: "2-digit", day: "2-digit",
  hour: "2-digit", minute: "2-digit", second: "2-digit", hourCycle: "h23"
});

function campos(instante: number) {
  return Object.fromEntries(partes.formatToParts(instante).map(p => [p.type, p.value]));
}

/** Respostas ISO com offset e valores civis de datetime-local sempre representam Sao Paulo. */
export function instanteSaoPaulo(valor: string): number {
  if (/T.*(?:Z|[+-]\d{2}:\d{2})$/i.test(valor)) return Date.parse(valor);
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,9}))?)?$/.exec(valor);
  if (!match) return NaN;
  const [, ano, mes, dia, hora, minuto, segundo = "00", fracao = ""] = match;
  const milissegundos = Number(fracao.padEnd(3, "0").slice(0, 3));
  const civil = Date.parse(`${ano}-${mes}-${dia}T${hora}:${minuto}:${segundo}.${String(milissegundos).padStart(3, "0")}Z`);
  if (!Number.isFinite(civil)) return NaN;
  const esperado = `${ano}-${mes}-${dia}T${hora}:${minuto}:${segundo}`;
  // Busca os offsets dos dois lados de uma possivel transicao historica de horario de verao.
  const candidatos = [-86400000, 0, 86400000].map(delta => {
    const referencia = civil + delta;
    const p = campos(referencia);
    const comoUtc = Date.parse(`${p.year}-${p.month}-${p.day}T${p.hour}:${p.minute}:${p.second}Z`);
    return civil - (comoUtc - (referencia - milissegundos));
  }).filter(instante => {
    const p = campos(instante);
    return `${p.year}-${p.month}-${p.day}T${p.hour}:${p.minute}:${p.second}` === esperado;
  });
  // Horario inexistente e rejeitado; em sobreposicao historica vale o primeiro instante, como java.time.
  return candidatos.length ? Math.min(...candidatos) : NaN;
}

export function dataSaoPaulo(agora = Date.now()) {
  const p = campos(agora);
  return `${p.year}-${p.month}-${p.day}`;
}

export function inputDataHora(valor?: string) {
  if (!valor) return "";
  const instante = instanteSaoPaulo(valor);
  if (!Number.isFinite(instante)) return "";
  const p = campos(instante);
  return `${p.year}-${p.month}-${p.day}T${p.hour}:${p.minute}`;
}

export function formatarDataHora(valor: string) {
  const instante = instanteSaoPaulo(valor);
  return Number.isFinite(instante) ? new Intl.DateTimeFormat("pt-BR", { timeZone: FUSO_AGENDA,
    dateStyle: "short", timeStyle: "short" }).format(instante) : "Data inválida";
}

export function formatarHorario(valor: string) {
  const instante = instanteSaoPaulo(valor);
  return Number.isFinite(instante) ? new Intl.DateTimeFormat("pt-BR", { timeZone: FUSO_AGENDA,
    hour: "2-digit", minute: "2-digit" }).format(instante) : "";
}

/** Aritmetica de calendario em UTC, sem interpretar datas civis no fuso do navegador. */
export function diasDoCalendario(mes: string) {
  const primeiro = new Date(`${mes}-01T12:00:00Z`);
  const inicio = primeiro.getTime() - primeiro.getUTCDay() * 86400000;
  return Array.from({ length: 42 }, (_, i) => new Date(inicio + i * 86400000).toISOString().slice(0, 10));
}

export function deslocarMes(mes: string, delta: number) {
  const data = new Date(`${mes}-01T12:00:00Z`);
  data.setUTCMonth(data.getUTCMonth() + delta);
  return data.toISOString().slice(0, 7);
}

export function nomeMes(mes: string) {
  return new Intl.DateTimeFormat("pt-BR", { month: "long", year: "numeric", timeZone: "UTC" })
    .format(new Date(`${mes}-01T12:00:00Z`));
}
