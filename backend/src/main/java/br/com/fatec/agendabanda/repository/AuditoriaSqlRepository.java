package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.security.EventoAutenticacao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Types;
import static br.com.fatec.agendabanda.repository.ExecucaoSql.executar;

@Repository
public class AuditoriaSqlRepository {
    private final JdbcTemplate jdbc;
    public AuditoriaSqlRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(EventoAutenticacao evento) {
        executar(() -> jdbc.update("EXEC dbo.usp_registrar_autenticacao ?,?,?,?",
                new SqlParameterValue(Types.NVARCHAR,evento.operacao().name()),
                new SqlParameterValue(Types.INTEGER,evento.codigoMembro()),
                new SqlParameterValue(Types.NVARCHAR,evento.motivo().name()),
                new SqlParameterValue(Types.NVARCHAR,evento.recurso().name())));
    }
}
