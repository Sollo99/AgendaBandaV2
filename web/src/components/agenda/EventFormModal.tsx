import { Button, Stack, Text } from "@chakra-ui/react";
import { api } from "../../api";
import { useConsulta } from "../../hooks/useConsulta";
import type { Agenda } from "../../types";
import EventForm from "../EventForm";
import ModalShell from "./ModalShell";

export default function EventFormModal({ evento, onClose, onSaved }: {
  evento: Agenda | null; onClose: () => void; onSaved: () => void;
}) {
  const { dados, erro, carregando, recarregar } = useConsulta(signal => Promise.all([
    api.listarMusicos(signal), api.listarLocais(signal), api.listarCidades(signal), api.listarTiposEvento(signal)
  ]));
  return <ModalShell onClose={onClose}>
    {carregando ? <Stack bg="bg.panel" p="6"><Text>Carregando formulário...</Text></Stack>
      : erro ? <Stack bg="bg.panel" p="6"><Text color="red.500">{erro}</Text><Button onClick={() => void recarregar()}>Tentar novamente</Button></Stack>
      : dados && <EventForm evento={evento} musicos={dados[0]} locais={dados[1]} cidades={dados[2]} tiposEvento={dados[3]}
        onCancel={onClose} onSaved={onSaved} />}
  </ModalShell>;
}
