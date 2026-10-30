package br.com.fatec.agendabanda.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static br.com.fatec.agendabanda.repository.ExecucaoSql.executar;

@Repository
public class LocalSqlRepository {
    private final JdbcTemplate jdbc;
    public LocalSqlRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void excluir(Integer id) {
        executar(() -> jdbc.update("EXEC dbo.usp_excluir_local ?", id));
    }
}
