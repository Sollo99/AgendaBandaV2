package br.com.fatec.agendabanda.security;

import br.com.fatec.agendabanda.model.SessaoPersistida;
import br.com.fatec.agendabanda.repository.MembroRepository;
import br.com.fatec.agendabanda.repository.SessaoPersistidaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public TokenService(SessaoPersistidaRepository sessaoRepository, MembroRepository membroRepository) {
        this.sessaoRepository = sessaoRepository;
        this.membroRepository = membroRepository;
    }

    @Transactional
    public String criarToken(SessaoUsuario usuario) {
        String token = UUID.randomUUID().toString();
        var sessao = new SessaoPersistida();
        sessao.setTokenHash(hash(token));
        sessao.setCodigoMembro(usuario.codigo());
        sessao.setCriadoEm(LocalDateTime.now());
        sessao.setExpiraEm(LocalDateTime.now().plusHours(DURACAO_HORAS));
        sessaoRepository.save(sessao);
        return token;
    }

    @Transactional
    public Optional<SessaoUsuario> buscarSessao(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return sessaoRepository.findByTokenHashAndExpiraEmAfter(hash(token), LocalDateTime.now())
                .flatMap(item -> membroRepository.findById(item.getCodigoMembro())
                        .filter(membro -> Boolean.TRUE.equals(membro.getAtivo()))
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

    public String extrairToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        return auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : request.getHeader("X-Auth-Token");
    }

    public SessaoUsuario sessaoObrigatoria(HttpServletRequest request) {
        return buscarSessao(extrairToken(request)).orElseThrow(() -> new IllegalArgumentException("Sessão inválida ou expirada."));
    }

    private String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo de hash indisponível.", ex);
        }
    }
}
