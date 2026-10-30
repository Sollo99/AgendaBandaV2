package br.com.fatec.agendabanda.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Types;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import static br.com.fatec.agendabanda.repository.ExecucaoSql.executar;

@Repository
public class RecuperacaoSqlRepository {
    private final JdbcTemplate jdbc;
    public RecuperacaoSqlRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @Transactional(propagation = Propagation.MANDATORY)
    public int expirar(Integer membro, LocalDateTime agora) {
        return executar(() -> jdbc.queryForObject("EXEC dbo.usp_expirar_recuperacoes ?,?", Integer.class,
                membro, Timestamp.valueOf(agora)));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int cancelar(Integer membro, LocalDateTime agora, String motivo, Integer solicitacao) {
        return executar(() -> jdbc.queryForObject("EXEC dbo.usp_cancelar_recuperacoes ?,?,?,?", Integer.class,
                membro, Timestamp.valueOf(agora), new SqlParameterValue(Types.NVARCHAR, motivo), new SqlParameterValue(Types.INTEGER, solicitacao)));
    }
}
