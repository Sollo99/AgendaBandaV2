import { useEffect, useRef, useState } from "react";
import type { ReactNode, SyntheticEvent } from "react";
import { Box, Button, Card, Grid, Heading, HStack, Input, Stack, Text } from "@chakra-ui/react";
import { api } from "../api";
import { dataSaoPaulo, formatarDataHora } from "../datas";
import type { HistoricoRegistro, Relatorio } from "../types";

function Tabela({ colunas, children }: { colunas: string[]; children: ReactNode }) {
  return <Box overflowX="auto"><table className="relatorio-tabela"><thead><tr>
    {colunas.map(c => <th key={c} scope="col">{c}</th>)}
  </tr></thead><tbody>{children}</tbody></table></Box>;
}

function json(valor: string | null) {
  if (!valor) return "Sem registro";
  try { return JSON.stringify(JSON.parse(valor), null, 2); }
  catch { return valor; }
}

export default function RelatoriosPage() {
  const ano = Number(dataSaoPaulo().slice(0, 4));
  const [inicio, setInicio] = useState(`${ano}-01-01`);
  const [fim, setFim] = useState(`${ano}-12-31`);
  const [relatorio, setRelatorio] = useState<Relatorio | null>(null);
  const [historicos, setHistoricos] = useState<HistoricoRegistro[]>([]);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState("");
  const sequencia = useRef(0);
  const controle = useRef<AbortController | null>(null);

  async function carregar(de: string, ate: string) {
    if (!de || !ate || de > ate) { setErro("Informe um período válido."); return; }
    controle.current?.abort();
    const atual = new AbortController();
    controle.current = atual;
    const pedido = ++sequencia.current;
    setCarregando(true);
    setErro("");
    try {
      const [dados, registros] = await Promise.all([api.consultarRelatorios(de, ate, atual.signal), api.consultarHistoricos(de, ate, atual.signal)]);
      if (pedido !== sequencia.current) return;
      setRelatorio(dados);
      setHistoricos(registros);
    } catch (e) {
      if (pedido === sequencia.current) setErro(e instanceof Error ? e.message : "Não foi possível carregar os relatórios.");
    } finally {
      if (pedido === sequencia.current) setCarregando(false);
    }
  }

  useEffect(() => {
    void carregar(`${ano}-01-01`, `${ano}-12-31`);
    return () => { ++sequencia.current; controle.current?.abort(); };
  }, [ano]);

  function consultar(e: SyntheticEvent) { e.preventDefault(); void carregar(inicio, fim); }
  const totais = relatorio?.resumo.reduce((s, r) => ({ eventos: s.eventos + r.total, realizados: s.realizados + r.realizados,
    agendados: s.agendados + r.agendados, cancelados: s.cancelados + r.cancelados }),
  { eventos: 0, realizados: 0, agendados: 0, cancelados: 0 });

  return <Stack gap="5">
    <Box><Heading size="lg">Relatórios</Heading><Text color="fg.muted">Eventos, presença dos músicos, utilização dos locais e histórico de alterações.</Text></Box>
    <HStack as="form" onSubmit={consultar} align="end" wrap="wrap">
      <Box><label htmlFor="relatorio-inicio">De</label><Input id="relatorio-inicio" type="date" min="1900-01-01" max="9998-12-31" required value={inicio} onChange={e => setInicio(e.target.value)} /></Box>
      <Box><label htmlFor="relatorio-fim">Até</label><Input id="relatorio-fim" type="date" min={inicio || "1900-01-01"} max="9998-12-31" required value={fim} onChange={e => setFim(e.target.value)} /></Box>
      <Button type="submit" loading={carregando}>Consultar</Button>
    </HStack>
    {erro && <Text color="red.500" role="alert">{erro}</Text>}
    {relatorio && totais && <>
      <Text color="fg.muted">Período consultado: {relatorio.inicio.split("-").reverse().join("/")} a {relatorio.fim.split("-").reverse().join("/")}. Os eventos são selecionados pela data de início.</Text>
      <Grid templateColumns={{ base: "1fr 1fr", md: "repeat(4,1fr)" }} gap="3">
        {[["Eventos", totais.eventos], ["Agendados", totais.agendados], ["Realizados", totais.realizados], ["Cancelados", totais.cancelados]].map(([titulo, valor]) =>
          <Card.Root key={titulo}><Card.Body><Text color="fg.muted">{titulo}</Text><Text fontSize="2xl" fontWeight="bold">{valor}</Text></Card.Body></Card.Root>)}
      </Grid>
      <Card.Root><Card.Body><Heading size="md" mb="3">Eventos por tipo</Heading>
        <Tabela colunas={["Tipo", "Total", "Agendados", "Realizados", "Cancelados", "Horas programadas"]}>
          {relatorio.resumo.map(r => <tr key={r.codigoTipo}><td>{r.tipo}</td><td>{r.total}</td><td>{r.agendados}</td><td>{r.realizados}</td><td>{r.cancelados}</td><td>{(r.minutosProgramados / 60).toFixed(1)}</td></tr>)}
        </Tabela>
      </Card.Body></Card.Root>
      <Card.Root><Card.Body><Heading size="md" mb="3">Presença dos músicos</Heading>
        <Text color="fg.muted" mb="3">A taxa considera apenas eventos realizados com presença ou ausência lançada. Sem lançamentos, a taxa aparece como “—”.</Text>
        <Tabela colunas={["Músico", "Instrumento", "Eventos vinculados", "Realizados", "Presenças", "Ausências", "Sem registro", "Taxa"]}>
          {relatorio.presencas.map(r => <tr key={r.codigoMusico}><td>{r.nome}</td><td>{r.instrumento}</td><td>{r.eventos}</td><td>{r.realizados}</td><td>{r.presencas}</td><td>{r.ausencias}</td><td>{r.semRegistro}</td><td>{r.taxaPresenca == null ? "—" : `${r.taxaPresenca.toFixed(2)}%`}</td></tr>)}
        </Tabela>
      </Card.Body></Card.Root>
      <Card.Root><Card.Body><Heading size="md" mb="3">Utilização dos locais</Heading>
        <Text color="fg.muted" mb="3">Horas programadas incluem a duração integral de eventos agendados e realizados; cancelados não entram no cálculo.</Text>
        <Tabela colunas={["Local", "Cidade", "Eventos", "Agendados", "Realizados", "Cancelados", "Horas programadas"]}>
          {relatorio.locais.map(r => <tr key={r.codigoLocal}><td>{r.nome}</td><td>{r.cidade}/{r.uf}</td><td>{r.eventos}</td><td>{r.agendados}</td><td>{r.realizados}</td><td>{r.cancelados}</td><td>{(r.minutosProgramados / 60).toFixed(1)}</td></tr>)}
        </Tabela>
      </Card.Body></Card.Root>
      <Card.Root><Card.Body><Heading size="md" mb="3">Histórico de alterações</Heading>
        <Text color="fg.muted" mb="3">Últimos 100 registros pela data da operação no período selecionado. Abra um registro para comparar os dados.</Text>
        {!historicos.length && <Text>Nenhuma alteração registrada neste período.</Text>}
        <Stack gap="3">{historicos.map(h => <Box key={`${h.entidade}:${h.codigo}`} borderWidth="1px" borderRadius="md" p="3">
          <details><summary>{formatarDataHora(h.dataHora)} · {h.entidade} #{h.registro} · {h.operacao}</summary>
            <Text my="2">Responsável: {h.responsavel ?? "Sem usuário autenticado"}{h.codigoResponsavel ? ` (#${h.codigoResponsavel})` : ""}</Text>
            <Grid templateColumns={{ base: "1fr", md: "1fr 1fr" }} gap="3">
              <Box><Text fontWeight="bold">Antes</Text><pre className="historico-json">{json(h.dadosAnteriores)}</pre></Box>
              <Box><Text fontWeight="bold">Depois</Text><pre className="historico-json">{json(h.dadosNovos)}</pre></Box>
            </Grid>
          </details>
        </Box>)}</Stack>
      </Card.Body></Card.Root>
    </>}
  </Stack>;
}
