package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.config.BackupConfig;
import br.com.fatec.agendabanda.dto.BackupDtos.Registro;
import br.com.fatec.agendabanda.exception.RegraNegocioException;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.*;
import java.util.*;

/** Comandos administrativos usam conexao exclusiva em master e autocommit, fora do JPA. */
@Repository
public class BackupSqlRepository {
    public record Arquivo(long tamanhoBytes,String assinaturaSchema) {}
    private record ArquivoBanco(int id,String tipo,String nome,String caminho) {}
    private final BackupConfig config;
    private final DataSource aplicacao;
    public BackupSqlRepository(BackupConfig config,DataSource aplicacao) { this.config=config; this.aplicacao=aplicacao; }

    @PostConstruct
    public void detectarDiretorio() {
        if (!config.habilitado || !config.detectarDiretorio) return;
        try(Connection c=conectarMaster();Statement s=c.createStatement()) {
            s.setQueryTimeout(15);
            try(ResultSet r=s.executeQuery("SELECT dbo.fn_agendabanda_diretorio_backup()")) {
                if (!r.next()) throw new SQLException("Diretório não informado.");
                config.usarDiretorioDetectado(r.getString(1));
            }
        } catch(SQLException|RuntimeException ex) {
            LoggerFactory.getLogger(BackupSqlRepository.class).warn("Não foi possível detectar a pasta de backup. Confira a execução do script 00 na mesma instância SQL.",ex);
        }
    }

    private Connection conectarMaster() throws SQLException {
        var origem=new SQLServerDataSource();
        origem.setURL(config.jdbcUrl);
        if (config.usarMasterAutomaticamente) origem.setDatabaseName("master");
        origem.setUser(config.usuario);origem.setPassword(config.senha);origem.setLoginTimeout(10);
        Connection c=origem.getConnection();
        if (!"master".equalsIgnoreCase(c.getCatalog())) { c.close();throw new RegraNegocioException("A conexão administrativa de backup deve usar o banco master."); }
        c.setAutoCommit(true);
        return c;
    }

    public Connection abrir() throws SQLException {
        Connection c=conectarMaster();
        try {
            try (Connection app=aplicacao.getConnection()) {
                if (!config.banco.equalsIgnoreCase(app.getCatalog()) || !servidor(c).equals(servidor(app)))
                    throw new RegraNegocioException("A conexão de backup deve usar a mesma instância e o banco configurado na aplicação.");
            }
            // A permissao e avaliada no banco alvo, retornando a master antes de qualquer RESTORE.
            try(PreparedStatement s=c.prepareStatement("USE "+banco()+"; SELECT HAS_PERMS_BY_NAME(DB_NAME(),N'DATABASE',N'VIEW DEFINITION')")) {
                try(ResultSet r=s.executeQuery()) {
                    if (!r.next() || r.getInt(1)!=1) throw new RegraNegocioException("A conta administrativa precisa consultar a definição da estrutura do banco para validar os backups.");
                }
            } finally { c.setCatalog("master"); }
            try (PreparedStatement s=c.prepareStatement("""
                    DECLARE @r INT;
                    EXEC @r=sys.sp_getapplock @Resource=?,@LockMode='Exclusive',@LockOwner='Session',@LockTimeout=0;
                    SELECT @r;
                    """)) {
                s.setString(1,"AgendaBanda:backup:"+config.banco);
                try(ResultSet r=s.executeQuery()) { if (!r.next() || r.getInt(1)<0) throw new RegraNegocioException("Outra operação de backup está em andamento nesta instância SQL."); }
            }
            return c;
        } catch (SQLException|RuntimeException ex) { c.close(); throw ex; }
    }
    private String servidor(Connection c) throws SQLException {
        try (Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT CONVERT(nvarchar(128),SERVERPROPERTY('ServerName'))")) {
            if (!r.next()) throw new SQLException("Instância SQL indisponível.");
            return r.getString(1).toLowerCase(Locale.ROOT);
        }
    }
    public Arquivo criar(Connection c,Registro registro) throws SQLException {
        String assinatura=assinatura(c);
        executar(c,"BACKUP DATABASE "+banco()+" TO DISK=? WITH COPY_ONLY,CHECKSUM,NOFORMAT,NOINIT,NAME=?",
                config.caminho(registro.arquivo()),"AgendaBanda:"+registro.codigo());
        long tamanho=verificar(c,registro);
        return new Arquivo(tamanho,assinatura);
    }
    public long verificar(Connection c,Registro registro) throws SQLException {
        try(PreparedStatement s=inspecao(c,registro,"VERIFY")) { consumir(s); }
        try (PreparedStatement s=inspecao(c,registro,"HEADER")) {
            try (ResultSet r=s.executeQuery()) {
                if (!r.next() || r.getInt("Position")!=1 || r.getInt("BackupType")!=1
                        || !config.banco.equalsIgnoreCase(r.getString("DatabaseName"))
                        || !("AgendaBanda:"+registro.codigo()).equals(r.getString("BackupName"))
                        || !r.getBoolean("HasBackupChecksums") || r.getBoolean("IsDamaged"))
                    throw new RegraNegocioException("O arquivo não corresponde ao backup completo selecionado.");
                long tamanho=r.getLong("CompressedBackupSize");
                if (r.next()) throw new RegraNegocioException("O arquivo contém conjuntos de backup adicionais.");
                return tamanho;
            }
        }
    }
    public String assinatura(Connection c) throws SQLException {
        MessageDigest hash;
        try { hash=MessageDigest.getInstance("SHA-256"); } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
        adicionar(hash,c,"SELECT s.name,o.name,o.type,COALESCE(m.definition,N'') FROM "+banco()+".sys.objects o JOIN "+banco()+".sys.schemas s ON s.schema_id=o.schema_id LEFT JOIN "+banco()+".sys.sql_modules m ON m.object_id=o.object_id WHERE o.is_ms_shipped=0 AND o.type IN ('U','V','P','FN','IF','TF','TR') ORDER BY s.name,o.type,o.name");
        adicionar(hash,c,"SELECT o.name,c.name,c.system_type_id,c.max_length,c.precision,c.scale,c.is_nullable,c.is_identity,COALESCE(c.collation_name,N''),COALESCE(d.definition,N'') FROM "+banco()+".sys.columns c JOIN "+banco()+".sys.objects o ON o.object_id=c.object_id LEFT JOIN "+banco()+".sys.default_constraints d ON d.object_id=c.default_object_id WHERE o.is_ms_shipped=0 AND o.type='U' ORDER BY o.name,c.column_id");
        adicionar(hash,c,"SELECT o.name,c.name,c.definition FROM "+banco()+".sys.check_constraints c JOIN "+banco()+".sys.objects o ON o.object_id=c.parent_object_id ORDER BY o.name,c.name");
        return HexFormat.of().formatHex(hash.digest());
    }
    private void adicionar(MessageDigest hash,Connection c,String sql) throws SQLException {
        try (Statement s=c.createStatement()) {
            s.setQueryTimeout(config.timeoutSegundos);
            try (ResultSet r=s.executeQuery(sql)) {
                int colunas=r.getMetaData().getColumnCount();
                while (r.next()) for(int i=1;i<=colunas;i++) {
                    byte[] valor=Objects.toString(r.getString(i),"").getBytes(StandardCharsets.UTF_8);
                    hash.update((valor.length+":").getBytes(StandardCharsets.US_ASCII)); hash.update(valor);
                }
            }
        }
    }
    public void restaurar(Connection c,Registro registro,Runnable antesDeAlterar) throws SQLException {
        List<ArquivoBanco> atuais=new ArrayList<>();
        try(PreparedStatement s=c.prepareStatement("SELECT file_id,type_desc,name,physical_name FROM sys.master_files WHERE database_id=DB_ID(?) ORDER BY file_id")) {
            s.setString(1,config.banco);
            try(ResultSet r=s.executeQuery()) { while(r.next()) atuais.add(new ArquivoBanco(r.getInt(1),r.getString(2),r.getString(3),r.getString(4))); }
        }
        List<String> movimentos=new ArrayList<>();
        try(PreparedStatement s=inspecao(c,registro,"FILES")) {
            try(ResultSet r=s.executeQuery()) {
                while(r.next()) {
                    int id=r.getInt("FileId"); String tipo=r.getString("Type");
                    var atual=atuais.stream().filter(a -> a.id()==id).findFirst()
                            .orElseThrow(() -> new RegraNegocioException("Os arquivos do backup não correspondem à estrutura atual."));
                    if (!(("D".equals(tipo)&&"ROWS".equals(atual.tipo())) || ("L".equals(tipo)&&"LOG".equals(atual.tipo()))))
                        throw new RegraNegocioException("Este backup possui um tipo de arquivo não suportado pela restauração.");
                    movimentos.add("MOVE "+literal(r.getString("LogicalName"))+" TO "+literal(atual.caminho()));
                }
            }
        }
        if (movimentos.isEmpty() || movimentos.size()!=atuais.size()) throw new RegraNegocioException("A quantidade de arquivos do backup difere da base atual.");
        antesDeAlterar.run();
        SQLException erro=null;
        try {
            executar(c,"ALTER DATABASE "+banco()+" SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
            executar(c,"RESTORE DATABASE "+banco()+" FROM DISK=? WITH REPLACE,RECOVERY,CHECKSUM,"+String.join(",",movimentos),config.caminho(registro.arquivo()));
        } catch(SQLException ex) { erro=ex; throw ex; }
        finally {
            try { executar(c,"ALTER DATABASE "+banco()+" SET MULTI_USER WITH ROLLBACK IMMEDIATE"); }
            catch(SQLException ex) { if (erro!=null) erro.addSuppressed(ex); else throw ex; }
        }
    }
    public boolean online(Connection c) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM sys.databases WHERE name=? AND state_desc='ONLINE' AND user_access_desc='MULTI_USER'")) {
            s.setString(1,config.banco);try(ResultSet r=s.executeQuery()) { return r.next(); }
        }
    }
    public void exigirPermissaoRestauracao(Connection c) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT CASE WHEN IS_SRVROLEMEMBER(N'sysadmin')=1 OR IS_SRVROLEMEMBER(N'dbcreator')=1 OR SUSER_SID()=(SELECT owner_sid FROM sys.databases WHERE name=?) THEN 1 ELSE 0 END")) {
            s.setString(1,config.banco);
            try(ResultSet r=s.executeQuery()) {
                if (!r.next() || r.getInt(1)!=1)
                    throw new RegraNegocioException("A conta SQL configurada não tem permissão de restauração. Solicite a configuração administrativa do servidor.");
            }
        }
    }
    public void invalidarCredenciais(Connection c) throws SQLException {
        // Executado somente depois do RESTORE. Nao deixar sessoes ou aprovacoes antigas ressurgirem.
        c.setAutoCommit(false);
        try {
            executar(c,"DELETE FROM "+banco()+".dbo.sessoes");
            executar(c,"UPDATE "+banco()+".dbo.redefinicoes_senha SET status=N'CANCELADA',encerrado_em=CONVERT(datetime2,SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time'),motivo_encerramento=N'Banco restaurado; autorização anterior invalidada.' WHERE status IN (N'PENDENTE',N'APROVADA')");
            c.commit();
        } catch(SQLException|RuntimeException ex) { c.rollback();throw ex; }
        finally { c.setAutoCommit(true); }
    }
    private void executar(Connection c,String sql,Object...valores) throws SQLException {
        try(PreparedStatement s=c.prepareStatement(sql)) {
            s.setQueryTimeout(config.timeoutSegundos);
            for(int i=0;i<valores.length;i++) s.setNString(i+1,(String)valores[i]);
            consumir(s);
        }
    }
    private PreparedStatement inspecao(Connection c,Registro registro,String modo) throws SQLException {
        PreparedStatement s;
        if (config.detectarDiretorio) {
            s=c.prepareStatement("EXEC dbo.usp_agendabanda_inspecionar_backup ?,?");
            s.setString(1,registro.codigo().toString());s.setString(2,modo);
        } else {
            String comando=switch(modo) { case "VERIFY" -> "RESTORE VERIFYONLY FROM DISK=? WITH CHECKSUM";case "HEADER" -> "RESTORE HEADERONLY FROM DISK=?";case "FILES" -> "RESTORE FILELISTONLY FROM DISK=?";default -> throw new IllegalArgumentException("Inspeção inválida."); };
            s=c.prepareStatement(comando);s.setNString(1,config.caminho(registro.arquivo()));
        }
        s.setQueryTimeout(config.timeoutSegundos);
        return s;
    }
    private void consumir(PreparedStatement s) throws SQLException {
        boolean resultado=s.execute();
        while(resultado || s.getUpdateCount()!=-1) resultado=s.getMoreResults(Statement.CLOSE_CURRENT_RESULT);
    }
    private String banco() { return "["+config.banco+"]"; }
    private String literal(String valor) { return "N'"+valor.replace("'","''")+"'"; }
}
