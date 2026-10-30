package br.com.fatec.agendabanda.security;

import br.com.fatec.agendabanda.model.SessaoPersistida;
import br.com.fatec.agendabanda.repository.SessaoSqlRepository;
import br.com.fatec.agendabanda.repository.ManutencaoSqlRepository;
import br.com.fatec.agendabanda.repository.SessaoPersistidaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import br.com.fatec.agendabanda.exception.SessaoExpiradaException;
import java.time.Clock;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class TokenService {
    private static final int DURACAO_HORAS = 8;
    private final SessaoPersistidaRepository sessaoRepository;
    private final SessaoSqlRepository consultas;
    private final ManutencaoSqlRepository manutencao;
    private final Clock clock;

    public TokenService(SessaoPersistidaRepository sessaoRepository, SessaoSqlRepository consultas, ManutencaoSqlRepository manutencao, Clock clock) {
        this.sessaoRepository = sessaoRepository;
        this.consultas = consultas;
        this.manutencao = manutencao;
        this.clock = clock;
    }

    @Transactional
    public String criarToken(SessaoUsuario usuario) {
        String token = UUID.randomUUID().toString();
        var sessao = new SessaoPersistida();
        sessao.setTokenHash(hash(token));
        sessao.setCodigoMembro(usuario.codigo());
        sessao.setCriadoEm(LocalDateTime.now(clock));
        sessao.setExpiraEm(LocalDateTime.now(clock).plusHours(DURACAO_HORAS));
        sessaoRepository.save(sessao);
        return token;
    }

    @Transactional
    public Optional<SessaoUsuario> buscarSessao(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return consultas.buscarAtiva(hash(token), LocalDateTime.now(clock));
    }

    @Transactional
    public void removerToken(String token) {
        if (token != null && !token.isBlank()) sessaoRepository.deleteByTokenHash(hash(token));
    }

    @Transactional
    public void removerSessoesDoMembro(Integer codigoMembro) {
        sessaoRepository.deleteByCodigoMembro(codigoMembro);
    }

    @Scheduled(initialDelayString = "${app.auth.sessao.varredura-ms:60000}",
            fixedDelayString = "${app.auth.sessao.varredura-ms:60000}")
    @Transactional
    public int limparSessoesExpiradas() {
        return manutencao.limparSessoes(LocalDateTime.now(clock));
    }

    public String extrairToken(HttpServletRequest request) {
        return SegurancaHttp.cookie(request, SegurancaHttp.COOKIE_SESSAO);
    }

    public SessaoUsuario sessaoObrigatoria(HttpServletRequest request) {
        if (request.getAttribute("sessao") instanceof SessaoUsuario autenticada) return autenticada;
        return buscarSessao(extrairToken(request)).orElseThrow(SessaoExpiradaException::new);
    }

    private String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo de hash indisponível.", ex);
        }
    }
}
