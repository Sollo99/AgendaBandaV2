package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.exception.RegraNegocioException;

import br.com.fatec.agendabanda.model.RedefinicaoSenha;
import br.com.fatec.agendabanda.repository.MembroRepository;
import br.com.fatec.agendabanda.repository.RedefinicaoSenhaRepository;
import br.com.fatec.agendabanda.repository.RecuperacaoSqlRepository;
import br.com.fatec.agendabanda.exception.RecuperacaoExpiradaException;
import jakarta.persistence.EntityManager;
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
    private final RecuperacaoSqlRepository sql;
    private final EntityManager entityManager;
    private final Clock clock;
    private final long horasPendente;
    private final long horasAprovada;

    public CicloRecuperacaoService(RedefinicaoSenhaRepository repository, MembroRepository membroRepository,
            RecuperacaoSqlRepository sql, EntityManager entityManager, Clock clock,
            @Value("${app.auth.recuperacao.pendente-horas:24}") long horasPendente,
            @Value("${app.auth.recuperacao.aprovada-horas:2}") long horasAprovada) {
        if (horasPendente <= 0 || horasAprovada <= 0) throw new RegraNegocioException("Os prazos de recuperação devem ser positivos.");
        this.repository = repository;
        this.membroRepository = membroRepository;
        this.sql = sql;
        this.entityManager = entityManager;
        this.clock = clock;
        this.horasPendente = horasPendente;
        this.horasAprovada = horasAprovada;
    }

    public LocalDateTime agora() { return LocalDateTime.now(clock); }
    public LocalDateTime prazoPendente() { return agora().plusHours(horasPendente); }
    public LocalDateTime prazoAprovado() { return agora().plusHours(horasAprovada); }

    // Chamadores de expirarSeNecessario já possuem o bloqueio do membro, sempre antes do pedido.
    public boolean expirarSeNecessario(RedefinicaoSenha pedido) {
        if (ABERTAS.contains(pedido.getStatus()) && sql.expirar(pedido.getMembro().getCodigo(), agora()) > 0) {
            // JDBC alterou a linha: atualiza a instancia gerenciada sem regravar estado antigo.
            entityManager.refresh(pedido);
            return true;
        }
        return false;
    }

    @Transactional
    public Optional<RedefinicaoSenha> abertaValida(Integer codigoMembro) {
        if (membroRepository.bloquearPorCodigo(codigoMembro).isEmpty()) return Optional.empty();
        sql.expirar(codigoMembro, agora());
        return repository.findByMembroCodigoAndStatusIn(codigoMembro, ABERTAS);
    }

    @Transactional
    public void cancelarAbertas(Integer codigoMembro, String motivo) {
        if (membroRepository.bloquearPorCodigo(codigoMembro).isEmpty()) return;
        sql.cancelar(codigoMembro, agora(), motivo, null);
    }

    // Chamador administrativo ja possui o lock global e o lock do membro.
    // A excecao de expiracao precisa continuar sem rollback no servico que trata o pedido.
    public void cancelarSolicitacao(RedefinicaoSenha pedido, String motivo) {
        int alteradas = sql.cancelar(pedido.getMembro().getCodigo(), agora(), motivo, pedido.getCodigoRedefinicao());
        if (alteradas == 0) throw new RegraNegocioException("A solicitação já está encerrada.");
        entityManager.refresh(pedido);
        if ("EXPIRADA".equals(pedido.getStatus())) throw new RecuperacaoExpiradaException();
    }

    public void encerrar(RedefinicaoSenha pedido, String status, String motivo) {
        pedido.setStatus(status);
        pedido.setEncerradoEm(agora());
        pedido.setMotivoEncerramento(motivo);
    }
}
