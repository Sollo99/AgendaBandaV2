package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.repository.AuditoriaSqlRepository;
import br.com.fatec.agendabanda.security.EventoAutenticacao;
import br.com.fatec.agendabanda.security.EventoAutenticacao.*;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AuditoriaAutenticacaoService {
    private final AuditoriaSqlRepository repository;
    private final TransactionTemplate isolada;

    public AuditoriaAutenticacaoService(AuditoriaSqlRepository repository, PlatformTransactionManager manager) {
        this.repository=repository;
        this.isolada=new TransactionTemplate(manager);
        this.isolada.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // A criacao/revogacao da sessao e seu historico sao confirmados juntos.
    public void login(Integer membro) {
        repository.registrar(new EventoAutenticacao(Operacao.LOGIN,membro,Motivo.SUCESSO,Recurso.LOGIN));
    }

    public void logout(Integer membro) {
        repository.registrar(new EventoAutenticacao(Operacao.LOGOUT,membro,Motivo.SUCESSO,Recurso.LOGOUT));
    }

    public void falhaLogin(Integer alvo) {
        registrarFalha(new EventoAutenticacao(Operacao.LOGIN_FALHOU,alvo,Motivo.CREDENCIAIS_INVALIDAS,Recurso.LOGIN));
    }

    public void negar(Integer membro, Motivo motivo, Recurso recurso) {
        registrarFalha(new EventoAutenticacao(Operacao.ACESSO_NEGADO,membro,motivo,recurso));
    }

    public void limitar(Recurso recurso) {
        registrarFalha(new EventoAutenticacao(Operacao.LIMITE_EXCEDIDO,null,Motivo.LIMITE_EXCEDIDO,recurso));
    }

    private void registrarFalha(EventoAutenticacao evento) {
        if (TransactionSynchronizationManager.isActualTransactionActive() && TransactionSynchronizationManager.isSynchronizationActive()) {
            // Aguarda liberar os locks da tentativa original antes de abrir outra transacao.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) { persistirIsolado(evento); }
            });
        } else persistirIsolado(evento);
    }

    private void persistirIsolado(EventoAutenticacao evento) {
        try { isolada.executeWithoutResult(status -> repository.registrar(evento)); }
        catch (RuntimeException ex) {
            // Uma falha de auditoria nao transforma uma recusa de acesso em erro interno.
            LoggerFactory.getLogger(AuditoriaAutenticacaoService.class).warn(
                    "Falha ao registrar evento de autenticacao {}.",evento.operacao(),ex);
        }
    }
}
