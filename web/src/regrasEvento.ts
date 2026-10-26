import type { Agenda } from "./types";

export function podeRegistrarPresenca(evento: Pick<Agenda, "status" | "dataHoraInicio">, agora = Date.now()) {
  const inicio = new Date(evento.dataHoraInicio).getTime();
  return evento.status !== "CANCELADO" && Number.isFinite(inicio) && inicio <= agora;
}

export function eventoTerminou(dataHoraFim: string, agora = Date.now()) {
  const fim = new Date(dataHoraFim).getTime();
  return Number.isFinite(fim) && fim <= agora;
}
