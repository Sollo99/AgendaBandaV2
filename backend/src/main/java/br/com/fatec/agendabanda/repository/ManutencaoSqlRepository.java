package br.com.fatec.agendabanda.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import static br.com.fatec.agendabanda.repository.ExecucaoSql.executar;

@Repository
public class ManutencaoSqlRepository {
    private final JdbcTemplate jdbc;
    public ManutencaoSqlRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void limparHistoricos(LocalDateTime limiteEventos, LocalDateTime limiteOperacoes) {
        executar(() -> jdbc.update("EXEC dbo.usp_limpar_historicos ?,?,?",
                Timestamp.valueOf(limiteEventos), Timestamp.valueOf(limiteOperacoes), 1000));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int limparSessoes(LocalDateTime agora) {
        return executar(() -> jdbc.queryForObject("EXEC dbo.usp_limpar_sessoes ?,?", Integer.class, Timestamp.valueOf(agora), 1000));
    }
}
