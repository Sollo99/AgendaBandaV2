import { SyntheticEvent, useEffect, useState } from "react";
import { Box, Button, Card, Grid, HStack, Input, Stack, Text } from "@chakra-ui/react";
import { api } from "../api";
import type { Cidade } from "../types";

const vazio = { nome: "", uf: "" };

export default function CidadesPage() {
  const [cidades, setCidades] = useState<Cidade[]>([]);
  const [form, setForm] = useState({ ...vazio });
  const [editando, setEditando] = useState<number | null>(null);
  const [erro, setErro] = useState("");

  async function carregar() {
    setCidades(await api.listarCidades());
  }

  useEffect(() => { carregar().catch((e) => setErro(e.message)); }, []);

  async function salvar(e: SyntheticEvent) {
    e.preventDefault();
    setErro("");
    try {
      const payload = { nome: form.nome, uf: form.uf.toUpperCase() };
      if (editando) await api.atualizarCidade(editando, payload);
      else await api.criarCidade(payload);
      setForm({ ...vazio });
      setEditando(null);
      await carregar();
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Erro ao salvar cidade.");
    }
  }

  async function excluir(cidade: Cidade) {
    if (!confirm(`Excluir a cidade "${cidade.nome}/${cidade.uf}"?`)) return;
    try {
      await api.excluirCidade(cidade.codigoCidade);
      await carregar();
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Erro ao excluir cidade.");
    }
  }

  return (
    <Stack gap="5">
      <Box><Text fontSize="2xl" fontWeight="bold">Cidades</Text><Text color="fg.muted">Cidades disponíveis para locais e shows.</Text></Box>
      {erro && <Text color="red.500">{erro}</Text>}
      <Card.Root borderRadius="2xl"><Card.Body><Stack as="form" gap="4" onSubmit={salvar}><Grid templateColumns={{ base: "1fr", md: "1fr 180px" }} gap="4"><Box><Text mb="1">Nome</Text><Input maxLength={100} value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} required /></Box><Box><Text mb="1">UF</Text><Input value={form.uf} maxLength={2} onChange={(e) => setForm({ ...form, uf: e.target.value.toUpperCase() })} required /></Box></Grid><HStack><Button type="submit">{editando ? "Atualizar" : "Cadastrar"}</Button><Button type="button" variant="outline" onClick={() => { setForm({ ...vazio }); setEditando(null); }}>Limpar</Button></HStack></Stack></Card.Body></Card.Root>
      <Grid templateColumns={{ base: "1fr", md: "repeat(2, 1fr)" }} gap="3">{cidades.map((cidade) => <Card.Root key={cidade.codigoCidade} borderRadius="2xl"><Card.Body><FlexCidade cidade={cidade} onEditar={() => { setEditando(cidade.codigoCidade); setForm({ nome: cidade.nome, uf: cidade.uf }); }} onExcluir={() => excluir(cidade)} /></Card.Body></Card.Root>)}</Grid>
    </Stack>
  );
}

function FlexCidade({ cidade, onEditar, onExcluir }: { cidade: Cidade; onEditar: () => void; onExcluir: () => void }) {
  return <Stack gap="2"><Text fontWeight="bold">{cidade.nome}/{cidade.uf}</Text><HStack><Button size="sm" onClick={onEditar}>Editar</Button><Button size="sm" variant="outline" onClick={onExcluir}>Excluir</Button></HStack></Stack>;
}
