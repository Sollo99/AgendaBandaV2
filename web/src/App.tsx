import { useCallback, useEffect, useRef, useState } from "react";
import { Box, Button, Flex, Heading, HStack, Text } from "@chakra-ui/react";
import { api, ApiError } from "./api";
import { encerrarSessao, EVENTO_SESSAO_ENCERRADA } from "./sessao";
import type { Membro } from "./types";
import LoginPage from "./components/LoginPage";
import AgendaPage from "./components/AgendaPage";
import UsersPage from "./components/UsersPage";
import LocaisPage from "./components/LocaisPage";
import CidadesPage from "./components/CidadesPage";
import TiposEventoPage from "./components/TiposEventoPage";
import PerfilPage from "./components/PerfilPage";
import SolicitacoesSenhaAdmin from "./components/SolicitacoesSenhaAdmin";
import { ColorModeButton } from "./components/ui/ColorModeButton";

type Aba = "agenda" | "perfil" | "usuarios" | "locais" | "cidades" | "tipos";

export default function App() {
  const [usuario, setUsuario] = useState<Membro | null>(null);
  const [aba, setAba] = useState<Aba>("agenda");
  const [carregando, setCarregando] = useState(true);
  const [erroInicial, setErroInicial] = useState("");
  const [mensagemLogin, setMensagemLogin] = useState("");
  const validacaoAtual = useRef(0);

  const validarSessao = useCallback(async () => {
    const tentativa = ++validacaoAtual.current;
    const token = localStorage.getItem("token");
    setCarregando(true);
    setErroInicial("");
    setUsuario(null);
    if (!token) {
      localStorage.removeItem("usuario");
      setCarregando(false);
      return;
    }
    try {
      const perfil = await api.obterMeuPerfil();
      if (tentativa === validacaoAtual.current && token === localStorage.getItem("token")) {
        localStorage.setItem("usuario", JSON.stringify(perfil));
        setUsuario(perfil);
      }
    } catch (err) {
      if (tentativa === validacaoAtual.current && !(err instanceof ApiError && err.status === 401)) {
        setErroInicial("Não foi possível verificar sua sessão. Confira a conexão e tente novamente.");
      }
    } finally {
      if (tentativa === validacaoAtual.current) setCarregando(false);
    }
  }, []);

  useEffect(() => {
    function aoEncerrar(evento: Event) {
      ++validacaoAtual.current;
      setUsuario(null);
      setAba("agenda");
      setErroInicial("");
      setCarregando(false);
      setMensagemLogin((evento as CustomEvent<string>).detail ?? "");
    }
    function aoMudarStorage(evento: StorageEvent) {
      if (evento.key === "token" || evento.key === null) {
        if (!localStorage.getItem("token")) aoEncerrar(new CustomEvent(EVENTO_SESSAO_ENCERRADA));
        else void validarSessao();
      }
    }
    window.addEventListener(EVENTO_SESSAO_ENCERRADA, aoEncerrar);
    window.addEventListener("storage", aoMudarStorage);
    void validarSessao();
    return () => {
      ++validacaoAtual.current;
      window.removeEventListener(EVENTO_SESSAO_ENCERRADA, aoEncerrar);
      window.removeEventListener("storage", aoMudarStorage);
    };
  }, [validarSessao]);

  function sair() {
    const pedido = api.logout();
    encerrarSessao();
    void pedido.catch(() => undefined);
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
      {usuario.administrador && <SolicitacoesSenhaAdmin />}
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
            <ColorModeButton />
            <Button variant="ghost" onClick={sair}>Sair</Button>
          </HStack>
        </Flex>
      </Box>

      <Box maxW="1180px" mx="auto" p="6">
        {aba === "agenda" && <AgendaPage usuario={usuario} />}
        {aba === "perfil" && <PerfilPage usuario={usuario} onAtualizar={setUsuario} />}
        {aba === "usuarios" && usuario.administrador && <UsersPage usuario={usuario} />}
        {aba === "locais" && usuario.administrador && <LocaisPage />}
        {aba === "cidades" && usuario.administrador && <CidadesPage />}
        {aba === "tipos" && usuario.administrador && <TiposEventoPage />}
      </Box>
    </Box>
  );
}
