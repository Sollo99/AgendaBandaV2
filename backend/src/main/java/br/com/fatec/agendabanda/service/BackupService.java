package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.config.BackupConfig;
import br.com.fatec.agendabanda.dto.BackupDtos.*;
import br.com.fatec.agendabanda.dto.PaginaDto;
import br.com.fatec.agendabanda.exception.*;
import br.com.fatec.agendabanda.repository.BackupSqlRepository;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.sql.Connection;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class BackupService {
    private final BackupConfig config;
    private final BackupCatalogo catalogo;
    private final BackupSqlRepository sql;
    private final BackupManutencao manutencao;
    private final DataSource datasource;
    private final Clock clock;
    private final ExecutorService executor=Executors.newSingleThreadExecutor(r -> { var t=new Thread(r,"agendabanda-backup");t.setDaemon(true);return t; });
    private final Map<UUID,PermissaoConsulta> consultas=new ConcurrentHashMap<>();
    private final AtomicBoolean ocupada=new AtomicBoolean();
    private volatile UUID atual;
    private record PermissaoConsulta(byte[] segredo,Instant validade) {}

    public BackupService(BackupConfig config,BackupCatalogo catalogo,BackupSqlRepository sql,
            BackupManutencao manutencao,DataSource datasource,Clock clock) {
        this.config=config;this.catalogo=catalogo;this.sql=sql;this.manutencao=manutencao;this.datasource=datasource;this.clock=clock;
        if (catalogo.restoreInterrompido()) manutencao.bloquear();
    }
    public Painel painel() {
        return new Painel(config.habilitado,config.configurado(),config.diretorioDownload!=null,config.banco,
                !config.habilitado?"Backups estão desabilitados no servidor."
                :!config.configurado()?"Não foi possível preparar os backups automaticamente. Confira a execução dos sete scripts na mesma instância SQL Server."
                :manutencao.ativa()?"O banco está em manutenção. Aguarde a operação ou consulte o responsável pelo servidor."
                :"Os horários automáticos seguem São Paulo e funcionam enquanto o backend estiver ligado.",
                catalogo.preferencias(),catalogo.proximaExecucao(),atual==null?null:catalogo.obter(atual),manutencao.ativa());
    }
    public PaginaDto<Registro> listar(int page,int size) {
        PaginaDto.validar(page,size);
        var todos=catalogo.registros().stream().sorted(Comparator.comparing(Registro::inicio).reversed().thenComparing(Registro::codigo)).toList();
        return PaginaDto.de(todos.stream().skip((long)page*size).limit(size).toList(),page,size,todos.size());
    }
    public Registro obter(UUID id) { return catalogo.obter(id); }
    public Progresso progresso(UUID id) { return new Progresso(catalogo.obter(id),manutencao.ativa()); }
    public synchronized Painel configurar(Preferencias p) {
        if (!config.habilitado) throw new RegraNegocioException("Backups estão desabilitados no servidor.");
        if (ocupada.get()) throw new ConflitoException("Aguarde a operação em andamento antes de alterar os horários.");
        catalogo.configurar(p,p.agendado()?PlanejamentoBackup.proxima(agora(),p.horario(),p.diasSemana()):null);
        return painel();
    }
    public OperacaoAceita backup(Ator ator) { return iniciar(Tipo.BACKUP,Origem.MANUAL,null,ator,true); }
    public OperacaoAceita restaurar(UUID id,Ator ator,String confirmacao) {
        if (!config.banco.equals(confirmacao)) throw new RegraNegocioException("Digite exatamente o nome do banco para confirmar a restauração.");
        Registro origem=backupValido(id);
        return iniciar(Tipo.RESTORE,Origem.MANUAL,origem.codigo(),ator,true);
    }
    public synchronized boolean automatico(Origem origem) {
        if (!config.configurado() || ocupada.get() || manutencao.ativa()) return false;
        iniciar(Tipo.BACKUP,origem,null,null,false);
        return true;
    }
    public synchronized void agendarSeNecessario() {
        if (!config.configurado() || !catalogo.preferencias().agendado() || manutencao.ativa()) return;
        var p=catalogo.preferencias();
        var proxima=catalogo.proximaExecucao();
        if (proxima==null) { catalogo.configurar(p,PlanejamentoBackup.proxima(agora(),p.horario(),p.diasSemana()));return; }
        if (!proxima.isAfter(agora()) && !ocupada.get()) {
            // Avanca antes de executar: uma falha nao dispara tentativas continuas no mesmo horario.
            catalogo.configurar(p,PlanejamentoBackup.proxima(agora(),p.horario(),p.diasSemana()));
            automatico(Origem.AGENDADO);
        }
    }
    private synchronized OperacaoAceita iniciar(Tipo tipo,Origem origem,UUID backup,Ator ator,boolean concederConsulta) {
        if (!config.configurado()) throw new RegraNegocioException("Backups ainda não estão configurados no servidor.");
        if (manutencao.ativa() || !ocupada.compareAndSet(false,true)) throw new ConflitoException("Já existe uma operação de backup ou restauração em andamento.");
        UUID id=UUID.randomUUID();
        var registro=new Registro(id,tipo,origem,backup,ator==null?null:ator.codigo(),ator==null?null:ator.nome(),
                agora(),null,Estado.EM_FILA,tipo==Tipo.BACKUP?"agendabanda_"+id+".bak":null,null,null,"Aguardando execução.");
        try {
            catalogo.adicionar(registro); atual=id;
            String segredo=null;
            if (concederConsulta) {
                byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes);
                segredo=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                consultas.entrySet().removeIf(e -> !e.getValue().validade().isAfter(clock.instant()));
                consultas.put(id,new PermissaoConsulta(hash(segredo),clock.instant().plusSeconds(6L*config.timeoutSegundos+600)));
            }
            executor.execute(() -> executar(registro));
            return new OperacaoAceita(registro,segredo);
        } catch (RuntimeException ex) {
            consultas.remove(id);atual=null;ocupada.set(false);
            throw ex;
        }
    }
    /** Credencial limitada a GET de uma unica operacao, sem acesso a qualquer outra rota. */
    public boolean podeAcompanhar(UUID id,String segredo) {
        if (segredo==null || !segredo.matches("[A-Za-z0-9_-]{43}")) return false;
        var permissao=consultas.get(id);
        return permissao!=null && permissao.validade().isAfter(clock.instant()) && MessageDigest.isEqual(permissao.segredo(),hash(segredo));
    }
    private byte[] hash(String valor) {
        try { return MessageDigest.getInstance("SHA-256").digest(valor.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
        catch(NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private void executar(Registro r) {
        try {
            fase(r.codigo(),"Preparando conexão e validando o banco.");
            if (r.tipo()==Tipo.RESTORE) executarRestauracao(r);
            else {
                try(var leitura=manutencao.tentarLeitura()) {
                    if (leitura==null) throw new ConflitoException("O banco está em manutenção.");
                    try(Connection c=sql.abrir()) { gravarBackup(c,r); }
                }
            }
        } catch(Exception ex) {
            LoggerFactory.getLogger(BackupService.class).error("Falha na operação de backup {}.",r.codigo(),ex);
            try {
                var atual=catalogo.obter(r.codigo());
                String mensagem=ex instanceof RegraNegocioException || ex instanceof ConflitoException?ex.getMessage()
                        :"A operação falhou. Confira a conexão, o espaço em disco e as permissões do SQL Server.";
                if (r.tipo()==Tipo.RESTORE && manutencao.ativa()) mensagem="A restauração falhou e a aplicação permanece em manutenção. O responsável pelo SQL Server deve verificar o banco antes de reiniciar o backend.";
                catalogo.atualizar(atual.atualizar(Estado.FALHOU,mensagem,agora(),atual.tamanhoBytes(),atual.assinaturaSchema()));
            } catch(RuntimeException erro) { LoggerFactory.getLogger(BackupService.class).error("Falha ao registrar o resultado da operação.",erro); }
        } finally { atual=null;ocupada.set(false); }
    }
    private void gravarBackup(Connection c,Registro r) throws Exception {
        fase(r.codigo(),"Gerando e verificando o backup.");
        var arquivo=sql.criar(c,r);
        catalogo.atualizar(catalogo.obter(r.codigo()).atualizar(Estado.CONCLUIDO,"Backup concluído e verificado.",agora(),arquivo.tamanhoBytes(),arquivo.assinaturaSchema()));
    }
    private void executarRestauracao(Registro r) throws Exception {
        var bancoAlterado=new AtomicBoolean();
        boolean seguroReabrir=false;
        HikariDataSource pool=null;
        int minimoAnterior=0;
        boolean suspenso=false;
        try(var exclusiva=manutencao.iniciarRestauracao()) {
            try(Connection c=sql.abrir()) {
                sql.exigirPermissaoRestauracao(c);
                Registro origem=backupValido(r.backupOrigem());
                fase(r.codigo(),"Verificando o arquivo e a compatibilidade da estrutura.");
                sql.verificar(c,origem);
                if (!Objects.equals(origem.assinaturaSchema(),sql.assinatura(c)))
                    throw new RegraNegocioException("O backup usa uma estrutura diferente da aplicação atual e exige restauração assistida.");
                fase(r.codigo(),"Criando cópia de segurança do estado atual.");
                UUID copia=UUID.randomUUID();
                var protecao=new Registro(copia,Tipo.BACKUP,Origem.PRE_RESTAURACAO,null,r.responsavel(),r.nomeResponsavel(),agora(),null,
                        Estado.EXECUTANDO,"agendabanda_"+copia+".bak",null,null,"Cópia anterior à restauração.");
                catalogo.adicionar(protecao);
                try { gravarBackup(c,protecao); }
                catch(Exception ex) {
                    catalogo.atualizar(catalogo.obter(copia).atualizar(Estado.FALHOU,"A cópia anterior à restauração falhou.",agora(),null,null));
                    throw ex;
                }
                pool=datasource.unwrap(HikariDataSource.class);
                minimoAnterior=pool.getMinimumIdle();
                pool.setMinimumIdle(0);
                pool.getHikariPoolMXBean().suspendPool();suspenso=true;
                pool.getHikariPoolMXBean().softEvictConnections();
                long limite=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
                while(pool.getHikariPoolMXBean().getTotalConnections()>0 && System.nanoTime()<limite) Thread.sleep(50);
                if (pool.getHikariPoolMXBean().getTotalConnections()>0)
                    throw new RegraNegocioException("Existem conexões em uso. A restauração não foi iniciada.");
                fase(r.codigo(),"Restaurando o banco. Aguarde e mantenha esta tela aberta.");
                // A partir daqui, qualquer erro exige comprovar saude e invalidar credenciais antes de reabrir.
                sql.restaurar(c,origem,() -> { catalogo.recuperacao(true);bancoAlterado.set(true); });
                if (!sql.online(c) || !Objects.equals(origem.assinaturaSchema(),sql.assinatura(c)))
                    throw new IllegalStateException("O banco restaurado não passou pela verificação final.");
                sql.invalidarCredenciais(c);
                catalogo.atualizar(catalogo.obter(r.codigo()).atualizar(Estado.CONCLUIDO,
                        "Restauração concluída. Entre novamente com uma conta existente no backup.",agora(),null,origem.assinaturaSchema()));
                catalogo.recuperacao(false);
                seguroReabrir=true;
            }
        } finally {
            if (!bancoAlterado.get()) seguroReabrir=true;
            if (pool!=null) {
                if (seguroReabrir) pool.setMinimumIdle(minimoAnterior);
                if (suspenso) pool.getHikariPoolMXBean().resumePool();
            }
            if (seguroReabrir) manutencao.liberar();
        }
    }
    public void recuperarInterrupcao() {
        if (!catalogo.restoreInterrompido() || !config.configurado()) return;
        try(Connection c=sql.abrir()) {
            if (!sql.online(c)) throw new IllegalStateException("Banco indisponível após interrupção.");
            var restauracao=catalogo.registros().stream().filter(r -> r.tipo()==Tipo.RESTORE)
                    .max(Comparator.comparing(Registro::inicio)).orElseThrow();
            if (!Objects.equals(backupValido(restauracao.backupOrigem()).assinaturaSchema(),sql.assinatura(c)))
                throw new IllegalStateException("A estrutura do banco precisa de verificação após interrupção.");
            sql.invalidarCredenciais(c);
            catalogo.recuperacao(false);
            manutencao.liberar();
        } catch(Exception ex) { LoggerFactory.getLogger(BackupService.class).error("Restauração interrompida exige verificação manual no SQL Server.",ex); }
    }
    private Registro backupValido(UUID id) {
        Registro r=catalogo.obter(id);
        if (r.tipo()!=Tipo.BACKUP || r.estado()!=Estado.CONCLUIDO || r.assinaturaSchema()==null)
            throw new RegraNegocioException("Selecione um backup concluído e verificado.");
        config.caminho(r.arquivo());
        return r;
    }
    public Path arquivoDownload(UUID id) throws IOException {
        Registro r=backupValido(id);
        if (config.diretorioDownload==null) throw new RegraNegocioException("O download não está configurado neste servidor.");
        Path raiz=config.diretorioDownload.toRealPath();
        Path arquivo=raiz.resolve(r.arquivo()).normalize();
        if (!arquivo.getParent().equals(raiz) || !Files.isRegularFile(arquivo,LinkOption.NOFOLLOW_LINKS))
            throw new RecursoNaoEncontradoException("Arquivo de backup não disponível para download.");
        if (!Files.isReadable(arquivo))
            throw new RegraNegocioException("O backend não tem acesso de leitura a este arquivo. A cópia continua disponível para restauração pelo SQL Server.");
        return arquivo;
    }
    private void fase(UUID id,String mensagem) {
        var r=catalogo.obter(id);
        catalogo.atualizar(r.atualizar(Estado.EXECUTANDO,mensagem,null,r.tamanhoBytes(),r.assinaturaSchema()));
    }
    private LocalDateTime agora() { return LocalDateTime.now(clock); }
    @PreDestroy public void fechar() {
        executor.shutdown();
        try { if (!executor.awaitTermination(30,TimeUnit.SECONDS)) executor.shutdownNow(); }
        catch(InterruptedException ex) { Thread.currentThread().interrupt();executor.shutdownNow(); }
    }
}
