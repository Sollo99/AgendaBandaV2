import { SyntheticEvent, useEffect, useRef, useState } from "react";
import { Box, Button, Card, Grid, HStack, Input, Stack, Text, Textarea } from "@chakra-ui/react";
import { api } from "../api";
import { eventoTerminou } from "../regrasEvento";
import { inputDataHora, instanteSaoPaulo } from "../datas";
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

export default function EventForm({ evento, locais, cidades, musicos, tiposEvento, onSaved, onCancel }: Props) {
  const tipoPadrao = evento?.tipoEvento.codigoTipo ?? tiposEvento.find((tipo) => tipo.categoriaDetalhe === "ENSAIO")?.codigoTipo ?? "";
  const [form, setForm] = useState({
    titulo: evento?.titulo ?? "",
    descricao: evento?.descricao ?? "",
    dataHoraInicio: inputDataHora(evento?.dataHoraInicio),
    dataHoraFim: inputDataHora(evento?.dataHoraFim),
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
  const [salvando, setSalvando] = useState(false);
  const montado = useRef(true);
  useEffect(() => { montado.current = true; return () => { montado.current = false; }; }, []);

  const tipoSelecionado = tiposEvento.find((tipo) => tipo.codigoTipo === Number(form.tipoEventoId))?.categoriaDetalhe ?? "EVENTO";
  // Permite remover participantes antigos que foram desativados e não estão mais na lista de novos músicos.
  const musicosDisponiveis = [...musicos, ...(evento?.musicos ?? []).filter((m) => !musicos.some((ativo) => ativo.codigo === m.codigo))];

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
    if (salvando) return;
    setErro("");
    const inicio = instanteSaoPaulo(form.dataHoraInicio), fim = instanteSaoPaulo(form.dataHoraFim);
    if (!Number.isFinite(inicio) || !Number.isFinite(fim) || fim <= inicio) {
      setErro("Informe horários válidos de São Paulo, com término posterior ao início.");
      return;
    }
    if (form.status === "REALIZADO" && !eventoTerminou(form.dataHoraFim)) {
      setErro("Um evento só pode ser marcado como realizado após seu término.");
      return;
    }
    const payload = {
      ...form,
      dataHoraInicio: form.dataHoraInicio,
      dataHoraFim: form.dataHoraFim,
      localId: Number(form.localId),
      tipoEventoId: Number(form.tipoEventoId),
      cidadeId: form.cidadeId ? Number(form.cidadeId) : null,
      musicosIds: form.musicosIds.map(Number)
    };

    setSalvando(true);
    try {
      if (evento) await api.atualizarAgenda(evento.codigoAgenda, payload);
      else await api.criarAgenda(payload);
      await onSaved();
    } catch (err) {
      if (montado.current) setErro(err instanceof Error ? err.message : "Erro ao salvar evento.");
    } finally { if (montado.current) setSalvando(false); }
  }

  return (
    <Card.Root borderRadius="2xl">
      <Card.Body>
        <Stack gap="4" as="form" onSubmit={salvar}>
          <Text fontWeight="bold">{evento ? "Editar evento" : "Novo evento"}</Text>
          {erro && <Text color="red.500">{erro}</Text>}
          <Grid templateColumns={{ base: "1fr", md: "1fr 1fr" }} gap="4">
            <Box><Text mb="1">Título</Text><Input maxLength={120} value={form.titulo} onChange={(e) => update("titulo", e.target.value)} required /></Box>
            <Box><Text mb="1">Tipo</Text><select className="native-field" value={form.tipoEventoId} onChange={(e) => update("tipoEventoId", e.target.value)} required><option value="">Selecione</option>{tiposEvento.map((tipo) => <option key={tipo.codigoTipo} value={tipo.codigoTipo}>{tipo.nome.charAt(0) + tipo.nome.slice(1).toLowerCase()}</option>)}</select></Box>
            <Box><Text mb="1">Status</Text><select className="native-field" value={form.status} disabled={evento?.status === "REALIZADO"} onChange={(e) => update("status", e.target.value)}><option value="AGENDADO">Agendado</option><option value="CANCELADO">Cancelado</option><option value="REALIZADO" disabled={!eventoTerminou(form.dataHoraFim)}>Realizado (após o término)</option></select></Box>
            <Box><Text mb="1">Início</Text><Input type="datetime-local" value={form.dataHoraInicio} onChange={(e) => update("dataHoraInicio", e.target.value)} required /></Box>
            <Box><Text mb="1">Fim</Text><Input type="datetime-local" value={form.dataHoraFim} onChange={(e) => update("dataHoraFim", e.target.value)} required /></Box>
            <Box><Text mb="1">Local</Text><select className="native-field" value={form.localId} onChange={(e) => update("localId", e.target.value)} required><option value="">Selecione</option>{locais.map((l) => <option key={l.codigoLocal} value={l.codigoLocal}>{l.nome}</option>)}</select></Box>
          </Grid>
          <Box><Text mb="1">Descrição</Text><Textarea maxLength={800} value={form.descricao} onChange={(e) => update("descricao", e.target.value)} /></Box>

          {tipoSelecionado === "ENSAIO" && (
            <Grid templateColumns={{ base: "1fr", md: "1fr 1fr" }} gap="4">
              <Box><Text mb="1">Repertório</Text><Textarea maxLength={800} value={form.repertorio} onChange={(e) => update("repertorio", e.target.value)} /></Box>
              <Box><Text mb="1">Observação</Text><Textarea maxLength={500} value={form.observacao} onChange={(e) => update("observacao", e.target.value)} /></Box>
            </Grid>
          )}

          {tipoSelecionado === "SHOW" && (
            <Box><Text mb="1">Cidade do show</Text><select className="native-field" value={form.cidadeId} onChange={(e) => update("cidadeId", e.target.value)} required><option value="">Selecione</option>{cidades.map((c) => <option key={c.codigoCidade} value={c.codigoCidade}>{c.nome}/{c.uf}</option>)}</select></Box>
          )}

          {tipoSelecionado === "REUNIAO" && (
            <Box><Text mb="1">Pauta</Text><Textarea maxLength={800} value={form.pauta} onChange={(e) => update("pauta", e.target.value)} /></Box>
          )}

          <Box><Text mb="2">Músicos participantes</Text><HStack wrap="wrap">{musicosDisponiveis.map((m) => <Button key={m.codigo} type="button" size="sm" variant={form.musicosIds.includes(String(m.codigo)) ? "solid" : "outline"} onClick={() => alternarMusico(String(m.codigo))}>{m.nome}</Button>)}</HStack></Box>

          <HStack><Button type="submit" loading={salvando}>Salvar</Button><Button type="button" variant="outline" onClick={onCancel}>Cancelar</Button></HStack>
        </Stack>
      </Card.Body>
    </Card.Root>
  );
}
