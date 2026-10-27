package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.AuthDtos.*;
import br.com.fatec.agendabanda.model.RedefinicaoSenha;
import br.com.fatec.agendabanda.repository.MembroRepository;
import br.com.fatec.agendabanda.repository.RedefinicaoSenhaRepository;
import br.com.fatec.agendabanda.security.SessaoUsuario;
import br.com.fatec.agendabanda.security.TokenService;
import br.com.fatec.agendabanda.security.LimitadorAutenticacao;
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
import java.util.Base64;
import java.security.SecureRandom;

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
    private final LimitadorAutenticacao limitador;

    public AuthService(MembroRepository membroRepository, BCryptPasswordEncoder passwordEncoder, TokenService tokenService, MapperService mapperService,
                       RedefinicaoSenhaRepository redefinicaoSenhaRepository, MembroService membroService,
                       CicloRecuperacaoService ciclo, Clock clock, LimitadorAutenticacao limitador) {
        this.membroRepository = membroRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mapperService = mapperService;
        this.redefinicaoSenhaRepository = redefinicaoSenhaRepository;
        this.membroService = membroService;
        this.ciclo = ciclo;
        this.clock = clock;
        this.limitador = limitador;
        this.hashFicticio = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String enderecoIp) {
        var codigo = membroRepository.buscarCodigoPorLogin(request.login().trim()).orElse(null);
        String alvo = codigo == null ? "login:" + request.login().trim() : "membro:" + codigo;
        limitador.verificarLogin(alvo, enderecoIp);
        var membro = codigo == null ? null : membroRepository.bloquearPorCodigo(codigo).orElse(null);
        boolean senhaConfere = passwordEncoder.matches(request.senha(), membro == null ? hashFicticio : membro.getSenha());
        if (!senhaConfere || membro == null || !Boolean.TRUE.equals(membro.getAtivo())
                || !"APROVADO".equals(membro.getStatusCadastro())) {
            limitador.registrarFalhaLogin(alvo, enderecoIp);
            throw new CredenciaisInvalidasException();
        }

        var sessao = new SessaoUsuario(membro.getCodigo(), membro.getNome(), membro.getLogin(), Boolean.TRUE.equals(membro.getAdministrador()));
        String token = tokenService.criarToken(sessao);
        limitador.limparFalhasLogin(alvo, enderecoIp);
        return new LoginResponse(token, mapperService.membroDto(membro));
    }

    @Transactional
    public RecuperarSenhaResponse solicitarRedefinicaoSenha(RecuperarSenhaRequest request) {
        var codigoMembro = membroRepository.buscarCodigoPorIdentificador(request.identificador().trim()).orElse(null);
        limitador.verificarIdentificador(LimitadorAutenticacao.Tipo.RECUPERACAO,
                codigoMembro == null ? "identificador:" + request.identificador().trim() : "membro:" + codigoMembro);
        if (codigoMembro == null) return respostaPublica();
        var membro = membroRepository.bloquearPorCodigo(codigoMembro).orElse(null);
        if (membro == null || !Boolean.TRUE.equals(membro.getAtivo()) || !"APROVADO".equals(membro.getStatusCadastro())) return respostaPublica();
        // A abertura nunca entrega ou valida uma credencial. A aprovacao gera outra chave.
        if (ciclo.abertaValida(membro.getCodigo()).isPresent()) return respostaPublica();

        var redefinicao = new RedefinicaoSenha();
        redefinicao.setMembro(membro);
        redefinicao.setStatus("PENDENTE");
        redefinicao.setChaveHash(hash(gerarCredencial()));
        redefinicao.setSolicitadoEm(LocalDateTime.now(clock));
        redefinicao.setExpiraEm(ciclo.prazoPendente());
        redefinicaoSenhaRepository.save(redefinicao);
        return respostaPublica();
    }

    @Transactional(noRollbackFor = RecuperacaoExpiradaException.class)
    public void redefinirSenha(RedefinirSenhaRequest request) {
        var alvo = redefinicaoSenhaRepository.buscarCodigoMembro(request.codigoSolicitacao()).orElse(null);
        limitador.verificarIdentificador(LimitadorAutenticacao.Tipo.RECUPERACAO,
                alvo == null ? "solicitacao:" + request.codigoSolicitacao() : "membro:" + alvo);
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
    public DecisaoRecuperacaoResponse decidirSolicitacao(Integer codigoSolicitacao, boolean aprovada, Integer codigoAdministrador, boolean identidadeVerificada) {
        if (aprovada && !identidadeVerificada) throw new IllegalArgumentException("Confirme a identidade do membro por um canal conhecido antes de aprovar.");
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
        String credencial = null;
        if (aprovada) {
            credencial = gerarCredencial();
            solicitacao.setChaveHash(hash(credencial));
            solicitacao.setExpiraEm(ciclo.prazoAprovado());
        }
        else ciclo.encerrar(solicitacao, "RECUSADA", "Pedido recusado pelo administrador.");
        redefinicaoSenhaRepository.save(solicitacao);
        return new DecisaoRecuperacaoResponse(aprovada ? "Entregue o codigo somente ao membro cuja identidade foi verificada." : "Solicitacao recusada.",
                credencial == null ? null : codigoSolicitacao + "." + credencial, aprovada ? solicitacao.getExpiraEm() : null);
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

    private RecuperarSenhaResponse respostaPublica() {
        return new RecuperarSenhaResponse(false, null,
                "Se os dados estiverem corretos, um administrador analisara o pedido. Contate um administrador por um canal conhecido para confirmar sua identidade e receber o codigo de redefinicao.", null);
    }

    private String gerarCredencial() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
