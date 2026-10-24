package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.AuthDtos.*;
import br.com.fatec.agendabanda.model.RedefinicaoSenha;
import br.com.fatec.agendabanda.repository.MembroRepository;
import br.com.fatec.agendabanda.repository.RedefinicaoSenhaRepository;
import br.com.fatec.agendabanda.security.SessaoUsuario;
import br.com.fatec.agendabanda.security.TokenService;
import br.com.fatec.agendabanda.exception.CredenciaisInvalidasException;
import br.com.fatec.agendabanda.exception.RecuperacaoExpiradaException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
    private final MembroRepository membroRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final MapperService mapperService;
    private final RedefinicaoSenhaRepository redefinicaoSenhaRepository;
    private final MembroService membroService;
    private final CicloRecuperacaoService ciclo;
    private final Clock clock;
    private final String hashFicticio;

    public AuthService(MembroRepository membroRepository, BCryptPasswordEncoder passwordEncoder, TokenService tokenService, MapperService mapperService,
                       RedefinicaoSenhaRepository redefinicaoSenhaRepository, MembroService membroService,
                       CicloRecuperacaoService ciclo, Clock clock) {
        this.membroRepository = membroRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mapperService = mapperService;
        this.redefinicaoSenhaRepository = redefinicaoSenhaRepository;
        this.membroService = membroService;
        this.ciclo = ciclo;
        this.clock = clock;
        this.hashFicticio = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        var codigo = membroRepository.buscarCodigoPorLogin(request.login().trim()).orElse(null);
        var membro = codigo == null ? null : membroRepository.bloquearPorCodigo(codigo).orElse(null);
        boolean senhaConfere = passwordEncoder.matches(request.senha(), membro == null ? hashFicticio : membro.getSenha());
        if (!senhaConfere || membro == null || !Boolean.TRUE.equals(membro.getAtivo())
                || !"APROVADO".equals(membro.getStatusCadastro())) {
            throw new CredenciaisInvalidasException();
        }

        var sessao = new SessaoUsuario(membro.getCodigo(), membro.getNome(), membro.getLogin(), Boolean.TRUE.equals(membro.getAdministrador()));
        String token = tokenService.criarToken(sessao);
        return new LoginResponse(token, mapperService.membroDto(membro));
    }

    @Transactional
    public RecuperarSenhaResponse solicitarRedefinicaoSenha(RecuperarSenhaRequest request) {
        var codigoMembro = membroRepository.buscarCodigoPorIdentificador(request.identificador().trim()).orElse(null);
        if (codigoMembro == null) {
            return respostaPublica(request.chaveSolicitacao());
        }
        // Serializa pedidos do mesmo membro; a restrição única no SQL protege também a persistência.
        var membro = membroRepository.bloquearPorCodigo(codigoMembro).orElse(null);
        if (membro == null || !Boolean.TRUE.equals(membro.getAtivo()) || !"APROVADO".equals(membro.getStatusCadastro())) return respostaPublica(request.chaveSolicitacao());

        var ultima = ciclo.abertaValida(membro.getCodigo()).orElse(null);
        if (ultima != null && ("APROVADA".equals(ultima.getStatus()) || "PENDENTE".equals(ultima.getStatus()))) {
            if (!chaveConfere(ultima, request.chaveSolicitacao())) {
                return respostaPublica(request.chaveSolicitacao());
            }
            if ("APROVADA".equals(ultima.getStatus())) {
                return new RecuperarSenhaResponse(true, ultima.getCodigoRedefinicao(), "Solicitação aprovada. Defina sua nova senha.", request.chaveSolicitacao());
            }
            return respostaPublica(request.chaveSolicitacao());
        }

        var redefinicao = new RedefinicaoSenha();
        redefinicao.setMembro(membro);
        redefinicao.setStatus("PENDENTE");
        redefinicao.setChaveHash(hash(request.chaveSolicitacao()));
        redefinicao.setSolicitadoEm(LocalDateTime.now(clock));
        redefinicao.setExpiraEm(ciclo.prazoPendente());
        redefinicaoSenhaRepository.save(redefinicao);
        return respostaPublica(request.chaveSolicitacao());
    }

    @Transactional(noRollbackFor = RecuperacaoExpiradaException.class)
    public void redefinirSenha(RedefinirSenhaRequest request) {
        bloquearMembroDaSolicitacao(request.codigoSolicitacao());
        var redefinicao = redefinicaoSenhaRepository.findByCodigoRedefinicaoAndStatus(request.codigoSolicitacao(), "APROVADA")
                .orElseThrow(() -> new IllegalArgumentException("A solicitação não está aprovada ou já foi utilizada."));
        if (ciclo.expirarSeNecessario(redefinicao)) throw new RecuperacaoExpiradaException();
        if (!chaveConfere(redefinicao, request.chaveSolicitacao())) {
            throw new IllegalArgumentException("Solicitação de redefinição inválida.");
        }
        if (!Boolean.TRUE.equals(redefinicao.getMembro().getAtivo()) || !"APROVADO".equals(redefinicao.getMembro().getStatusCadastro())) {
            throw new IllegalArgumentException("Solicitação de redefinição inválida.");
        }
        ciclo.encerrar(redefinicao, "CONCLUIDA", "Senha redefinida pela solicitação aprovada.");
        redefinicao.setConcluidoEm(LocalDateTime.now(clock));
        redefinicaoSenhaRepository.saveAndFlush(redefinicao);
        membroService.redefinirSenha(redefinicao.getMembro(), request.novaSenha());
    }

    public List<SolicitacaoRedefinicaoSenhaDto> listarSolicitacoesAbertas() {
        for (Integer codigo : redefinicaoSenhaRepository.buscarMembrosComPedidosVencidos(ciclo.agora())) ciclo.abertaValida(codigo);
        return redefinicaoSenhaRepository.findByStatusInOrderBySolicitadoEmAsc(CicloRecuperacaoService.ABERTAS).stream()
                .map(item -> new SolicitacaoRedefinicaoSenhaDto(item.getCodigoRedefinicao(), item.getMembro().getCodigo(),
                        item.getMembro().getNome(), item.getMembro().getLogin(), item.getMembro().getEmail(), item.getSolicitadoEm(),
                        item.getStatus(), item.getExpiraEm()))
                .toList();
    }

    @Transactional(noRollbackFor = RecuperacaoExpiradaException.class)
    public void decidirSolicitacao(Integer codigoSolicitacao, boolean aprovada, Integer codigoAdministrador) {
        exigirAdministradorAtivo(codigoAdministrador);
        bloquearMembroDaSolicitacao(codigoSolicitacao);
        var solicitacao = redefinicaoSenhaRepository.findByCodigoRedefinicaoAndStatus(codigoSolicitacao, "PENDENTE")
                .orElseThrow(() -> new IllegalArgumentException("Solicitação pendente não encontrada."));
        if (ciclo.expirarSeNecessario(solicitacao)) throw new RecuperacaoExpiradaException();
        if (!Boolean.TRUE.equals(solicitacao.getMembro().getAtivo())) throw new IllegalArgumentException("O membro não está ativo.");
        solicitacao.setStatus(aprovada ? "APROVADA" : "RECUSADA");
        solicitacao.setAdministrador(membroRepository.findById(codigoAdministrador)
                .orElseThrow(() -> new IllegalArgumentException("Administrador não encontrado.")));
        solicitacao.setDecididoEm(LocalDateTime.now(clock));
        if (aprovada) solicitacao.setExpiraEm(ciclo.prazoAprovado());
        else ciclo.encerrar(solicitacao, "RECUSADA", "Pedido recusado pelo administrador.");
        redefinicaoSenhaRepository.save(solicitacao);
    }

    @Transactional(noRollbackFor = RecuperacaoExpiradaException.class)
    public void cancelarSolicitacao(Integer codigoSolicitacao, Integer codigoAdministrador) {
        exigirAdministradorAtivo(codigoAdministrador);
        bloquearMembroDaSolicitacao(codigoSolicitacao);
        var pedido = redefinicaoSenhaRepository.findByCodigoRedefinicaoAndStatusIn(codigoSolicitacao, CicloRecuperacaoService.ABERTAS)
                .orElseThrow(() -> new IllegalArgumentException("A solicitação já está encerrada."));
        if (ciclo.expirarSeNecessario(pedido)) throw new RecuperacaoExpiradaException();
        ciclo.encerrar(pedido, "CANCELADA", "Cancelada pelo administrador de código " + codigoAdministrador + ".");
        redefinicaoSenhaRepository.saveAndFlush(pedido);
    }

    private void exigirAdministradorAtivo(Integer codigo) {
        if (membroRepository.bloquearAdministradoresAtivos().stream().noneMatch(m -> m.getCodigo().equals(codigo))) {
            throw new SecurityException("Apenas administradores ativos podem executar esta ação.");
        }
    }

    private RecuperarSenhaResponse respostaPublica(String chave) {
        // O código é devolvido mesmo sem uma conta correspondente: ele não comprova sua existência.
        return new RecuperarSenhaResponse(false, null,
                "Se os dados estiverem corretos, a solicitação será analisada por um administrador. "
                        + "Se já houver uma solicitação aberta, use o navegador ou o código original.", chave);
    }

    private void bloquearMembroDaSolicitacao(Integer codigoSolicitacao) {
        var codigoMembro = redefinicaoSenhaRepository.buscarCodigoMembro(codigoSolicitacao)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação inválida."));
        membroRepository.bloquearPorCodigo(codigoMembro)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação inválida."));
    }

    private boolean chaveConfere(RedefinicaoSenha redefinicao, String chave) {
        return chave != null && MessageDigest.isEqual(redefinicao.getChaveHash().getBytes(StandardCharsets.UTF_8), hash(chave).getBytes(StandardCharsets.UTF_8));
    }

    private String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo de hash indisponível.", ex);
        }
    }
}
