import { SyntheticEvent, useState } from "react";
import { Box, Button, Card, Heading, Input, Stack, Text } from "@chakra-ui/react";
import { api } from "../api";
import type { Membro } from "../types";

type Modo = "login" | "cadastro" | "recuperar" | "redefinir";

export default function LoginPage({ onLogin }: { onLogin: (usuario: Membro) => void }) {
  const [modo, setModo] = useState<Modo>("login");
  const [login, setLogin] = useState("usuario");
  const [senha, setSenha] = useState("usuario");
  const [cadastro, setCadastro] = useState({ nome: "", cpf: "", email: "", login: "", senha: "" });
  const [identificador, setIdentificador] = useState("");
  const [codigoSolicitacao, setCodigoSolicitacao] = useState<number | null>(null);
  const [novaSenha, setNovaSenha] = useState("");
  const [confirmacaoSenha, setConfirmacaoSenha] = useState("");
  const [erro, setErro] = useState("");
  const [mensagem, setMensagem] = useState("");
  const [carregando, setCarregando] = useState(false);

  function trocarModo(novoModo: Modo) {
    setModo(novoModo);
    setErro("");
    setMensagem("");
  }

  async function entrar(e: SyntheticEvent) {
    e.preventDefault();
    setErro("");
    setCarregando(true);
    try {
      const resposta = await api.login(login, senha);
      localStorage.setItem("token", resposta.token);
      localStorage.setItem("usuario", JSON.stringify(resposta.usuario));
      onLogin(resposta.usuario);
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Falha no login.");
    } finally {
      setCarregando(false);
    }
  }

  async function cadastrar(e: SyntheticEvent) {
    e.preventDefault();
    setErro("");
    setCarregando(true);
    try {
      await api.cadastro(cadastro);
      setLogin(cadastro.login);
      setSenha("");
      setMensagem("Cadastro realizado. Entre com seu login e senha.");
      setModo("login");
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Não foi possível criar a conta.");
    } finally {
      setCarregando(false);
    }
  }

  async function solicitarRedefinicao(e: SyntheticEvent) {
    e.preventDefault();
    setErro("");
    setCarregando(true);
    try {
      const chaveStorage = `redefinicao-senha:${identificador.trim().toLowerCase()}`;
      let chaveSolicitacao = localStorage.getItem(chaveStorage);
      if (!chaveSolicitacao) {
        chaveSolicitacao = crypto.randomUUID();
        localStorage.setItem(chaveStorage, chaveSolicitacao);
      }
      const resposta = await api.recuperarSenha(identificador, chaveSolicitacao);
      setMensagem(resposta.mensagem);
      if (resposta.podeRedefinir && resposta.codigoSolicitacao) {
        setCodigoSolicitacao(resposta.codigoSolicitacao);
        setModo("redefinir");
      }
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Não foi possível solicitar a redefinição.");
    } finally {
      setCarregando(false);
    }
  }

  async function redefinirSenha(e: SyntheticEvent) {
    e.preventDefault();
    setErro("");
    if (novaSenha !== confirmacaoSenha) {
      setErro("A confirmação da nova senha não confere.");
      return;
    }
    setCarregando(true);
    try {
      if (!codigoSolicitacao) throw new Error("Solicitação de redefinição inválida.");
      const chaveSolicitacao = localStorage.getItem(`redefinicao-senha:${identificador.trim().toLowerCase()}`);
      if (!chaveSolicitacao) throw new Error("Não foi possível validar esta solicitação neste dispositivo.");
      const resposta = await api.redefinirSenha(codigoSolicitacao, chaveSolicitacao, novaSenha);
      localStorage.removeItem(`redefinicao-senha:${identificador.trim().toLowerCase()}`);
      setMensagem(resposta.mensagem);
      setSenha("");
      setModo("login");
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Não foi possível redefinir a senha.");
    } finally {
      setCarregando(false);
    }
  }

  return (
    <Box minH="100vh" display="grid" placeItems="center" bg="bg.subtle" p="6">
      <Card.Root w="100%" maxW="480px" borderRadius="2xl" boxShadow="lg">
        <Card.Body p="8">
          <Stack gap="5">
            <Box>
              <Heading>Agenda da Banda</Heading>
              <Text color="fg.muted">{modo === "login" ? "Acesse com login e senha para visualizar a agenda." : modo === "cadastro" ? "Crie sua conta de membro." : modo === "recuperar" ? "Informe seu login ou e-mail para solicitar a redefinição." : "Defina uma nova senha para sua conta."}</Text>
            </Box>
            {mensagem && <Text color="green.600">{mensagem}</Text>}
            {erro && <Text color="red.500">{erro}</Text>}

            {modo === "login" && <Stack gap="4" as="form" onSubmit={entrar}>
              <Box><Text mb="2">Login</Text><Input value={login} onChange={(e) => setLogin(e.target.value)} /></Box>
              <Box><Text mb="2">Senha</Text><Input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} /></Box>
              <Button type="submit" loading={carregando}>Entrar</Button>
              <Button type="button" variant="outline" onClick={() => trocarModo("cadastro")}>Criar conta</Button>
              <Button type="button" variant="ghost" onClick={() => trocarModo("recuperar")}>Esqueci minha senha</Button>
            </Stack>}

            {modo === "cadastro" && <Stack gap="4" as="form" onSubmit={cadastrar}>
              <Box><Text mb="2">Nome</Text><Input value={cadastro.nome} onChange={(e) => setCadastro({ ...cadastro, nome: e.target.value })} required /></Box>
              <Box><Text mb="2">CPF</Text><Input value={cadastro.cpf} onChange={(e) => setCadastro({ ...cadastro, cpf: e.target.value })} required /></Box>
              <Box><Text mb="2">E-mail</Text><Input type="email" value={cadastro.email} onChange={(e) => setCadastro({ ...cadastro, email: e.target.value })} required /></Box>
              <Box><Text mb="2">Login</Text><Input value={cadastro.login} onChange={(e) => setCadastro({ ...cadastro, login: e.target.value })} required /></Box>
              <Box><Text mb="2">Senha</Text><Input type="password" minLength={6} value={cadastro.senha} onChange={(e) => setCadastro({ ...cadastro, senha: e.target.value })} required /></Box>
              <Button type="submit" loading={carregando}>Cadastrar</Button>
              <Button type="button" variant="ghost" onClick={() => trocarModo("login")}>Voltar ao login</Button>
            </Stack>}

            {modo === "recuperar" && <Stack gap="4" as="form" onSubmit={solicitarRedefinicao}>
              <Box><Text mb="2">Login ou e-mail</Text><Input value={identificador} onChange={(e) => setIdentificador(e.target.value)} required /></Box>
              <Button type="submit" loading={carregando}>Solicitar redefinição</Button>
              <Button type="button" variant="ghost" onClick={() => trocarModo("login")}>Voltar ao login</Button>
            </Stack>}

            {modo === "redefinir" && <Stack gap="4" as="form" onSubmit={redefinirSenha}>
              <Box><Text mb="2">Nova senha</Text><Input type="password" minLength={6} value={novaSenha} onChange={(e) => setNovaSenha(e.target.value)} required /></Box>
              <Box><Text mb="2">Confirmar nova senha</Text><Input type="password" minLength={6} value={confirmacaoSenha} onChange={(e) => setConfirmacaoSenha(e.target.value)} required /></Box>
              <Button type="submit" loading={carregando}>Redefinir senha</Button>
              <Button type="button" variant="ghost" onClick={() => trocarModo("login")}>Voltar ao login</Button>
            </Stack>}
          </Stack>
        </Card.Body>
      </Card.Root>
    </Box>
  );
}
