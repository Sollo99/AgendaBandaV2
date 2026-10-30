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
public class MembroSqlRepository {
    private final JdbcTemplate jdbc;

    public MembroSqlRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void alterarAcesso(Integer id, Boolean administrador, Boolean ativo) {
        executar(() -> jdbc.queryForObject("EXEC dbo.usp_alterar_acesso_membro ?,?,?", Integer.class,
                id, new SqlParameterValue(Types.BIT, administrador), new SqlParameterValue(Types.BIT, ativo)));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void decidirCadastro(Integer id, boolean aprovada, LocalDateTime agora) {
        executar(() -> jdbc.update("EXEC dbo.usp_decidir_cadastro ?,?,?", id, aprovada, Timestamp.valueOf(agora)));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void liberarIdentificadores(Integer id, String senhaHash, LocalDateTime agora) {
        executar(() -> jdbc.update("EXEC dbo.usp_liberar_identificadores ?,?,?", id,
                new SqlParameterValue(Types.NVARCHAR, senhaHash), Timestamp.valueOf(agora)));
    }
}
