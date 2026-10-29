import type { Agenda } from "./types";
import { instanteSaoPaulo } from "./datas";

export function podeRegistrarPresenca(evento: Pick<Agenda, "status" | "dataHoraInicio">, agora = Date.now()) {
  const inicio = instanteSaoPaulo(evento.dataHoraInicio);
  return evento.status !== "CANCELADO" && Number.isFinite(inicio) && inicio <= agora;
}

export function eventoTerminou(dataHoraFim: string, agora = Date.now()) {
  const fim = instanteSaoPaulo(dataHoraFim);
  return Number.isFinite(fim) && fim <= agora;
}
export function podeConfirmar(evento: Pick<Agenda, "status" | "dataHoraInicio">, agora = Date.now()) {
  return evento.status === "AGENDADO" && instanteSaoPaulo(evento.dataHoraInicio) > agora;
}
