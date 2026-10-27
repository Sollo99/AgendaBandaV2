package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.security.SessaoUsuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

/** Compartilha a conexao JPA/JDBC e limpa o contexto antes de devolve-la ao pool. */
@Component
public class AuditoriaContexto {
    private static final Logger log = LoggerFactory.getLogger(AuditoriaContexto.class);
    private final JdbcTemplate jdbc;

    public AuditoriaContexto(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void prepararDoRequest() {
        Integer codigo = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                && attrs.getRequest().getAttribute("sessao") instanceof SessaoUsuario sessao) codigo = sessao.codigo();
        preparar(codigo);
    }

    public void preparar(Integer codigo) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Auditoria exige uma transação ativa.");
        }
        if (TransactionSynchronizationManager.hasResource(this)) return;
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            definir(connection, codigo);
            TransactionSynchronizationManager.bindResource(this, connection);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    TransactionSynchronizationManager.unbindResourceIfPossible(AuditoriaContexto.this);
                    try {
                        definir(connection, null);
                    } catch (SQLException ex) {
                        log.error("Falha ao limpar contexto de auditoria; descartando conexão.", ex);
                        try { connection.abort(Runnable::run); }
                        catch (SQLException erroAbort) { log.error("Falha ao descartar conexão de auditoria.", erroAbort); }
                    }
                }
            });
            return null;
        });
    }

    private void definir(Connection connection, Integer codigo) throws SQLException {
        try (var statement = connection.prepareStatement(
                "EXEC sys.sp_set_session_context @key=N'membro_responsavel', @value=?, @read_only=0")) {
            if (codigo == null) statement.setNull(1, Types.INTEGER);
            else statement.setInt(1, codigo);
            statement.execute();
        }
    }
}
