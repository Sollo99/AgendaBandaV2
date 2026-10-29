package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.exception.RegraNegocioException;

import br.com.fatec.agendabanda.model.RedefinicaoSenha;
import br.com.fatec.agendabanda.repository.MembroRepository;
import br.com.fatec.agendabanda.repository.RedefinicaoSenhaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CicloRecuperacaoService {
    public static final List<String> ABERTAS = List.of("PENDENTE", "APROVADA");
    private final RedefinicaoSenhaRepository repository;
    private final MembroRepository membroRepository;
    private final Clock clock;
    private final long horasPendente;
    private final long horasAprovada;

    public CicloRecuperacaoService(RedefinicaoSenhaRepository repository, MembroRepository membroRepository, Clock clock,
            @Value("${app.auth.recuperacao.pendente-horas:24}") long horasPendente,
            @Value("${app.auth.recuperacao.aprovada-horas:2}") long horasAprovada) {
        if (horasPendente <= 0 || horasAprovada <= 0) throw new RegraNegocioException("Os prazos de recuperação devem ser positivos.");
        this.repository = repository;
        this.membroRepository = membroRepository;
        this.clock = clock;
        this.horasPendente = horasPendente;
        this.horasAprovada = horasAprovada;
    }

    public LocalDateTime agora() { return LocalDateTime.now(clock); }
    public LocalDateTime prazoPendente() { return agora().plusHours(horasPendente); }
    public LocalDateTime prazoAprovado() { return agora().plusHours(horasAprovada); }

    // Chamadores de expirarSeNecessario já possuem o bloqueio do membro, sempre antes do pedido.
    public boolean expirarSeNecessario(RedefinicaoSenha pedido) {
        if (ABERTAS.contains(pedido.getStatus()) && !pedido.getExpiraEm().isAfter(agora())) {
            encerrar(pedido, "EXPIRADA", "Prazo de recuperação encerrado.");
            repository.saveAndFlush(pedido);
            return true;
        }
        return false;
    }

    @Transactional
    public Optional<RedefinicaoSenha> abertaValida(Integer codigoMembro) {
        if (membroRepository.bloquearPorCodigo(codigoMembro).isEmpty()) return Optional.empty();
        var aberta = repository.findByMembroCodigoAndStatusIn(codigoMembro, ABERTAS);
        if (aberta.isPresent() && expirarSeNecessario(aberta.get())) return Optional.empty();
        return aberta;
    }

    @Transactional
    public void cancelarAbertas(Integer codigoMembro, String motivo) {
        if (membroRepository.bloquearPorCodigo(codigoMembro).isEmpty()) return;
        repository.findByMembroCodigoAndStatusIn(codigoMembro, ABERTAS).ifPresent(pedido -> {
            encerrar(pedido, "CANCELADA", motivo);
            repository.saveAndFlush(pedido);
        });
    }

    public void encerrar(RedefinicaoSenha pedido, String status, String motivo) {
        pedido.setStatus(status);
        pedido.setEncerradoEm(agora());
        pedido.setMotivoEncerramento(motivo);
    }
}
