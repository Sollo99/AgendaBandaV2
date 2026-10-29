import { useEffect, useRef, useState } from "react";
import { Flex, Stack, Text } from "@chakra-ui/react";
import { api } from "../../api";
import type { Agenda, Membro, ParticipacaoAtualizacao } from "../../types";
import PresenceControls from "./PresenceControls";

export default function ParticipationList({ evento, usuario, onAtualizar }: {
  evento: Agenda; usuario: Membro; onAtualizar: (evento: Agenda) => void;
}) {
  const [processando, setProcessando] = useState(false);
  const [erro, setErro] = useState("");
  const [agora, setAgora] = useState(Date.now());
  const montado = useRef(true);
  useEffect(() => {
    montado.current = true;
    const timer = window.setInterval(() => setAgora(Date.now()), 1000);
    return () => { montado.current = false; window.clearInterval(timer); };
  }, []);

  async function atualizar(musico: number, dados: ParticipacaoAtualizacao, propria: boolean) {
    if (processando) return;
    setProcessando(true);
    setErro("");
    try {
      const atualizada = propria ? await api.atualizarMinhaParticipacao(evento.codigoAgenda, dados)
        : await api.atualizarParticipacao(evento.codigoAgenda, musico, dados);
      if (!montado.current) return;
      const participacoes = evento.participacoes.map(p => p.musico.codigo === musico ? atualizada : p);
      onAtualizar({ ...evento, participacoes, totalConfirmados: participacoes.filter(p => p.statusConfirmacao === "CONFIRMADO").length });
    } catch (e) {
      if (montado.current) setErro(e instanceof Error ? e.message : "Não foi possível atualizar a participação.");
    } finally { if (montado.current) setProcessando(false); }
  }

  if (!evento.participacoes.length) return null;
  return <Stack gap="2">
    <Text fontWeight="bold">Participações</Text>
    <Text color="fg.muted">{evento.totalConfirmados} confirmados de {evento.totalParticipantes} participantes</Text>
    {erro && <Text color="red.500" role="alert">{erro}</Text>}
    {evento.participacoes.map(p => <Flex key={p.musico.codigo} justify="space-between" align="center" gap="2" wrap="wrap">
      <Text>{p.musico.nome}: {p.statusConfirmacao.toLowerCase()}{p.presente === true ? " • presente" : p.presente === false ? " • ausente" : ""}</Text>
      <PresenceControls evento={evento} participacao={p} usuario={usuario} agora={agora} processando={processando}
        onChange={(dados, propria) => void atualizar(p.musico.codigo, dados, propria)} />
    </Flex>)}
  </Stack>;
}
