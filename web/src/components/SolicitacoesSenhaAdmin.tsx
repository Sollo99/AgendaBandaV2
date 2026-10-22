import { useEffect, useRef, useState } from "react";
import { Box, Button, Card, HStack, Stack, Text } from "@chakra-ui/react";
import { api } from "../api";
import type { SolicitacaoSenha } from "../types";

export default function SolicitacoesSenhaAdmin() {
  const [pendentes, setPendentes] = useState<SolicitacaoSenha[]>([]);
  const [aberto, setAberto] = useState(false);
  const [aviso, setAviso] = useState("");
  const iniciou = useRef(false);
  const codigosPendentes = useRef<number[]>([]);

  async function carregar() {
    const lista = await api.listarSolicitacoesSenha();
    if (iniciou.current && lista.some((item) => !codigosPendentes.current.includes(item.codigoSolicitacao))) setAberto(true);
    if (!iniciou.current && lista.length) setAviso(`Há ${lista.length} solicitação(ões) de redefinição de senha pendente(s).`);
    iniciou.current = true;
    codigosPendentes.current = lista.map((item) => item.codigoSolicitacao);
    setPendentes(lista);
  }

  useEffect(() => {
    carregar().catch(() => undefined);
    const intervalo = window.setInterval(() => carregar().catch(() => undefined), 15000);
    return () => window.clearInterval(intervalo);
  }, []);

  async function decidir(id: number, aprovada: boolean) {
    await api.decidirSolicitacaoSenha(id, aprovada);
    await carregar();
  }

  return <>
    {aviso && <Box position="fixed" right="6" bottom="6" zIndex="toast" bg="bg.panel" borderWidth="1px" borderRadius="lg" p="4" boxShadow="lg"><Stack><Text>{aviso}</Text><HStack><Button size="sm" onClick={() => { setAviso(""); setAberto(true); }}>Ver solicitações</Button><Button size="sm" variant="ghost" onClick={() => setAviso("")}>Fechar</Button></HStack></Stack></Box>}
    {aberto && <Box position="fixed" inset="0" bg="blackAlpha.600" zIndex="modal" display="grid" placeItems="center" p="4"><Card.Root w="100%" maxW="620px"><Card.Body><Stack gap="4"><HStack justify="space-between"><Text fontSize="lg" fontWeight="bold">Solicitações de senha</Text><Button size="sm" variant="ghost" onClick={() => setAberto(false)}>Fechar</Button></HStack>{pendentes.length === 0 ? <Text>Nenhuma solicitação pendente.</Text> : pendentes.map((item) => <Box key={item.codigoSolicitacao} borderWidth="1px" borderRadius="md" p="3"><Stack gap="2"><Text fontWeight="bold">{item.nomeMembro}</Text><Text>{item.loginMembro} · {item.emailMembro}</Text><HStack><Button size="sm" onClick={() => decidir(item.codigoSolicitacao, true)}>Aprovar</Button><Button size="sm" variant="outline" onClick={() => decidir(item.codigoSolicitacao, false)}>Recusar</Button></HStack></Stack></Box>)}</Stack></Card.Body></Card.Root></Box>}
  </>;
}
