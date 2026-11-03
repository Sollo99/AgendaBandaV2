package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.exception.RegraNegocioException;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import br.com.fatec.agendabanda.repository.ManutencaoSqlRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class RetencaoHistoricosJob {
    private final ManutencaoSqlRepository manutencao;
    private final Clock clock;
    private final BackupManutencao manutencaoBackup;
    private final TransactionTemplate transacoes;
    @Value("${app.auditoria.retencao.eventos-dias:365}") private int eventosDias;
    @Value("${app.auditoria.retencao.operacoes-dias:180}") private int operacoesDias;

    public RetencaoHistoricosJob(ManutencaoSqlRepository manutencao, Clock clock, BackupManutencao manutencaoBackup, PlatformTransactionManager manager) {
        this.manutencao=manutencao; this.clock=clock; this.manutencaoBackup=manutencaoBackup; this.transacoes=new TransactionTemplate(manager);
    }

    @PostConstruct
    public void validar() {
        if (eventosDias < 1 || operacoesDias < 1) throw new RegraNegocioException("Retencao deve ser positiva.");
    }

    @Scheduled(initialDelayString="${app.auditoria.retencao.varredura-ms:3600000}",
            fixedDelayString="${app.auditoria.retencao.varredura-ms:3600000}")
    public void limparVencidos() {
        try(var leitura=manutencaoBackup.tentarLeitura()) {
            if (leitura==null) return;
            var agora = LocalDateTime.now(clock);
            transacoes.executeWithoutResult(status -> manutencao.limparHistoricos(agora.minusDays(eventosDias), agora.minusDays(operacoesDias)));
        }
    }
}
