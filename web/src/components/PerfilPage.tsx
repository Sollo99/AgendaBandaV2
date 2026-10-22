import { SyntheticEvent, useState } from "react";
import { Box, Button, Card, Grid, HStack, Input, Stack, Text, Textarea } from "@chakra-ui/react";
import { api } from "../api";
import type { Membro } from "../types";

export default function PerfilPage({ usuario, onAtualizar }: { usuario: Membro; onAtualizar: (usuario: Membro) => void }) {
  const [form, setForm] = useState({
    nome: usuario.nome,
    cpf: usuario.cpf,
    email: usuario.email,
    login: usuario.login,
    senhaAtual: "",
    novaSenha: "",
    instrumento: usuario.instrumento ?? "",
    biografia: usuario.biografia ?? ""
  });
  const [erro, setErro] = useState("");
  const [mensagem, setMensagem] = useState("");
  const [carregando, setCarregando] = useState(false);

  async function salvar(e: SyntheticEvent) {
    e.preventDefault();
    setErro("");
    setMensagem("");
    setCarregando(true);
    try {
      const { senhaAtual, novaSenha, ...dadosDoPerfil } = form;
      const dados = novaSenha
        ? { ...dadosDoPerfil, senhaAtual, novaSenha }
        : dadosDoPerfil;
      const atualizado = await api.atualizarMeuPerfil(dados);
      localStorage.setItem("usuario", JSON.stringify(atualizado));
      onAtualizar(atualizado);
      setForm({ ...form, senhaAtual: "", novaSenha: "" });
      setMensagem("Perfil atualizado com sucesso.");
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Não foi possível atualizar o perfil.");
    } finally {
      setCarregando(false);
    }
  }

  return (
    <Stack gap="5">
      <Box><Text fontSize="2xl" fontWeight="bold">Meu perfil</Text><Text color="fg.muted">Atualize seus dados pessoais e, se necessário, sua senha.</Text></Box>
      {erro && <Text color="red.500">{erro}</Text>}
      {mensagem && <Text color="green.600">{mensagem}</Text>}
      <Card.Root borderRadius="2xl"><Card.Body><Stack as="form" gap="4" onSubmit={salvar}>
        <Grid templateColumns={{ base: "1fr", md: "repeat(2, 1fr)" }} gap="4">
          <Box><Text mb="1">Nome</Text><Input value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} required /></Box>
          <Box><Text mb="1">CPF</Text><Input value={form.cpf} onChange={(e) => setForm({ ...form, cpf: e.target.value })} required /></Box>
          <Box><Text mb="1">E-mail</Text><Input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required /></Box>
          <Box><Text mb="1">Login</Text><Input value={form.login} onChange={(e) => setForm({ ...form, login: e.target.value })} required /></Box>
        </Grid>
        {usuario.perfil === "MUSICO" && <>
          <Text fontWeight="semibold">Dados de músico</Text>
          <Grid templateColumns={{ base: "1fr", md: "1fr 2fr" }} gap="4">
            <Box><Text mb="1">Instrumento</Text><Input value={form.instrumento} onChange={(e) => setForm({ ...form, instrumento: e.target.value })} /></Box>
            <Box><Text mb="1">Biografia</Text><Textarea value={form.biografia} onChange={(e) => setForm({ ...form, biografia: e.target.value })} /></Box>
          </Grid>
        </>}
        <Text fontWeight="semibold">Alterar senha</Text>
        <Grid templateColumns={{ base: "1fr", md: "repeat(2, 1fr)" }} gap="4">
          <Box><Text mb="1">Senha atual</Text><Input type="password" value={form.senhaAtual} onChange={(e) => setForm({ ...form, senhaAtual: e.target.value })} /></Box>
          <Box><Text mb="1">Nova senha</Text><Input type="password" minLength={6} value={form.novaSenha} onChange={(e) => setForm({ ...form, novaSenha: e.target.value })} /></Box>
        </Grid>
        <HStack><Button type="submit" loading={carregando}>Salvar alterações</Button></HStack>
      </Stack></Card.Body></Card.Root>
    </Stack>
  );
}
