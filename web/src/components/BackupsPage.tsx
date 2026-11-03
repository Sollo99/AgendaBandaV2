import { useEffect, useRef, useState } from "react";
import type { SyntheticEvent } from "react";
import { Box, Button, Card, Grid, Heading, HStack, Input, Stack, Text } from "@chakra-ui/react";
import { api, ApiError } from "../api";
import { encerrarSessao } from "../sessao";
import { formatarDataHora } from "../datas";
import { useConsulta } from "../hooks/useConsulta";
import type { BackupOperacao, BackupPreferencias, BackupRegistro } from "../types";
import Pagination from "./Pagination";

const dias = [[1,"Segunda"],[2,"Terça"],[3,"Quarta"],[4,"Quinta"],[5,"Sexta"],[6,"Sábado"],[7,"Domingo"]] as const;
const origens = { MANUAL: "Manual", AGENDADO: "Agendado", INICIALIZACAO: "Ao iniciar o backend", PRE_RESTAURACAO: "Proteção antes de restaurar" };
const estados = { EM_FILA: "Na fila", EXECUTANDO: "Em andamento", CONCLUIDO: "Concluído", FALHOU: "Falhou", INTERROMPIDO: "Interrompido" };
const terminou = (r: BackupRegistro) => r.estado !== "EM_FILA" && r.estado !== "EXECUTANDO";

export default function BackupsPage() {
  const [page,setPage] = useState(0);
  const consulta = useConsulta(signal => Promise.all([api.painelBackups(signal),api.listarBackups(page,signal)]),[page]);
  const painel = consulta.dados?.[0], pagina = consulta.dados?.[1];
  const [form,setForm] = useState<BackupPreferencias>({ aoIniciar: true, agendado: false, horario: "02:00", diasSemana: [1,2,3,4,5,6,7] });
  const [ocupado,setOcupado] = useState(false);
  const [operacao,setOperacao] = useState<BackupOperacao | null>(null);
  const [progresso,setProgresso] = useState<BackupRegistro | null>(null);
  const [restaurar,setRestaurar] = useState<BackupRegistro | null>(null);
  const [senha,setSenha] = useState("");
  const [confirmacao,setConfirmacao] = useState("");
  const [erro,setErro] = useState("");
  const [mensagem,setMensagem] = useState("");
  const [manutencao,setManutencao] = useState(false);
  const montado = useRef(false);
  useEffect(() => { montado.current=true;return () => { montado.current=false; }; },[]);
  useEffect(() => { if (painel) { setForm(painel.preferencias);setManutencao(painel.manutencao); } },[painel]);
  const emExecucao = ocupado || operacao !== null || !!painel?.emAndamento;
  const bloqueado = emExecucao || manutencao;
  const recarregar = consulta.recarregar;

  useEffect(() => {
    if (!operacao) return;
    let encerrado=false;
    let controle: AbortController | null=null;
    const acompanhar = async () => {
      controle?.abort();controle=new AbortController();
      try {
        const resultado=await api.acompanharBackup(operacao.registro.codigo,operacao.acompanhamento,controle.signal);
        if (encerrado) return;
        setProgresso(resultado.registro);setManutencao(resultado.manutencao);
        if (terminou(resultado.registro)) {
          setOperacao(null);
          if (resultado.registro.estado==="CONCLUIDO") {
            if (resultado.registro.tipo==="RESTORE") {
              encerrarSessao("Banco restaurado. Entre novamente com uma conta existente no backup.");
              return;
            }
            setMensagem(resultado.registro.mensagem);
          } else setErro(resultado.registro.mensagem);
          if (!resultado.manutencao) void recarregar();
        }
      } catch(e) {
        if (!encerrado && !(e instanceof DOMException && e.name==="AbortError")) {
          setErro(e instanceof Error?e.message:"Não foi possível acompanhar a operação. A execução pode continuar no servidor.");
          if (e instanceof ApiError && e.status===401) setOperacao(null);
        }
      }
    };
    void acompanhar();
    const timer=window.setInterval(() => void acompanhar(),3000);
    return () => { encerrado=true;controle?.abort();window.clearInterval(timer); };
  },[operacao,recarregar]);

  useEffect(() => {
    if (operacao || !painel?.emAndamento || manutencao) return;
    const timer=window.setInterval(() => void recarregar(),5000);
    return () => window.clearInterval(timer);
  },[operacao,painel?.emAndamento,manutencao,recarregar]);

  async function salvar(e: SyntheticEvent) {
    e.preventDefault();
    if (bloqueado) return;
    if (!form.diasSemana.length) { setErro("Selecione pelo menos um dia da semana.");return; }
    setOcupado(true);setErro("");setMensagem("");
    try {
      await api.configurarBackups(form);
      if (montado.current) { setMensagem("Preferências de backup salvas.");await recarregar(); }
    } catch(e) { if (montado.current) setErro(e instanceof Error?e.message:"Não foi possível salvar."); }
    finally { if (montado.current) setOcupado(false); }
  }
  function acompanhar(r: BackupOperacao) { setOperacao(r);setProgresso(r.registro); }
  async function gerar() {
    if (bloqueado) return;
    setOcupado(true);setErro("");setMensagem("");
    try { const r=await api.gerarBackup();if (montado.current) acompanhar(r); }
    catch(e) { if (montado.current) setErro(e instanceof Error?e.message:"Não foi possível iniciar o backup."); }
    finally { if (montado.current) setOcupado(false); }
  }
  async function confirmarRestauracao(e: SyntheticEvent) {
    e.preventDefault();
    if (!restaurar || bloqueado) return;
    setOcupado(true);setErro("");setMensagem("");
    try {
      const r=await api.restaurarBackup(restaurar.codigo,senha,confirmacao);
      if (montado.current) { acompanhar(r);setRestaurar(null);setConfirmacao(""); }
    } catch(e) { if (montado.current) setErro(e instanceof Error?e.message:"Não foi possível iniciar a restauração."); }
    finally { if (montado.current) { setSenha("");setOcupado(false); } }
  }
  function selecionar(r: BackupRegistro) { setRestaurar(r);setSenha("");setConfirmacao("");setErro(""); }
  const registroAtual = operacao ? progresso : painel?.emAndamento;
  return <Stack gap="5">
    <Box><Heading size="lg">Backups e restauração</Heading><Text color="fg.muted">Proteja os dados da banda e acompanhe as cópias disponíveis.</Text></Box>
    {(erro || consulta.erro) && <Text color="red.500" role="alert">{erro || consulta.erro}</Text>}
    {mensagem && <Text color="green.600" role="status">{mensagem}</Text>}
    {consulta.carregando && !painel && <Text>Carregando backups...</Text>}
    {painel && <>
      <Text color="fg.muted">{painel.aviso}</Text>
      {manutencao && <Text role="alert">O banco está em manutenção. A restauração substitui os dados atuais e encerra as sessões.</Text>}
      <HStack wrap="wrap">
        <Button onClick={() => void gerar()} disabled={!painel.configurado || bloqueado}>Gerar backup agora</Button>
        <Button variant="outline" onClick={() => void recarregar()} disabled={ocupado || !!operacao || manutencao}>Atualizar histórico</Button>
      </HStack>
      {registroAtual && <Card.Root><Card.Body><Stack gap="2" role="status" aria-live="polite">
        <Text fontWeight="bold">{registroAtual.tipo==="BACKUP"?"Backup":"Restauração"}: {estados[registroAtual.estado]}</Text>
        <Text>{registroAtual.mensagem}</Text>
        <Text color="fg.muted">A operação continua no servidor. Aguarde a conclusão antes de sair desta tela.</Text>
      </Stack></Card.Body></Card.Root>}
      <Card.Root><Card.Body><Stack as="form" gap="4" onSubmit={salvar}>
        <Heading size="md">Backups automáticos</Heading>
        <label><input type="checkbox" checked={form.aoIniciar} disabled={!painel.habilitado || bloqueado}
          onChange={e => setForm({ ...form,aoIniciar:e.target.checked })} /> Fazer backup ao iniciar o backend</label>
        <Text color="fg.muted">Essa opção vem ligada por padrão. Após preparar o banco pelos scripts de instalação, uma cópia é gerada a cada inicialização.</Text>
        <label><input type="checkbox" checked={form.agendado} disabled={!painel.habilitado || bloqueado}
          onChange={e => setForm({ ...form,agendado:e.target.checked })} /> Fazer backups nos dias e horário escolhidos</label>
        <Box><label htmlFor="backup-horario">Horário de São Paulo</label>
          <Input id="backup-horario" type="time" required value={form.horario} maxW="180px"
            disabled={!form.agendado || !painel.habilitado || bloqueado} onChange={e => setForm({ ...form,horario:e.target.value })} /></Box>
        <HStack wrap="wrap">{dias.map(([dia,nome]) => <label key={dia}><input type="checkbox"
          checked={form.diasSemana.includes(dia)} disabled={!form.agendado || !painel.habilitado || bloqueado}
          onChange={e => setForm({ ...form,diasSemana:e.target.checked?[...form.diasSemana,dia]:form.diasSemana.filter(d => d!==dia) })} /> {nome}</label>)}</HStack>
        <Text color="fg.muted">O agendamento funciona com o backend ligado. Se um horário for perdido, uma cópia é feita na próxima verificação disponível.</Text>
        {painel.proximaExecucao && <Text>Próximo horário: {formatarDataHora(painel.proximaExecucao)}</Text>}
        <HStack><Button type="submit" loading={ocupado} disabled={!painel.habilitado || bloqueado}>Salvar preferências</Button></HStack>
      </Stack></Card.Body></Card.Root>
      {restaurar && <Card.Root borderColor="orange.400"><Card.Body><Stack as="form" onSubmit={confirmarRestauracao} gap="4">
        <Heading size="md">Confirmar restauração</Heading>
        <Text>Backup de {formatarDataHora(restaurar.inicio)}: {restaurar.arquivo}</Text>
        <Text>Os dados posteriores a essa cópia serão substituídos. O sistema fará um backup do estado atual antes de restaurar. Todos precisarão entrar novamente; contas e senhas voltarão ao estado da cópia selecionada.</Text>
        <Text>Feche conexões externas com esse banco antes de continuar.</Text>
        <Box><label htmlFor="backup-confirmacao">Digite {painel.banco} para confirmar</label><Input id="backup-confirmacao" required maxLength={128} value={confirmacao} onChange={e => setConfirmacao(e.target.value)} autoComplete="off" /></Box>
        <Box><label htmlFor="backup-senha">Sua senha atual de administrador</label><Input id="backup-senha" type="password" required maxLength={72} value={senha} onChange={e => setSenha(e.target.value)} autoComplete="current-password" /></Box>
        <HStack><Button type="submit" colorPalette="orange" disabled={bloqueado || confirmacao!==painel.banco || !senha}>Restaurar este backup</Button>
          <Button type="button" variant="outline" disabled={ocupado} onClick={() => { setRestaurar(null);setSenha(""); }}>Cancelar</Button></HStack>
      </Stack></Card.Body></Card.Root>}
      <Heading size="md">Histórico de operações</Heading>
      {!pagina?.content.length && <Text>Nenhuma operação registrada.</Text>}
      <Grid templateColumns={{ base:"1fr",md:"repeat(2,1fr)" }} gap="3">
        {pagina?.content.map(r => <Card.Root key={r.codigo}><Card.Body><Stack gap="2">
          <Text fontWeight="bold">{r.tipo==="BACKUP"?"Backup":"Restauração"} · {estados[r.estado]}</Text>
          <Text>{formatarDataHora(r.inicio)} · {origens[r.origem]}</Text>
          <Text color="fg.muted">Responsável: {r.nomeResponsavel ?? "Sistema automático"}</Text>
          <Text>{r.mensagem}</Text>
          {r.arquivo && <Text fontSize="sm" overflowWrap="anywhere">{r.arquivo}</Text>}
          {r.tamanhoBytes!=null && <Text fontSize="sm">Dados do backup: {(r.tamanhoBytes/1024/1024).toFixed(2)} MB</Text>}
          {r.tipo==="BACKUP" && r.estado==="CONCLUIDO" && <HStack wrap="wrap">
            <Button size="sm" variant="outline" disabled={!painel.configurado || bloqueado} onClick={() => selecionar(r)}>Restaurar</Button>
            {painel.downloadDisponivel && <Button asChild size="sm" variant="outline" disabled={manutencao}><a href={api.enderecoArquivoBackup(r.codigo)} target="_blank" rel="noreferrer">Baixar arquivo</a></Button>}
          </HStack>}
        </Stack></Card.Body></Card.Root>)}
      </Grid>
      {pagina && <Pagination page={page} totalPages={pagina.totalPages} totalElements={pagina.totalElements}
        carregando={consulta.carregando || !!operacao || manutencao} onChange={setPage} />}
    </>}
  </Stack>;
}
