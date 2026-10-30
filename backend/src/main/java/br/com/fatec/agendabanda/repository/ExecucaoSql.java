package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.exception.ConflitoException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.ConcurrencyFailureException;
import java.sql.SQLException;
import java.util.function.Supplier;

/** Traduz somente os codigos publicos das procedures, sem expor detalhes do driver. */
final class ExecucaoSql {
    private ExecucaoSql() {}

    static <T> T executar(Supplier<T> operacao) {
        try { return operacao.get(); }
        catch (DataAccessException ex) {
            for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
                if (causa instanceof SQLException sql) {
                    for (SQLException erro = sql; erro != null; erro = erro.getNextException()) {
                        if (erro.getErrorCode() == 50004) throw new ConcurrencyFailureException("Recurso em uso. Aguarde e tente novamente.", ex);
                        if (erro.getErrorCode() == 50003) throw new SecurityException("Operação não autorizada.");
                        if (erro.getErrorCode() == 50002) throw new ConflitoException("Existe conflito de horário para o local ou para um dos músicos.");
                        if (erro.getErrorCode() == 50001) throw MensagemSqlPublica.excecao(erro.getMessage());
                    }
                }
            }
            throw ex;
        }
    }
}
