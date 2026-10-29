import { lazy, Suspense, useCallback, useEffect, useRef, useState } from "react";
import { Box, Button, Flex, Heading, HStack, Text } from "@chakra-ui/react";
import { api, ApiError } from "./api";
import { encerrarSessao, EVENTO_SESSAO_ENCERRADA, CHAVE_MUDANCA_SESSAO, geracaoSessao, sincronizarSessao } from "./sessao";
import type { Membro } from "./types";
import LoginPage from "./components/LoginPage";
import AgendaPage from "./components/AgendaPage";
const UsersPage = lazy(() => import("./components/UsersPage"));
const LocaisPage = lazy(() => import("./components/LocaisPage"));
const CidadesPage = lazy(() => import("./components/CidadesPage"));
const TiposEventoPage = lazy(() => import("./components/TiposEventoPage"));
import PerfilPage from "./components/PerfilPage";
const RelatoriosPage = lazy(() => import("./components/RelatoriosPage"));
const SolicitacoesSenhaAdmin = lazy(() => import("./components/SolicitacoesSenhaAdmin"));
import { ColorModeButton } from "./components/ui/ColorModeButton";

type Aba = "agenda" | "perfil" | "usuarios" | "locais" | "cidades" | "tipos" | "relatorios";

export default function App() {
  const [usuario, setUsuario] = useState<Membro | null>(null);
  const [aba, setAba] = useState<Aba>("agenda");
  const [carregando, setCarregando] = useState(true);
  const [erroInicial, setErroInicial] = useState("");
  const [mensagemLogin, setMensagemLogin] = useState("");
  const validacaoAtual = useRef(0);
  const pedidoSessao = useRef<AbortController | null>(null);

  const validarSessao = useCallback(async () => {
    pedidoSessao.current?.abort();
    const controle = new AbortController();
    pedidoSessao.current = controle;
    const tentativa = ++validacaoAtual.current;
    const geracao = geracaoSessao();
    setCarregando(true);
    setErroInicial("");
    setUsuario(null);
    try {
      const perfil = await api.obterMeuPerfil(controle.signal);
      if (tentativa === validacaoAtual.current && geracao === geracaoSessao()) {
        setUsuario(perfil);
      }
    } catch (err) {
      if (!controle.signal.aborted && tentativa === validacaoAtual.current && !(err instanceof ApiError && err.status === 401)) {
        setErroInicial("Não foi possível verificar sua sessão. Confira a conexão e tente novamente.");
      }
    } finally {
      if (tentativa === validacaoAtual.current) setCarregando(false);
    }
  }, []);

  useEffect(() => {
    localStorage.removeItem("token");
    localStorage.removeItem("usuario");
    function aoEncerrar(evento: Event) {
      ++validacaoAtual.current;
      setUsuario(null);
      setAba("agenda");
      setErroInicial("");
      setCarregando(false);
      setMensagemLogin((evento as CustomEvent<string>).detail ?? "");
    }
    function aoMudarStorage(evento: StorageEvent) {
      if (evento.key === null || (evento.key === CHAVE_MUDANCA_SESSAO && !evento.newValue?.startsWith("login:"))) sincronizarSessao(false);
      else if (evento.key === CHAVE_MUDANCA_SESSAO) {
        sincronizarSessao(true);
        void validarSessao();
      }
    }
    window.addEventListener(EVENTO_SESSAO_ENCERRADA, aoEncerrar);
    window.addEventListener("storage", aoMudarStorage);
    void validarSessao();
    return () => {
      ++validacaoAtual.current;
      pedidoSessao.current?.abort();
      window.removeEventListener(EVENTO_SESSAO_ENCERRADA, aoEncerrar);
      window.removeEventListener("storage", aoMudarStorage);
    };
  }, [validarSessao]);

  async function sair() {
    try {
      await api.logout();
      encerrarSessao();
    } catch {
      setErroInicial("Nao foi possivel encerrar a sessao no servidor. Confira a conexao e tente novamente.");
    }
  }

  function atualizarUsuario(perfil: Membro) {
    setUsuario(perfil);
    if (!perfil.administrador && aba !== "agenda" && aba !== "perfil") setAba("agenda");
  }

  if (carregando) return <Box p="8"><Text>Verificando sessão...</Text></Box>;
  if (erroInicial) return <Box p="8"><Text mb="4">{erroInicial}</Text><HStack><Button onClick={() => void validarSessao()}>Tentar novamente</Button><Button variant="outline" onClick={sair}>Voltar ao login</Button></HStack></Box>;

  if (!usuario) {
    return <LoginPage mensagemInicial={mensagemLogin} onLogin={(perfil) => {
      ++validacaoAtual.current;
      setMensagemLogin("");
      setAba("agenda");
      setUsuario(perfil);
    }} />;
  }

  return (
    <Box minH="100vh" bg="bg.subtle">
      {usuario.administrador && <Suspense fallback={null}><SolicitacoesSenhaAdmin /></Suspense>}
      <Box borderBottomWidth="1px" bg="bg.panel" position="sticky" top="0" zIndex="10">
        <Flex maxW="1180px" mx="auto" px="6" py="4" justify="space-between" align="center" gap="4" wrap="wrap">
          <Box>
            <Heading size="lg">Agenda da Banda</Heading>
            <Text color="fg.muted">Logado como {usuario.nome}</Text>
          </Box>
          <HStack wrap="wrap">
            <Button variant={aba === "agenda" ? "solid" : "outline"} onClick={() => setAba("agenda")}>Agenda</Button>
            <Button variant={aba === "perfil" ? "solid" : "outline"} onClick={() => setAba("perfil")}>Meu perfil</Button>
            {usuario.administrador && <Button variant={aba === "usuarios" ? "solid" : "outline"} onClick={() => setAba("usuarios")}>Usuários</Button>}
            {usuario.administrador && <Button variant={aba === "locais" ? "solid" : "outline"} onClick={() => setAba("locais")}>Locais</Button>}
            {usuario.administrador && <Button variant={aba === "cidades" ? "solid" : "outline"} onClick={() => setAba("cidades")}>Cidades</Button>}
            {usuario.administrador && <Button variant={aba === "tipos" ? "solid" : "outline"} onClick={() => setAba("tipos")}>Tipos de evento</Button>}
            {usuario.administrador && <Button variant={aba === "relatorios" ? "solid" : "outline"} onClick={() => setAba("relatorios")}>Relatórios</Button>}
            <ColorModeButton />
            <Button variant="ghost" onClick={sair}>Sair</Button>
          </HStack>
        </Flex>
      </Box>

      <Box maxW="1180px" mx="auto" p="6"><Suspense fallback={<Text>Carregando tela...</Text>}>
        {aba === "agenda" && <AgendaPage usuario={usuario} />}
        {aba === "perfil" && <PerfilPage usuario={usuario} onAtualizar={atualizarUsuario} />}
        {aba === "usuarios" && usuario.administrador && <UsersPage usuario={usuario} onAtualizar={atualizarUsuario} />}
        {aba === "locais" && usuario.administrador && <LocaisPage />}
        {aba === "cidades" && usuario.administrador && <CidadesPage />}
        {aba === "tipos" && usuario.administrador && <TiposEventoPage />}
        {aba === "relatorios" && usuario.administrador && <RelatoriosPage />}
      </Suspense></Box>
    </Box>
  );
}
