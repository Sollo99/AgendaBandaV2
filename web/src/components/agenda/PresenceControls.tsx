import { Button, HStack } from "@chakra-ui/react";
import { podeConfirmar, podeRegistrarPresenca } from "../../regrasEvento";
import type { Agenda, Membro, ParticipacaoAtualizacao, ParticipacaoEvento } from "../../types";

export default function PresenceControls({ evento, participacao, usuario, agora, processando, onChange }: {
  evento: Agenda; participacao: ParticipacaoEvento; usuario: Membro; agora: number; processando: boolean;
  onChange: (dados: ParticipacaoAtualizacao, propria: boolean) => void;
}) {
  return <HStack>
    {participacao.musico.codigo === usuario.codigo && podeConfirmar(evento, agora) && <>
      <Button size="xs" variant="outline" disabled={processando} onClick={() => onChange({ statusConfirmacao: "CONFIRMADO" }, true)}>Confirmar</Button>
      <Button size="xs" variant="outline" disabled={processando} onClick={() => onChange({ statusConfirmacao: "RECUSADO" }, true)}>Recusar</Button>
    </>}
    {usuario.administrador && podeRegistrarPresenca(evento, agora) && <Button size="xs" variant="outline" disabled={processando}
      onClick={() => onChange(participacao.presente === true ? { presente: false } : { presente: true, statusConfirmacao: "CONFIRMADO" }, false)}>
      {participacao.presente === true ? "Marcar ausente" : participacao.statusConfirmacao === "CONFIRMADO" ? "Marcar presente" : "Confirmar e marcar presente"}
    </Button>}
  </HStack>;
}
