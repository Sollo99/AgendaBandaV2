import { SyntheticEvent, useEffect, useState } from "react";
import { Box, Button, Card, Grid, HStack, Input, Stack, Text } from "@chakra-ui/react";
import { api } from "../api";
import { encerrarSessao } from "../sessao";
import type { Membro } from "../types";

const vazio = { nome: "", cpf: "", email: "", login: "", senha: "", administrador: false, ativo: true, perfil: "MEMBRO", instrumento: "", biografia: "", especialidade: "" };

export default function UsersPage({ usuario, onAtualizar }: { usuario: Membro; onAtualizar: (usuario: Membro) => void }) {
  const [membros, setMembros] = useState<Membro[]>([]);
  const [form, setForm] = useState({ ...vazio });
  const [editando, setEditando] = useState<number | null>(null);
  const [erro, setErro] = useState("");
  const [analisando, setAnalisando] = useState<number | null>(null);

  async function carregar() {
    setMembros(await api.listarMembros());
  }

  useEffect(() => { carregar().catch((e) => setErro(e.message)); }, []);

  function preencher(m: Membro) {
    setEditando(m.codigo);
    setForm({
      nome: m.nome, cpf: m.cpf ?? "", email: m.email ?? "", login: m.login ?? "", senha: "", administrador: m.administrador,
      ativo: m.ativo, perfil: m.perfil, instrumento: m.instrumento ?? "", biografia: m.biografia ?? "", especialidade: m.especialidade ?? ""
    });
  }

  async function salvar(e: SyntheticEvent) {
    e.preventDefault();
    setErro("");
    try {
      if (editando) {
        const atualizado = await api.atualizarMembro(editando, form);
        if (editando === usuario.codigo) {
          if (!atualizado.ativo || form.senha) {
            encerrarSessao(!atualizado.ativo ? "Sua conta foi desativada." : "Senha alterada. Entre novamente com a nova senha.");
            return;
          }
          onAtualizar(atualizado);
          if (!atualizado.administrador) return;
        }
      }
      else await api.criarMembro(form);
      setForm({ ...vazio });
      setEditando(null);
      await carregar();
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Erro ao salvar usuário.");
    }
  }

  function update(campo: string, valor: string | boolean) {
    setForm((prev) => ({ ...prev, [campo]: valor }));
  }

  async function alterarStatus(membro: Membro) {
    setErro("");
    try {
      const atualizado = await api.alterarStatusMembro(membro.codigo, !membro.ativo);
      if (membro.codigo === usuario.codigo) {
        if (!atualizado.ativo) {
          encerrarSessao("Sua conta foi desativada.");
          return;
        }
        onAtualizar(atualizado);
        if (!atualizado.administrador) return;
      }
      await carregar();
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Erro ao alterar o status do membro.");
    }
  }

  async function decidirCadastro(membro: Membro, aprovada: boolean) {
    setErro("");
    setAnalisando(membro.codigo);
    try {
      await api.decidirCadastro(membro.codigo, aprovada);
      await carregar();
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Não foi possível analisar o cadastro.");
    } finally {
      setAnalisando(null);
    }
  }

  async function liberarIdentificadores(membro: Membro) {
    if (!confirm(`Liberar CPF, e-mail e login de "${membro.nome}" para novo cadastro? A recusa será preservada e este pedido não poderá mais ser aprovado.`)) return;
    setErro("");
    setAnalisando(membro.codigo);
    try {
      await api.liberarIdentificadores(membro.codigo);
      await carregar();
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Não foi possível liberar os identificadores.");
    } finally {
      setAnalisando(null);
    }
  }

  return (
    <Stack gap="5">
      <Box><Text fontSize="2xl" fontWeight="bold">Gerenciamento de usuários</Text><Text color="fg.muted">Apenas administradores acessam esta tela. Desative membros para preservar os vínculos e históricos.</Text></Box>
      {erro && <Text color="red.500">{erro}</Text>}
      <Card.Root borderRadius="2xl"><Card.Body>
        <Stack as="form" gap="4" onSubmit={salvar}>
          <Grid templateColumns={{ base: "1fr", md: "repeat(3, 1fr)" }} gap="4">
            <Box><Text mb="1">Nome</Text><Input maxLength={100} value={form.nome} onChange={(e) => update("nome", e.target.value)} required /></Box>
            <Box><Text mb="1">CPF</Text><Input maxLength={14} value={form.cpf} onChange={(e) => update("cpf", e.target.value)} required /></Box>
            <Box><Text mb="1">E-mail</Text><Input maxLength={120} type="email" value={form.email} onChange={(e) => update("email", e.target.value)} required /></Box>
            <Box><Text mb="1">Login</Text><Input maxLength={50} value={form.login} onChange={(e) => update("login", e.target.value)} required /></Box>
            <Box><Text mb="1">Senha {editando && "(opcional)"}</Text><Input maxLength={72} minLength={6} required={!editando} type="password" value={form.senha} onChange={(e) => update("senha", e.target.value)} /></Box>
            <Box><Text mb="1">Perfil</Text><select className="native-field" value={form.perfil} onChange={(e) => update("perfil", e.target.value)}><option value="MEMBRO">Membro</option><option value="MUSICO">Músico</option><option value="PRODUTOR">Produtor</option></select></Box>
            {form.perfil === "MUSICO" && <Box><Text mb="1">Instrumento</Text><Input maxLength={80} value={form.instrumento} onChange={(e) => update("instrumento", e.target.value)} /></Box>}
            {form.perfil === "PRODUTOR" && <Box><Text mb="1">Especialidade</Text><Input maxLength={100} value={form.especialidade} onChange={(e) => update("especialidade", e.target.value)} /></Box>}
          </Grid>
          <HStack><label><input type="checkbox" checked={form.administrador} onChange={(e) => update("administrador", e.target.checked)} /> Administrador</label><label><input type="checkbox" checked={form.ativo} onChange={(e) => update("ativo", e.target.checked)} /> Ativo</label></HStack>
          <HStack><Button type="submit">{editando ? "Atualizar" : "Cadastrar"}</Button><Button type="button" variant="outline" onClick={() => { setEditando(null); setForm({ ...vazio }); }}>Limpar</Button></HStack>
        </Stack>
      </Card.Body></Card.Root>
      <Grid templateColumns={{ base: "1fr", md: "repeat(2, 1fr)" }} gap="3">
        {membros.map((m) => <Card.Root key={m.codigo} borderRadius="2xl"><Card.Body><Stack gap="2">
          <Text fontWeight="bold">{m.nome}</Text><Text>{m.login === null ? "Identificadores liberados para novo cadastro" : `${m.login} • ${m.email}`}</Text>
          <Text>{m.perfil} {m.administrador ? "• Admin" : ""} • {m.statusCadastro === "PENDENTE" ? "Cadastro pendente" : m.statusCadastro === "RECUSADO" ? "Cadastro recusado" : m.ativo ? "Ativo" : "Inativo"}</Text>
          {m.statusCadastro === "APROVADO" ? <HStack><Button size="sm" onClick={() => preencher(m)}>Editar</Button><Button size="sm" variant="outline" onClick={() => alterarStatus(m)}>{m.ativo ? "Desativar" : "Ativar"}</Button></HStack> : m.login !== null && <HStack wrap="wrap">
            <Button size="sm" disabled={analisando !== null} onClick={() => decidirCadastro(m, true)}>Aprovar cadastro</Button>
            {m.statusCadastro === "PENDENTE" && <Button size="sm" variant="outline" disabled={analisando !== null} onClick={() => decidirCadastro(m, false)}>Recusar cadastro</Button>}
            {m.statusCadastro === "RECUSADO" && <Button size="sm" variant="outline" disabled={analisando !== null} onClick={() => liberarIdentificadores(m)}>Liberar identificadores</Button>}
          </HStack>}
        </Stack></Card.Body></Card.Root>)}
      </Grid>
    </Stack>
  );
}
