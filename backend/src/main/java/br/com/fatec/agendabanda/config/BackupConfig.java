package br.com.fatec.agendabanda.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.nio.file.*;

@Component
public class BackupConfig {
    public final boolean habilitado, usarMasterAutomaticamente, detectarDiretorio;
    public final String banco, jdbcUrl, usuario, senha;
    public volatile String diretorioSql;
    public final Path catalogo;
    public volatile Path diretorioDownload;
    public final int timeoutSegundos;
    private final boolean detectarDownload;

    public BackupConfig(Environment env, DataSourceProperties datasource) {
        habilitado=env.getProperty("app.backup.habilitado",Boolean.class,true);
        banco=env.getProperty("app.backup.banco","AgendaBanda");
        String url=env.getProperty("app.backup.jdbc-url","");
        usarMasterAutomaticamente=url.isBlank();
        jdbcUrl=usarMasterAutomaticamente?datasource.determineUrl():url;
        usuario=valorOu(env.getProperty("app.backup.usuario",""),datasource.determineUsername());
        senha=valorOu(env.getProperty("app.backup.senha",""),datasource.determinePassword());
        diretorioSql=normalizar(env.getProperty("app.backup.diretorio-sql",""));
        detectarDiretorio=diretorioSql.isBlank();
        catalogo=Path.of(env.getProperty("app.backup.catalogo","data/backup-admin")).toAbsolutePath().normalize();
        String download=env.getProperty("app.backup.diretorio-download","");
        detectarDownload=download.isBlank();
        diretorioDownload=detectarDownload?null:Path.of(download).toAbsolutePath().normalize();
        timeoutSegundos=env.getProperty("app.backup.timeout-segundos",Integer.class,1800);
        if (!banco.matches("[A-Za-z][A-Za-z0-9_]{0,127}") || banco.matches("(?i)master|model|msdb|tempdb"))
            throw new IllegalArgumentException("Nome do banco de backup inválido.");
        if (timeoutSegundos<30 || timeoutSegundos>7200) throw new IllegalArgumentException("Timeout de backup deve estar entre 30 e 7200 segundos.");
        validarDiretorio(diretorioSql);
        detectarPastaDownload();
    }
    public synchronized void usarDiretorioDetectado(String diretorio) {
        String normalizado=normalizar(diretorio);
        validarDiretorio(normalizado);
        if (normalizado.isBlank()) throw new IllegalArgumentException("O SQL Server não informou um diretório de backup.");
        if (detectarDiretorio) diretorioSql=normalizado;
        detectarPastaDownload();
    }
    private void detectarPastaDownload() {
        if (detectarDownload && !diretorioSql.isBlank()) {
            Path pasta=Path.of(diretorioSql).toAbsolutePath().normalize();
            if (Files.isDirectory(pasta) && Files.isReadable(pasta)) diretorioDownload=pasta;
        }
    }
    private void validarDiretorio(String diretorio) {
        if (!diretorio.isEmpty() && !(diretorio.startsWith("/") || diretorio.matches("[A-Za-z]:/.*")))
            throw new IllegalArgumentException("O diretório SQL dos backups deve ser absoluto.");
    }
    private String normalizar(String valor) {
        String caminho=valor.replace('\\','/');
        while(caminho.length()>1 && caminho.endsWith("/") && !caminho.matches("[A-Za-z]:/")) caminho=caminho.substring(0,caminho.length()-1);
        return caminho;
    }
    private String valorOu(String valor,String padrao) { return valor.isEmpty()?padrao:valor; }
    public boolean configurado() { return habilitado && !diretorioSql.isBlank() && !jdbcUrl.isBlank(); }
    public String caminho(String arquivo) {
        if (!arquivo.matches("agendabanda_[0-9a-f-]{36}\\.bak")) throw new IllegalArgumentException("Arquivo de backup inválido.");
        return diretorioSql+(diretorioSql.endsWith("/")?"":"/")+arquivo;
    }
}
