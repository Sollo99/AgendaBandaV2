import { useEffect, useRef, useState } from "react";
import { Box, Button, Card, HStack, Input, Stack, Text } from "@chakra-ui/react";
import { api } from "../api";
import type { SolicitacaoSenha } from "../types";

export default function SolicitacoesSenhaAdmin() {
  const [solicitacoes, setSolicitacoes] = useState<SolicitacaoSenha[]>([]);
  const [aberto, setAberto] = useState(false);
  const [aviso, setAviso] = useState("");
  const [erro, setErro] = useState("");
  const [processando, setProcessando] = useState<number | null>(null);
  const [credencial, setCredencial] = useState<{ codigo: string; expiraEm: string; membro: string } | null>(null);
  const iniciou = useRef(false);
  const codigosPendentes = useRef<number[]>([]);

  async function carregar() {
    const lista = await api.listarSolicitacoesSenha();
    const pendentes = lista.filter((item) => item.status === "PENDENTE");
    if (iniciou.current && pendentes.some((item) => !codigosPendentes.current.includes(item.codigoSolicitacao))) setAberto(true);
    if (!iniciou.current && pendentes.length) setAviso(`Há ${pendentes.length} solicitação(ões) de redefinição de senha pendente(s).`);
    iniciou.current = true;
    codigosPendentes.current = pendentes.map((item) => item.codigoSolicitacao);
    setSolicitacoes(lista);
  }

  useEffect(() => {
    const atualizar = () => carregar().catch((err) => setErro(err instanceof Error ? err.message : "Não foi possível consultar as recuperações."));
    void atualizar();
    const intervalo = window.setInterval(atualizar, 15000);
    return () => window.clearInterval(intervalo);
  }, []);

  async function decidir(id: number, acao: "aprovar" | "recusar" | "cancelar") {
    if (acao === "aprovar" && !window.confirm("Voce confirmou a identidade do membro presencialmente ou por contato previamente conhecido? Entregue o codigo somente a essa pessoa. Nao basta reconhecer o login ou e-mail informado no pedido.")) return;
    setProcessando(id);
    setErro("");
    try {
      if (acao === "cancelar") await api.cancelarSolicitacaoSenha(id);
      else {
        const resultado = await api.decidirSolicitacaoSenha(id, acao === "aprovar");
        if (resultado.codigoRecuperacao && resultado.expiraEm) {
          setCredencial({ codigo: resultado.codigoRecuperacao, expiraEm: resultado.expiraEm,
            membro: solicitacoes.find((item) => item.codigoSolicitacao === id)?.nomeMembro ?? "" });
        }
      }
      await carregar();
    } catch (err) {
      setErro(err instanceof Error ? err.message : "Não foi possível atualizar a solicitação.");
    } finally {
      setProcessando(null);
    }
  }

  return <>
    {credencial && <Box position="fixed" inset="0" zIndex="tooltip" bg="blackAlpha.700" display="grid" placeItems="center" p="4"><Card.Root maxW="620px" w="100%"><Card.Body><Stack gap="4">
      <Text fontWeight="bold">Codigo de redefinicao para {credencial.membro}</Text>
      <Text>Entregue por um canal conhecido somente ao membro cuja identidade foi verificada. Este codigo aparece uma unica vez. Se for perdido, cancele a solicitacao e abra outra.</Text>
      <Input value={credencial.codigo} readOnly autoComplete="off" />
      <Text>Expira em {new Date(credencial.expiraEm).toLocaleString("pt-BR")}</Text>
      <Button onClick={() => setCredencial(null)}>Concluir entrega e fechar</Button>
    </Stack></Card.Body></Card.Root></Box>}
    <Box maxW="1180px" mx="auto" px="6" pt="3"><Button size="sm" variant="outline" onClick={() => { setAviso(""); setAberto(true); }}>Recuperações de senha ({solicitacoes.length})</Button></Box>
    {aviso && <Box position="fixed" right="6" bottom="6" zIndex="toast" bg="bg.panel" borderWidth="1px" borderRadius="lg" p="4" boxShadow="lg"><Stack><Text>{aviso}</Text><HStack><Button size="sm" onClick={() => { setAviso(""); setAberto(true); }}>Ver solicitações</Button><Button size="sm" variant="ghost" onClick={() => setAviso("")}>Fechar</Button></HStack></Stack></Box>}
    {aberto && <Box position="fixed" inset="0" bg="blackAlpha.600" zIndex="modal" display="grid" placeItems="center" p="4">
      <Card.Root w="100%" maxW="620px" maxH="90vh" overflowY="auto"><Card.Body><Stack gap="4">
        <HStack justify="space-between"><Text fontSize="lg" fontWeight="bold">Recuperações abertas</Text><Button size="sm" variant="ghost" onClick={() => setAberto(false)}>Fechar</Button></HStack>
        {erro && <Text color="red.500">{erro}</Text>}
        {solicitacoes.length === 0 ? <Text>Nenhuma solicitação aberta.</Text> : solicitacoes.map((item) => <Box key={item.codigoSolicitacao} borderWidth="1px" borderRadius="md" p="3"><Stack gap="2">
          <Text fontWeight="bold">{item.nomeMembro}</Text><Text>{item.loginMembro} · {item.emailMembro}</Text>
          <Text>{item.status === "PENDENTE" ? "Aguardando aprovação" : "Aprovada, aguardando a nova senha"}</Text>
          <Text fontSize="sm">Expira em {new Date(item.expiraEm).toLocaleString("pt-BR")}</Text>
          <HStack wrap="wrap">
            {item.status === "PENDENTE" && <><Button size="sm" disabled={processando !== null} onClick={() => decidir(item.codigoSolicitacao, "aprovar")}>Aprovar</Button><Button size="sm" variant="outline" disabled={processando !== null} onClick={() => decidir(item.codigoSolicitacao, "recusar")}>Recusar</Button></>}
            <Button size="sm" variant="outline" disabled={processando !== null} onClick={() => decidir(item.codigoSolicitacao, "cancelar")}>Cancelar solicitação</Button>
          </HStack>
        </Stack></Box>)}
      </Stack></Card.Body></Card.Root>
    </Box>}
  </>;
}
