package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.exception.RegraNegocioException;

import br.com.fatec.agendabanda.dto.AuthDtos.*;
import br.com.fatec.agendabanda.model.RedefinicaoSenha;
import br.com.fatec.agendabanda.repository.MembroRepository;
import br.com.fatec.agendabanda.repository.RedefinicaoSenhaRepository;
import br.com.fatec.agendabanda.security.SessaoUsuario;
import br.com.fatec.agendabanda.security.TokenService;
import br.com.fatec.agendabanda.security.LimitadorAutenticacao;
import br.com.fatec.agendabanda.exception.CredenciaisInvalidasException;
import br.com.fatec.agendabanda.exception.RecuperacaoExpiradaException;
import br.com.fatec.agendabanda.exception.RedefinicaoInvalidaException;
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
    private final SenhaMembroService senhaMembroService;
    private final CicloRecuperacaoService ciclo;
    private final Clock clock;
    private final String hashFicticio;
    private final LimitadorAutenticacao limitador;
    private final AuditoriaContexto auditoria;
    private final AuditoriaAutenticacaoService auditoriaAutenticacao;

    public AuthService(MembroRepository membroRepository, BCryptPasswordEncoder passwordEncoder, TokenService tokenService, MapperService mapperService,
                       RedefinicaoSenhaRepository redefinicaoSenhaRepository, SenhaMembroService senhaMembroService,
                       CicloRecuperacaoService ciclo, Clock clock, LimitadorAutenticacao limitador, AuditoriaContexto auditoria,
                       AuditoriaAutenticacaoService auditoriaAutenticacao) {
        this.membroRepository = membroRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mapperService = mapperService;
        this.redefinicaoSenhaRepository = redefinicaoSenhaRepository;
        this.senhaMembroService = senhaMembroService;
        this.ciclo = ciclo;
        this.clock = clock;
        this.limitador = limitador;
        this.auditoria = auditoria;
        this.auditoriaAutenticacao = auditoriaAutenticacao;
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
            auditoriaAutenticacao.falhaLogin(codigo);
            limitador.registrarFalhaLogin(alvo, enderecoIp);
            throw new CredenciaisInvalidasException();
        }

        var sessao = new SessaoUsuario(membro.getCodigo(), membro.getNome(), membro.getLogin(), Boolean.TRUE.equals(membro.getAdministrador()));
        String token = tokenService.criarToken(sessao);
        auditoriaAutenticacao.login(membro.getCodigo());
        limitador.limparFalhasLogin(alvo, enderecoIp);
        return new LoginResponse(token, mapperService.membroDto(membro));
    }

    @Transactional
    public void logout(String token, SessaoUsuario sessao) {
        tokenService.removerToken(token);
        auditoriaAutenticacao.logout(sessao.codigo());
    }

    @Transactional
    public RecuperarSenhaResponse solicitarRedefinicaoSenha(RecuperarSenhaRequest request, String enderecoIp) {
        var codigoMembro = membroRepository.buscarCodigoPorIdentificador(request.identificador().trim()).orElse(null);
        limitador.verificarIdentificador(LimitadorAutenticacao.Tipo.RECUPERACAO,
                codigoMembro == null ? "identificador:" + request.identificador().trim() : "membro:" + codigoMembro, enderecoIp);
        if (codigoMembro == null) return respostaPublica();
        // Informar um identificador nao comprova identidade: abertura/expiracao publica ficam anonimas.
        auditoria.preparar(null);
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

    @Transactional(noRollbackFor = RedefinicaoInvalidaException.class)
    public void redefinirSenha(RedefinirSenhaRequest request, String enderecoIp) {
        var alvo = redefinicaoSenhaRepository.buscarCodigoMembro(request.codigoSolicitacao()).orElse(null);
        limitador.verificarIdentificador(LimitadorAutenticacao.Tipo.RECUPERACAO,
                alvo == null ? "solicitacao:" + request.codigoSolicitacao() : "membro:" + alvo, enderecoIp);
        if (alvo == null) throw new RedefinicaoInvalidaException();
        membroRepository.bloquearPorCodigo(alvo).orElseThrow(RedefinicaoInvalidaException::new);
        var redefinicao = redefinicaoSenhaRepository.findByCodigoRedefinicaoAndStatus(request.codigoSolicitacao(), "APROVADA")
                .orElseThrow(RedefinicaoInvalidaException::new);
        if (!chaveConfere(redefinicao, request.chaveSolicitacao())) {
            throw new RedefinicaoInvalidaException();
        }
        auditoria.preparar(alvo);
        // Persiste a expiracao, mas nao a distingue de qualquer outro codigo indisponivel.
        if (ciclo.expirarSeNecessario(redefinicao)) throw new RedefinicaoInvalidaException();
        if (!Boolean.TRUE.equals(redefinicao.getMembro().getAtivo()) || !"APROVADO".equals(redefinicao.getMembro().getStatusCadastro())) {
            throw new RedefinicaoInvalidaException();
        }
        ciclo.encerrar(redefinicao, "CONCLUIDA", "Senha redefinida pela solicitação aprovada.");
        redefinicao.setConcluidoEm(LocalDateTime.now(clock));
        redefinicaoSenhaRepository.saveAndFlush(redefinicao);
        senhaMembroService.redefinirSenha(redefinicao.getMembro(), request.novaSenha());
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
        if (aprovada && !identidadeVerificada) throw new RegraNegocioException("Confirme a identidade do membro por um canal conhecido antes de aprovar.");
        auditoria.preparar(codigoAdministrador);
        exigirAdministradorAtivo(codigoAdministrador);
        bloquearMembroDaSolicitacao(codigoSolicitacao);
        var solicitacao = redefinicaoSenhaRepository.findByCodigoRedefinicaoAndStatus(codigoSolicitacao, "PENDENTE")
                .orElseThrow(() -> new RegraNegocioException("Solicitação pendente não encontrada."));
        if (ciclo.expirarSeNecessario(solicitacao)) throw new RecuperacaoExpiradaException();
        if (!Boolean.TRUE.equals(solicitacao.getMembro().getAtivo())) throw new RegraNegocioException("O membro não está ativo.");
        solicitacao.setStatus(aprovada ? "APROVADA" : "RECUSADA");
        solicitacao.setAdministrador(membroRepository.findById(codigoAdministrador)
                .orElseThrow(() -> new RegraNegocioException("Administrador não encontrado.")));
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
        auditoria.preparar(codigoAdministrador);
        exigirAdministradorAtivo(codigoAdministrador);
        bloquearMembroDaSolicitacao(codigoSolicitacao);
        var pedido = redefinicaoSenhaRepository.findByCodigoRedefinicaoAndStatusIn(codigoSolicitacao, CicloRecuperacaoService.ABERTAS)
                .orElseThrow(() -> new RegraNegocioException("A solicitação já está encerrada."));
        if (ciclo.expirarSeNecessario(pedido)) throw new RecuperacaoExpiradaException();
        ciclo.cancelarSolicitacao(pedido, "Cancelada pelo administrador de código " + codigoAdministrador + ".");
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
                .orElseThrow(() -> new RegraNegocioException("Solicitação inválida."));
        membroRepository.bloquearPorCodigo(codigoMembro)
                .orElseThrow(() -> new RegraNegocioException("Solicitação inválida."));
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
