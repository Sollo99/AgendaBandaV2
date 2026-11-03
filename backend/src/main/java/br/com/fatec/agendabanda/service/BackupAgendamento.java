package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.BackupDtos.Origem;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BackupAgendamento {
    private final BackupService backups;
    public BackupAgendamento(BackupService backups) { this.backups=backups; }
    @EventListener(ApplicationReadyEvent.class)
    public void aoIniciar() {
        backups.recuperarInterrupcao();
        if (backups.painel().preferencias().aoIniciar()) {
            try { backups.automatico(Origem.INICIALIZACAO); }
            catch(RuntimeException ex) { LoggerFactory.getLogger(BackupAgendamento.class).error("Não foi possível iniciar o backup automático.",ex); }
        }
    }
    @Scheduled(initialDelay=60000,fixedDelay=60000)
    public void verificarHorario() { backups.agendarSeNecessario(); }
}
