package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.exception.RegraNegocioException;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class RetencaoHistoricosJob {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    @Value("${app.auditoria.retencao.eventos-dias:365}") private int eventosDias;
    @Value("${app.auditoria.retencao.operacoes-dias:180}") private int operacoesDias;

    public RetencaoHistoricosJob(JdbcTemplate jdbc, Clock clock) { this.jdbc=jdbc; this.clock=clock; }

    @PostConstruct
    public void validar() {
        if (eventosDias < 1 || operacoesDias < 1) throw new RegraNegocioException("Retencao deve ser positiva.");
    }

    @Scheduled(initialDelayString="${app.auditoria.retencao.varredura-ms:3600000}",
            fixedDelayString="${app.auditoria.retencao.varredura-ms:3600000}")
    @Transactional
    public void limparVencidos() {
        var agora = LocalDateTime.now(clock);
        jdbc.update("DELETE TOP (1000) FROM historico_eventos WHERE data_hora < ?", Timestamp.valueOf(agora.minusDays(eventosDias)));
        jdbc.update("DELETE TOP (1000) FROM historico_operacoes WHERE data_hora < ?", Timestamp.valueOf(agora.minusDays(operacoesDias)));
    }
}
