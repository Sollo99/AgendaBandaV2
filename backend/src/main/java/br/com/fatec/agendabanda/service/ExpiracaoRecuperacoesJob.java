package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.repository.RedefinicaoSenhaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiracaoRecuperacoesJob {
    private final RedefinicaoSenhaRepository repository;
    private final CicloRecuperacaoService ciclo;

    public ExpiracaoRecuperacoesJob(RedefinicaoSenhaRepository repository, CicloRecuperacaoService ciclo) {
        this.repository = repository;
        this.ciclo = ciclo;
    }

    @Scheduled(fixedDelayString = "${app.auth.recuperacao.varredura-ms:60000}", initialDelayString = "${app.auth.recuperacao.varredura-ms:60000}")
    public void expirar() {
        // Cada membro é processado em uma transação separada pelo serviço injetado.
        for (Integer codigo : repository.buscarMembrosComPedidosVencidos(ciclo.agora())) ciclo.abertaValida(codigo);
    }
}
