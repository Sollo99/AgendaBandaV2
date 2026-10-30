package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.dto.RelatorioDtos.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class RelatorioSqlRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public RelatorioSqlRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    private MapSqlParameterSource periodo(LocalDateTime inicio, LocalDateTime fim) {
        return new MapSqlParameterSource("inicio", Timestamp.valueOf(inicio)).addValue("fim", Timestamp.valueOf(fim));
    }

    public List<ResumoTipo> resumo(LocalDateTime inicio, LocalDateTime fim) {
        return jdbc.query("SELECT * FROM dbo.fn_resumo_periodo(:inicio,:fim) ORDER BY tipo_nome", periodo(inicio, fim),
                (rs, row) -> new ResumoTipo(rs.getInt("codigo_tipo"), rs.getString("tipo_nome"), rs.getInt("total"),
                        rs.getInt("agendados"), rs.getInt("realizados"), rs.getInt("cancelados"), rs.getLong("minutos_programados")));
    }

    public List<PresencaMusico> presencas(LocalDateTime inicio, LocalDateTime fim) {
        return jdbc.query("""
                SELECT mu.codigo, m.nome, mu.instrumento, COUNT(v.codigo_agenda) AS eventos,
                    COALESCE(SUM(v.realizado),0) AS realizados, COALESCE(SUM(v.presenca),0) AS presencas,
                    COALESCE(SUM(v.ausencia),0) AS ausencias, COALESCE(SUM(v.sem_registro),0) AS sem_registro,
                    dbo.fn_taxa_presenca_musico(mu.codigo,:inicio,:fim) AS taxa
                FROM musicos mu JOIN membros m ON m.codigo=mu.codigo
                LEFT JOIN dbo.vw_presenca_musicos v ON v.codigo_musico=mu.codigo AND v.dt_hora_in>=:inicio AND v.dt_hora_in<:fim
                GROUP BY mu.codigo,m.nome,mu.instrumento ORDER BY m.nome,mu.codigo
                """, periodo(inicio, fim), (rs, row) -> new PresencaMusico(rs.getInt("codigo"), rs.getString("nome"),
                rs.getString("instrumento"), rs.getInt("eventos"), rs.getInt("realizados"), rs.getInt("presencas"),
                rs.getInt("ausencias"), rs.getInt("sem_registro"), rs.getBigDecimal("taxa")));
    }

    public List<OcupacaoLocal> locais(LocalDateTime inicio, LocalDateTime fim) {
        return jdbc.query("""
                SELECT l.codigo_local,l.nome,c.nome AS cidade,c.uf,COUNT(v.codigo_agenda) AS eventos,
                    COALESCE(SUM(v.agendado),0) AS agendados,COALESCE(SUM(v.realizado),0) AS realizados,
                    COALESCE(SUM(v.cancelado),0) AS cancelados,COALESCE(SUM(v.minutos_programados),0) AS minutos
                FROM locais l JOIN cidades c ON c.codigo_cidade=l.codigo_cidade
                LEFT JOIN dbo.vw_ocupacao_locais v ON v.codigo_local=l.codigo_local AND v.dt_hora_in>=:inicio AND v.dt_hora_in<:fim
                GROUP BY l.codigo_local,l.nome,c.nome,c.uf ORDER BY l.nome,l.codigo_local
                """, periodo(inicio, fim), (rs, row) -> new OcupacaoLocal(rs.getInt("codigo_local"), rs.getString("nome"),
                rs.getString("cidade"), rs.getString("uf"), rs.getInt("eventos"), rs.getInt("agendados"),
                rs.getInt("realizados"), rs.getInt("cancelados"), rs.getLong("minutos")));
    }

    public List<Historico> historicos(LocalDateTime inicio, LocalDateTime fim) {
        return jdbc.query("""
                SELECT TOP (100) h.* FROM dbo.vw_historico_unificado h
                WHERE h.data_hora>=:inicio AND h.data_hora<:fim
                ORDER BY h.data_hora DESC,h.codigo DESC,h.entidade
                """, periodo(inicio, fim), (rs, row) -> new Historico(rs.getLong("codigo"), rs.getString("entidade"),
                rs.getString("registro"), rs.getString("operacao"), rs.getTimestamp("data_hora").toLocalDateTime(),
                rs.getObject("membro_responsavel", Integer.class), rs.getString("responsavel"),
                rs.getString("dados_anteriores"), rs.getString("dados_novos")));
    }
}
