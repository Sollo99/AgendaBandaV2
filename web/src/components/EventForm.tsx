import { SyntheticEvent, useState } from "react";
import { Box, Button, Card, Grid, HStack, Input, Stack, Text, Textarea } from "@chakra-ui/react";
import { api } from "../api";
import type { Agenda, Cidade, LocalEvento, MusicoResumo, TipoEvento } from "../types";

type Props = {
  evento: Agenda | null;
  locais: LocalEvento[];
  cidades: Cidade[];
  musicos: MusicoResumo[];
  tiposEvento: TipoEvento[];
  onSaved: () => void;
  onCancel: () => void;
};

function toInputDate(value?: string) {
  if (!value) return "";
  return value.substring(0, 16);
}

export default function EventForm({ evento, locais, cidades, musicos, tiposEvento, onSaved, onCancel }: Props) {
  const tipoPadrao = evento?.tipoEvento.codigoTipo ?? tiposEvento.find((tipo) => tipo.nome === "ENSAIO")?.codigoTipo ?? "";
  const [form, setForm] = useState({
    titulo: evento?.titulo ?? "",
    descricao: evento?.descricao ?? "",
    dataHoraInicio: toInputDate(evento?.dataHoraInicio),
    dataHoraFim: toInputDate(evento?.dataHoraFim),
    localId: evento?.local.codigoLocal?.toString() ?? "",
    tipoEventoId: String(tipoPadrao),
    status: evento?.status ?? "AGENDADO",
    repertorio: evento?.repertorio ?? "",
    observacao: evento?.observacao ?? "",
    pauta: evento?.pauta ?? "",
    cidadeId: evento?.cidadeShow?.codigoCidade?.toString() ?? "",
    musicosIds: evento?.musicos.map((m) => String(m.codigo)) ?? []
  });
  const [erro, setErro] = useState("");

  const tipoSelecionado = tiposEvento.find((tipo) => tipo.codigoTipo === Number(form.tipoEventoId))?.categoriaDetalhe ?? "EVENTO";

  function update(campo: string, valor: string) {
    setForm((prev) => ({ ...prev, [campo]: valor }));
  }

  function alternarMusico(id: string) {
    setForm((prev) => ({
      ...prev,
      musicosIds: prev.musicosIds.includes(id) ? prev.musicosIds.filter((x) => x !== id) : [...prev.musicosIds, id]
    }));
  }

  async function salvar(e: SyntheticEvent) {
    e.preventDefault();
    setErro("");
    const payload = {
      ...form,
      dataHoraInicio: form.dataHoraInicio,
      dataHoraFim: form.dataHoraFim,
      localId: Number(form.localId),
      tipoEventoId: Number(form.tipoEventoId),
      cidadeId: form.cidadeId ? Number(form.cidadeId) : null,
      musicosIds: form.musicosIds.map(Number)
    };

    try {
      if (evento) await api.atualizarAgenda(evento.codigoAgenda, payload);
      else await api.criarAgenda(payload);
      onSaved();
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Erro ao salvar evento.");
    }
  }

  return (
    <Card.Root borderRadius="2xl">
      <Card.Body>
        <Stack gap="4" as="form" onSubmit={salvar}>
          <Text fontWeight="bold">{evento ? "Editar evento" : "Novo evento"}</Text>
          {erro && <Text color="red.500">{erro}</Text>}
          <Grid templateColumns={{ base: "1fr", md: "1fr 1fr" }} gap="4">
            <Box><Text mb="1">Título</Text><Input value={form.titulo} onChange={(e) => update("titulo", e.target.value)} required /></Box>
            <Box><Text mb="1">Tipo</Text><select className="native-field" value={form.tipoEventoId} onChange={(e) => update("tipoEventoId", e.target.value)} required><option value="">Selecione</option>{tiposEvento.map((tipo) => <option key={tipo.codigoTipo} value={tipo.codigoTipo}>{tipo.nome.charAt(0) + tipo.nome.slice(1).toLowerCase()}</option>)}</select></Box>
            <Box><Text mb="1">Status</Text><select className="native-field" value={form.status} onChange={(e) => update("status", e.target.value)}><option value="AGENDADO">Agendado</option><option value="CANCELADO">Cancelado</option><option value="REALIZADO">Realizado</option></select></Box>
            <Box><Text mb="1">Início</Text><Input type="datetime-local" value={form.dataHoraInicio} onChange={(e) => update("dataHoraInicio", e.target.value)} required /></Box>
            <Box><Text mb="1">Fim</Text><Input type="datetime-local" value={form.dataHoraFim} onChange={(e) => update("dataHoraFim", e.target.value)} required /></Box>
            <Box><Text mb="1">Local</Text><select className="native-field" value={form.localId} onChange={(e) => update("localId", e.target.value)} required><option value="">Selecione</option>{locais.map((l) => <option key={l.codigoLocal} value={l.codigoLocal}>{l.nome}</option>)}</select></Box>
          </Grid>
          <Box><Text mb="1">Descrição</Text><Textarea value={form.descricao} onChange={(e) => update("descricao", e.target.value)} /></Box>

          {tipoSelecionado === "ENSAIO" && (
            <Grid templateColumns={{ base: "1fr", md: "1fr 1fr" }} gap="4">
              <Box><Text mb="1">Repertório</Text><Textarea value={form.repertorio} onChange={(e) => update("repertorio", e.target.value)} /></Box>
              <Box><Text mb="1">Observação</Text><Textarea value={form.observacao} onChange={(e) => update("observacao", e.target.value)} /></Box>
            </Grid>
          )}

          {tipoSelecionado === "SHOW" && (
            <Box><Text mb="1">Cidade do show</Text><select className="native-field" value={form.cidadeId} onChange={(e) => update("cidadeId", e.target.value)} required><option value="">Selecione</option>{cidades.map((c) => <option key={c.codigoCidade} value={c.codigoCidade}>{c.nome}/{c.uf}</option>)}</select></Box>
          )}

          {tipoSelecionado === "REUNIAO" && (
            <Box><Text mb="1">Pauta</Text><Textarea value={form.pauta} onChange={(e) => update("pauta", e.target.value)} /></Box>
          )}

          <Box><Text mb="2">Músicos participantes</Text><HStack wrap="wrap">{musicos.map((m) => <Button key={m.codigo} type="button" size="sm" variant={form.musicosIds.includes(String(m.codigo)) ? "solid" : "outline"} onClick={() => alternarMusico(String(m.codigo))}>{m.nome}</Button>)}</HStack></Box>

          <HStack><Button type="submit">Salvar</Button><Button type="button" variant="outline" onClick={onCancel}>Cancelar</Button></HStack>
        </Stack>
      </Card.Body>
    </Card.Root>
  );
}
