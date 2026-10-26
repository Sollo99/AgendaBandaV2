package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.CadastroRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.sql.SQLException;
import br.com.fatec.agendabanda.security.LimitadorAutenticacao;

@Service
public class CadastroPublicoService {
    private final MembroService membros;
    private final LimitadorAutenticacao limitador;
    public CadastroPublicoService(MembroService membros, LimitadorAutenticacao limitador) {
        this.membros = membros;
        this.limitador = limitador;
    }

    // Fora da transação de criação: uma colisão concorrente já foi revertida antes da resposta pública.
    public void solicitar(CadastroRequest request) {
        limitador.verificarIdentificador(LimitadorAutenticacao.Tipo.CADASTRO, "cpf:" + request.cpf().replaceAll("[^0-9]", ""));
        limitador.verificarIdentificador(LimitadorAutenticacao.Tipo.CADASTRO, "email:" + request.email().trim());
        limitador.verificarIdentificador(LimitadorAutenticacao.Tipo.CADASTRO, "login:" + request.login().trim());
        try {
            membros.criarCadastro(request);
        } catch (DataIntegrityViolationException ex) {
            if (!duplicidadeSqlServer(ex)) throw ex;
        }
    }

    private boolean duplicidadeSqlServer(Throwable ex) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLException sql) {
                for (SQLException erro = sql; erro != null; erro = erro.getNextException()) {
                    if (erro.getErrorCode() == 2601 || erro.getErrorCode() == 2627) return true;
                }
            }
        }
        return false;
    }
}
