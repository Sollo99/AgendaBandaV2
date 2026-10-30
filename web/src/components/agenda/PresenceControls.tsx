import { Button, HStack } from "@chakra-ui/react";
import { podeConfirmar, podeRegistrarPresenca } from "../../regrasEvento";
import type { Agenda, Membro, ParticipacaoAtualizacao, ParticipacaoEvento } from "../../types";

export default function PresenceControls({ evento, participacao, usuario, agora, processando, onChange }: {
  evento: Agenda; participacao: ParticipacaoEvento; usuario: Membro; agora: number; processando: boolean;
  onChange: (dados: ParticipacaoAtualizacao, propria: boolean) => void;
}) {
  return <HStack wrap="wrap">
    {participacao.musico.codigo === usuario.codigo && podeConfirmar(evento, agora) && <>
      <Button size="xs" variant="outline" disabled={processando} onClick={() => onChange({ statusConfirmacao: "CONFIRMADO" }, true)}>Confirmar</Button>
      <Button size="xs" variant="outline" disabled={processando} onClick={() => onChange({ statusConfirmacao: "RECUSADO" }, true)}>Recusar</Button>
    </>}
    {usuario.administrador && podeRegistrarPresenca(evento, agora) && <>
      <Button size="xs" variant="outline" disabled={processando || participacao.presente === true}
        onClick={() => onChange(participacao.statusConfirmacao === "CONFIRMADO"
          ? { presente: true } : { presente: true, statusConfirmacao: "CONFIRMADO" }, false)}>
        {participacao.statusConfirmacao === "CONFIRMADO" ? "Marcar presente" : "Confirmar e marcar presente"}
      </Button>
      <Button size="xs" variant="outline" disabled={processando || participacao.presente === false}
        onClick={() => onChange({ presente: false }, false)}>Marcar ausente</Button>
    </>}
  </HStack>;
}
