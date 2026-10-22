package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.AuthDtos.*;
import br.com.fatec.agendabanda.model.RedefinicaoSenha;
import br.com.fatec.agendabanda.repository.MembroRepository;
import br.com.fatec.agendabanda.repository.RedefinicaoSenhaRepository;
import br.com.fatec.agendabanda.security.SessaoUsuario;
import br.com.fatec.agendabanda.security.TokenService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
public class AuthService {
    private final MembroRepository membroRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final MapperService mapperService;
    private final RedefinicaoSenhaRepository redefinicaoSenhaRepository;
    private final MembroService membroService;

    public AuthService(MembroRepository membroRepository, BCryptPasswordEncoder passwordEncoder, TokenService tokenService, MapperService mapperService,
                       RedefinicaoSenhaRepository redefinicaoSenhaRepository, MembroService membroService) {
        this.membroRepository = membroRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mapperService = mapperService;
        this.redefinicaoSenhaRepository = redefinicaoSenhaRepository;
        this.membroService = membroService;
    }

    public LoginResponse login(LoginRequest request) {
        var membro = membroRepository.findByLogin(request.login())
                .orElseThrow(() -> new IllegalArgumentException("Login ou senha inválidos."));

        if (!Boolean.TRUE.equals(membro.getAtivo())) {
            throw new IllegalArgumentException("Usuário desativado.");
        }
        if (!passwordEncoder.matches(request.senha(), membro.getSenha())) {
            throw new IllegalArgumentException("Login ou senha inválidos.");
        }

        var sessao = new SessaoUsuario(membro.getCodigo(), membro.getNome(), membro.getLogin(), Boolean.TRUE.equals(membro.getAdministrador()));
        String token = tokenService.criarToken(sessao);
        return new LoginResponse(token, mapperService.membroDto(membro));
    }

    @Transactional
    public RecuperarSenhaResponse solicitarRedefinicaoSenha(RecuperarSenhaRequest request) {
        var membro = buscarPorIdentificador(request.identificador());
        if (membro == null || !Boolean.TRUE.equals(membro.getAtivo())) {
            return new RecuperarSenhaResponse(false, null, "Se os dados estiverem corretos, a solicitação será analisada por um administrador.");
        }

        var ultima = redefinicaoSenhaRepository.findTopByMembroCodigoOrderBySolicitadoEmDesc(membro.getCodigo()).orElse(null);
        if (ultima != null && "APROVADA".equals(ultima.getStatus()) && chaveConfere(ultima, request.chaveSolicitacao())) {
            return new RecuperarSenhaResponse(true, ultima.getCodigoRedefinicao(), "Solicitação aprovada. Defina sua nova senha.");
        }
        if (ultima != null && "PENDENTE".equals(ultima.getStatus())) {
            return new RecuperarSenhaResponse(false, null, "Sua solicitação está aguardando a aprovação de um administrador.");
        }

        var redefinicao = new RedefinicaoSenha();
        redefinicao.setMembro(membro);
        redefinicao.setStatus("PENDENTE");
        redefinicao.setChaveHash(hash(request.chaveSolicitacao()));
        redefinicao.setSolicitadoEm(LocalDateTime.now());
        redefinicaoSenhaRepository.save(redefinicao);
        return new RecuperarSenhaResponse(false, null, "Solicitação enviada para aprovação de um administrador.");
    }

    @Transactional
    public void redefinirSenha(RedefinirSenhaRequest request) {
        var redefinicao = redefinicaoSenhaRepository.findByCodigoRedefinicaoAndStatus(request.codigoSolicitacao(), "APROVADA")
                .orElseThrow(() -> new IllegalArgumentException("A solicitação não está aprovada ou já foi utilizada."));
        if (!chaveConfere(redefinicao, request.chaveSolicitacao())) {
            throw new IllegalArgumentException("Solicitação de redefinição inválida.");
        }
        membroService.redefinirSenha(redefinicao.getMembro(), request.novaSenha());
        redefinicao.setStatus("CONCLUIDA");
        redefinicao.setConcluidoEm(LocalDateTime.now());
        redefinicaoSenhaRepository.save(redefinicao);
        tokenService.removerSessoesDoMembro(redefinicao.getMembro().getCodigo());
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoRedefinicaoSenhaDto> listarSolicitacoesPendentes() {
        return redefinicaoSenhaRepository.findByStatusOrderBySolicitadoEmAsc("PENDENTE").stream()
                .map(item -> new SolicitacaoRedefinicaoSenhaDto(item.getCodigoRedefinicao(), item.getMembro().getCodigo(),
                        item.getMembro().getNome(), item.getMembro().getLogin(), item.getMembro().getEmail(), item.getSolicitadoEm()))
                .toList();
    }

    @Transactional
    public void decidirSolicitacao(Integer codigoSolicitacao, boolean aprovada, Integer codigoAdministrador) {
        var solicitacao = redefinicaoSenhaRepository.findByCodigoRedefinicaoAndStatus(codigoSolicitacao, "PENDENTE")
                .orElseThrow(() -> new IllegalArgumentException("Solicitação pendente não encontrada."));
        solicitacao.setStatus(aprovada ? "APROVADA" : "RECUSADA");
        solicitacao.setAdministrador(membroRepository.findById(codigoAdministrador)
                .orElseThrow(() -> new IllegalArgumentException("Administrador não encontrado.")));
        solicitacao.setDecididoEm(LocalDateTime.now());
        redefinicaoSenhaRepository.save(solicitacao);
    }

    private br.com.fatec.agendabanda.model.Membro buscarPorIdentificador(String identificador) {
        String valor = identificador.trim();
        return membroRepository.findByLogin(valor)
                .or(() -> membroRepository.findByEmailIgnoreCase(valor))
                .orElse(null);
    }

    private boolean chaveConfere(RedefinicaoSenha redefinicao, String chave) {
        return redefinicao.getChaveHash().equals(hash(chave));
    }

    private String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo de hash indisponível.", ex);
        }
    }
}
