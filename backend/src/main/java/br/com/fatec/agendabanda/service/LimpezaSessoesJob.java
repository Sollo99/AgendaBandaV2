package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.security.TokenService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LimpezaSessoesJob {
    private final TokenService tokens;
    private final BackupManutencao manutencao;
    public LimpezaSessoesJob(TokenService tokens,BackupManutencao manutencao) { this.tokens=tokens;this.manutencao=manutencao; }
    @Scheduled(initialDelayString="${app.auth.sessao.varredura-ms:60000}",fixedDelayString="${app.auth.sessao.varredura-ms:60000}")
    public void limpar() {
        try(var leitura=manutencao.tentarLeitura()) { if (leitura!=null) tokens.limparSessoesExpiradas(); }
    }
}
