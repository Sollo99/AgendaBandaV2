import { lazy, Suspense, useState } from "react";
import { Box, Button, Flex, Heading, Stack, Text } from "@chakra-ui/react";
import { api } from "../api";
import { dataSaoPaulo, diasDoCalendario } from "../datas";
import { useConsulta } from "../hooks/useConsulta";
import type { Agenda, Membro } from "../types";
import AgendaCalendar from "./agenda/AgendaCalendar";
import EventDetailsModal from "./agenda/EventDetailsModal";
import Pagination from "./Pagination";
const EventFormModal = lazy(() => import("./agenda/EventFormModal"));

export default function AgendaPage({ usuario }: { usuario: Membro }) {
  const hoje = dataSaoPaulo();
  const [filtro, setFiltro] = useState({ mes: hoje.slice(0, 7), page: 0 });
  const [selecionado, setSelecionado] = useState<Agenda | null>(null);
  const [formulario, setFormulario] = useState<{ evento: Agenda | null } | null>(null);
  const [erro, setErro] = useState("");
  const dias = diasDoCalendario(filtro.mes);
  const consulta = useConsulta(signal => api.listarAgenda({
    page: filtro.page, size: 50, inicio: dias[0], fim: dias[41]
  }, signal), [filtro.mes, filtro.page]);
  const pagina = consulta.dados;

  function abrirFormulario(evento: Agenda | null) { setSelecionado(null); setFormulario({ evento }); }

  async function excluir(evento: Agenda) {
    if (!confirm(`Excluir o evento "${evento.titulo}"?`)) return;
    setErro("");
    try {
      await api.excluirAgenda(evento.codigoAgenda);
      setSelecionado(atual => atual?.codigoAgenda === evento.codigoAgenda ? null : atual);
      if (filtro.page > 0 && pagina?.content.length === 1) setFiltro(atual => ({ ...atual, page: atual.page - 1 }));
      else await consulta.recarregar();
    } catch (e) { setErro(e instanceof Error ? e.message : "Não foi possível excluir o evento."); }
  }

  return <Stack gap="5">
    <Flex justify="space-between" align="center" gap="4" wrap="wrap">
      <Box><Heading size="lg">Agenda</Heading><Text color="fg.muted">Ensaios, shows e reuniões. Horários de São Paulo.</Text></Box>
      <Button onClick={() => abrirFormulario(null)}>Novo evento</Button>
    </Flex>
    {(erro || consulta.erro) && <Text color="red.500" role="alert">{erro || consulta.erro}</Text>}
    {consulta.carregando && <Text>Carregando eventos...</Text>}
    <AgendaCalendar eventos={pagina?.content ?? []} mesAtual={filtro.mes} hoje={hoje}
      onMes={mes => { setSelecionado(null); setFiltro({ mes, page: 0 }); }} onSelecionar={setSelecionado} />
    {pagina && <>
      {pagina.totalPages > 1 && <Text color="fg.muted">O calendário exibe os eventos desta página. Use a paginação para ver os demais eventos do período.</Text>}
      <Pagination page={filtro.page} totalPages={pagina.totalPages} totalElements={pagina.totalElements}
        carregando={consulta.carregando} onChange={page => setFiltro(atual => ({ ...atual, page }))} />
    </>}
    {selecionado && <EventDetailsModal key={selecionado.codigoAgenda} evento={selecionado} usuario={usuario}
      onClose={() => setSelecionado(null)} onEdit={() => abrirFormulario(selecionado)} onDelete={() => void excluir(selecionado)}
      onAtualizar={evento => {
        setSelecionado(atual => atual?.codigoAgenda === evento.codigoAgenda ? evento : atual);
        void consulta.recarregar();
      }} />}
    {formulario && <Suspense fallback={<Text>Carregando formulário...</Text>}>
      <EventFormModal evento={formulario.evento} onClose={() => setFormulario(null)}
        onSaved={() => { setFormulario(atual => atual === formulario ? null : atual); void consulta.recarregar(); }} />
    </Suspense>}
  </Stack>;
}
