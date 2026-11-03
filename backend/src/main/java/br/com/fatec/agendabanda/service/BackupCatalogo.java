package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.config.BackupConfig;
import br.com.fatec.agendabanda.dto.BackupDtos.*;
import br.com.fatec.agendabanda.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static java.nio.file.StandardCopyOption.*;
import static java.nio.file.StandardOpenOption.*;

/** Catalogo operacional externo: restaurar a base nao reverte historico ou preferencias. */
@Component
public class BackupCatalogo {
    public record Dados(int versao,Preferencias preferencias,LocalDateTime proximaExecucao,List<Registro> registros,boolean recuperacaoNecessaria) {}
    private final ObjectMapper json;
    private final BackupConfig config;
    private Dados dados=new Dados(1,Preferencias.padrao(),null,List.of(),false);
    private FileChannel canal;
    private FileLock trava;

    public BackupCatalogo(ObjectMapper json,BackupConfig config) {
        this.json=json; this.config=config;
        if (!config.habilitado && !Files.exists(config.catalogo.resolve("catalogo.json"))) return;
        try {
            Files.createDirectories(config.catalogo);
            canal=FileChannel.open(config.catalogo.resolve("catalogo.lock"),CREATE,WRITE);
            trava=canal.tryLock();
            if (trava==null) throw new IOException("Catálogo já usado por outra instância.");
            Path arquivo=config.catalogo.resolve("catalogo.json");
            if (Files.exists(arquivo)) {
                dados=json.readValue(arquivo.toFile(),Dados.class);
                validar(dados);
            }
            var registros=new ArrayList<Registro>();
            for (Registro r:dados.registros()) {
                if (!r.finalizado()) {
                    r=r.atualizar(Estado.INTERROMPIDO,"Operação interrompida pelo encerramento do backend.",null,r.tamanhoBytes(),r.assinaturaSchema());
                }
                registros.add(r);
            }
            dados=new Dados(1,dados.preferencias(),dados.proximaExecucao(),List.copyOf(registros),dados.recuperacaoNecessaria());
            if (!config.habilitado && dados.recuperacaoNecessaria())
                throw new IllegalStateException("Reative os backups para verificar a restauração interrompida antes de liberar a aplicação.");
            salvar(dados);
        } catch (IOException|RuntimeException ex) {
            fechar();
            throw new IllegalStateException("Não foi possível abrir o catálogo de backups. Confira o diretório e use uma única instância.",ex);
        }
    }
    private void validar(Dados d) throws IOException {
        if (d==null || d.versao()!=1 || d.preferencias()==null || d.registros()==null) throw new IOException("Formato de catálogo inválido.");
        try {
            PlanejamentoBackup.proxima(LocalDateTime.of(2026,1,1,0,0),d.preferencias().horario(),d.preferencias().diasSemana());
            var codigos=new HashSet<UUID>();
            for (Registro r:d.registros()) {
                if (r==null || r.codigo()==null || !codigos.add(r.codigo()) || r.estado()==null || r.tipo()==null || r.origem()==null || r.inicio()==null)
                    throw new IOException("Registro de backup inválido.");
                if (r.tipo()==Tipo.BACKUP) config.caminho(r.arquivo());
            }
        } catch (RuntimeException ex) { throw new IOException("Conteúdo do catálogo inválido.",ex); }
    }
    public synchronized boolean restoreInterrompido() { return dados.recuperacaoNecessaria(); }
    public synchronized void recuperacao(boolean necessaria) {
        salvar(new Dados(1,dados.preferencias(),dados.proximaExecucao(),dados.registros(),necessaria));
    }
    public synchronized Preferencias preferencias() { return dados.preferencias(); }
    public synchronized LocalDateTime proximaExecucao() { return dados.proximaExecucao(); }
    public synchronized List<Registro> registros() { return List.copyOf(dados.registros()); }
    public synchronized Registro obter(UUID codigo) {
        return dados.registros().stream().filter(r -> r.codigo().equals(codigo)).findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Operação de backup não encontrada."));
    }
    public synchronized void configurar(Preferencias p,LocalDateTime proxima) {
        salvar(new Dados(1,p,proxima,dados.registros(),dados.recuperacaoNecessaria()));
    }
    public synchronized void adicionar(Registro registro) {
        var lista=new ArrayList<>(dados.registros()); lista.add(registro);
        salvar(new Dados(1,dados.preferencias(),dados.proximaExecucao(),List.copyOf(lista),dados.recuperacaoNecessaria()));
    }
    public synchronized void atualizar(Registro registro) {
        if (dados.registros().stream().noneMatch(r -> r.codigo().equals(registro.codigo())))
            throw new IllegalStateException("Operação não registrada.");
        salvar(new Dados(1,dados.preferencias(),dados.proximaExecucao(),
                dados.registros().stream().map(r -> r.codigo().equals(registro.codigo())?registro:r).toList(),dados.recuperacaoNecessaria()));
    }
    private void salvar(Dados novos) {
        if (canal==null) throw new IllegalStateException("Catálogo de backups não inicializado.");
        Path temporario=null;
        try {
            temporario=Files.createTempFile(config.catalogo,"catalogo-",".tmp");
            json.writeValue(temporario.toFile(),novos);
            try (FileChannel arquivo=FileChannel.open(temporario,WRITE)) { arquivo.force(true); }
            try { Files.move(temporario,config.catalogo.resolve("catalogo.json"),ATOMIC_MOVE,REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporario,config.catalogo.resolve("catalogo.json"),REPLACE_EXISTING); }
            dados=novos;
        } catch (IOException ex) { throw new IllegalStateException("Não foi possível salvar o catálogo de backups.",ex); }
        finally { if (temporario!=null) try { Files.deleteIfExists(temporario); } catch (IOException ignored) {} }
    }
    @PreDestroy public void fechar() {
        try { if (trava!=null) trava.release(); } catch (IOException ignored) {}
        try { if (canal!=null) canal.close(); } catch (IOException ignored) {}
    }
}
