import { SyntheticEvent, useEffect, useState } from "react";
import { Box, Button, Card, Grid, HStack, Input, Stack, Text, Textarea } from "@chakra-ui/react";
import { api } from "../api";
import type { TipoEvento } from "../types";

const vazio = { nome: "", descricao: "", categoriaDetalhe: "EVENTO", ativo: true };

export default function TiposEventoPage() {
  const [tipos, setTipos] = useState<TipoEvento[]>([]);
  const [form, setForm] = useState({ ...vazio });
  const [editando, setEditando] = useState<number | null>(null);
  const [erro, setErro] = useState("");

  async function carregar() {
    setTipos(await api.listarTiposEventoAdmin());
  }

  useEffect(() => { carregar().catch((e) => setErro(e.message)); }, []);

  async function salvar(e: SyntheticEvent) {
    e.preventDefault();
    setErro("");
    try {
      const payload = { ...form, nome: form.nome.toUpperCase() };
      if (editando) await api.atualizarTipoEvento(editando, payload);
      else await api.criarTipoEvento(payload);
      setForm({ ...vazio });
      setEditando(null);
      await carregar();
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Erro ao salvar tipo de evento.");
    }
  }

  async function excluir(tipo: TipoEvento) {
    if (!confirm(`Excluir o tipo "${tipo.nome}"?`)) return;
    try {
      await api.excluirTipoEvento(tipo.codigoTipo);
      await carregar();
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Erro ao excluir tipo de evento.");
    }
  }

  return (
    <Stack gap="5">
      <Box><Text fontSize="2xl" fontWeight="bold">Tipos de evento</Text><Text color="fg.muted">Categorias disponíveis no cadastro da agenda.</Text></Box>
      {erro && <Text color="red.500">{erro}</Text>}
      <Card.Root borderRadius="2xl"><Card.Body><Stack as="form" gap="4" onSubmit={salvar}><Grid templateColumns={{ base: "1fr", md: "repeat(3, 1fr)" }} gap="4"><Box><Text mb="1">Nome</Text><Input value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value.toUpperCase() })} required /></Box><Box><Text mb="1">Descrição</Text><Textarea value={form.descricao} onChange={(e) => setForm({ ...form, descricao: e.target.value })} /></Box><Box><Text mb="1">Detalhamento</Text><select className="native-field" value={form.categoriaDetalhe} onChange={(e) => setForm({ ...form, categoriaDetalhe: e.target.value })}><option value="EVENTO">Evento comum</option><option value="ENSAIO">Ensaio</option><option value="SHOW">Show</option><option value="REUNIAO">Reunião</option></select></Box></Grid><HStack><label><input type="checkbox" checked={form.ativo} onChange={(e) => setForm({ ...form, ativo: e.target.checked })} /> Ativo</label></HStack><HStack><Button type="submit">{editando ? "Atualizar" : "Cadastrar"}</Button><Button type="button" variant="outline" onClick={() => { setForm({ ...vazio }); setEditando(null); }}>Limpar</Button></HStack></Stack></Card.Body></Card.Root>
      <Grid templateColumns={{ base: "1fr", md: "repeat(2, 1fr)" }} gap="3">{tipos.map((tipo) => <Card.Root key={tipo.codigoTipo} borderRadius="2xl"><Card.Body><Stack gap="2"><Text fontWeight="bold">{tipo.nome} {!tipo.ativo && "(inativo)"}</Text><Text>{tipo.descricao || "Sem descrição."}</Text><Text>Detalhamento: {tipo.categoriaDetalhe}</Text><HStack><Button size="sm" onClick={() => { setEditando(tipo.codigoTipo); setForm({ nome: tipo.nome, descricao: tipo.descricao ?? "", categoriaDetalhe: tipo.categoriaDetalhe, ativo: tipo.ativo }); }}>Editar</Button><Button size="sm" variant="outline" onClick={() => excluir(tipo)}>Excluir</Button></HStack></Stack></Card.Body></Card.Root>)}</Grid>
    </Stack>
  );
}
