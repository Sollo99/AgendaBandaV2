package br.com.fatec.agendabanda.security;

import br.com.fatec.agendabanda.model.SessaoPersistida;
import br.com.fatec.agendabanda.repository.MembroRepository;
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
    private final MembroRepository membroRepository;
    private final Clock clock;

    public TokenService(SessaoPersistidaRepository sessaoRepository, MembroRepository membroRepository, Clock clock) {
        this.sessaoRepository = sessaoRepository;
        this.membroRepository = membroRepository;
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
        return sessaoRepository.findByTokenHashAndExpiraEmAfter(hash(token), LocalDateTime.now(clock))
                .flatMap(item -> membroRepository.findById(item.getCodigoMembro())
                        .filter(membro -> Boolean.TRUE.equals(membro.getAtivo()) && "APROVADO".equals(membro.getStatusCadastro()))
                        .map(membro -> new SessaoUsuario(membro.getCodigo(), membro.getNome(), membro.getLogin(), Boolean.TRUE.equals(membro.getAdministrador()))));
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
        return sessaoRepository.excluirVencidas(LocalDateTime.now(clock));
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
