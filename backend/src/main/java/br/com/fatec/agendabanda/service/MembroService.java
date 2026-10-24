package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.*;
import br.com.fatec.agendabanda.model.Membro;
import br.com.fatec.agendabanda.model.Musico;
import br.com.fatec.agendabanda.model.Produtor;
import br.com.fatec.agendabanda.repository.MembroRepository;
import br.com.fatec.agendabanda.repository.MusicoRepository;
import br.com.fatec.agendabanda.repository.ProdutorRepository;
import br.com.fatec.agendabanda.repository.ParticipacaoEventoRepository;
import br.com.fatec.agendabanda.security.TokenService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;
import java.util.Locale;

@Service
public class MembroService {
    private final MembroRepository membroRepository;
    private final MusicoRepository musicoRepository;
    private final ProdutorRepository produtorRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MapperService mapperService;
    private final ParticipacaoEventoRepository participacaoRepository;
    private final CicloRecuperacaoService cicloRecuperacao;
    private final TokenService tokenService;
    private final Clock clock;

    public MembroService(MembroRepository membroRepository, MusicoRepository musicoRepository, ProdutorRepository produtorRepository,
                         BCryptPasswordEncoder passwordEncoder, MapperService mapperService,
                         ParticipacaoEventoRepository participacaoRepository, CicloRecuperacaoService cicloRecuperacao,
                         TokenService tokenService, Clock clock) {
        this.membroRepository = membroRepository;
        this.musicoRepository = musicoRepository;
        this.produtorRepository = produtorRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapperService = mapperService;
        this.participacaoRepository = participacaoRepository;
        this.cicloRecuperacao = cicloRecuperacao;
        this.tokenService = tokenService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MembroDto> listar() {
        return membroRepository.findAll().stream().map(mapperService::membroDto).toList();
    }

    @Transactional(readOnly = true)
    public List<MusicoResumoDto> listarMusicos() {
        return musicoRepository.findByMembroAtivoTrueOrderByMembroNomeAsc().stream()
                .map(musico -> new MusicoResumoDto(musico.getCodigo(), musico.getMembro().getNome(), musico.getInstrumento()))
                .toList();
    }

    public Membro buscarEntidade(Integer id) {
        return membroRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Membro não encontrado."));
    }

    @Transactional
    public MembroDto criar(MembroRequest request) {
        if (request.senha() == null || request.senha().isBlank()) throw new IllegalArgumentException("Senha é obrigatória no cadastro.");

        var membro = new Membro();
        validarUnicidade(request.cpf(), request.email(), request.login(), null);
        aplicarDados(membro, request);
        membro.setSenha(passwordEncoder.encode(request.senha()));
        membro.setCriadoEm(LocalDateTime.now(clock));
        var salvo = membroRepository.save(membro);
        salvarPerfil(salvo, request);
        return mapperService.membroDto(salvo);
    }

    @Transactional
    public MembroDto atualizar(Integer id, MembroRequest request) {
        var administradores = membroRepository.bloquearAdministradoresAtivos();
        var membro = buscarParaAtualizar(id);
        protegerUltimoAdministrador(membro, Boolean.TRUE.equals(request.administrador()),
                request.ativo() == null || request.ativo(), administradores);
        if (!"APROVADO".equals(membro.getStatusCadastro())
                && (Boolean.TRUE.equals(request.administrador()) || request.ativo() == null || request.ativo())) {
            throw new IllegalArgumentException("Analise o cadastro antes de ativar a conta ou conceder administração.");
        }
        validarUnicidade(request.cpf(), request.email(), request.login(), id);
        aplicarDados(membro, request);
        if (request.senha() != null && !request.senha().isBlank()) {
            aplicarNovaSenha(membro, request.senha());
        }
        var salvo = membroRepository.save(membro);
        salvarPerfil(salvo, request);
        return mapperService.membroDto(salvo);
    }

    @Transactional
    public MembroDto alterarStatus(Integer id, boolean ativo) {
        var administradores = membroRepository.bloquearAdministradoresAtivos();
        var membro = buscarParaAtualizar(id);
        if (ativo && !"APROVADO".equals(membro.getStatusCadastro())) {
            throw new IllegalArgumentException("O cadastro precisa ser aprovado antes da ativação.");
        }
        protegerUltimoAdministrador(membro, Boolean.TRUE.equals(membro.getAdministrador()), ativo, administradores);
        membro.setAtivo(ativo);
        return mapperService.membroDto(membroRepository.save(membro));
    }

    @Transactional
    public void criarCadastro(CadastroRequest request) {
        String hashSenha = passwordEncoder.encode(request.senha());
        // Consulta os três campos mesmo se um deles já existir, sem indicar qual colidiu.
        boolean duplicado = membroRepository.findByLogin(request.login().trim()).isPresent()
                | membroRepository.findByEmailIgnoreCase(request.email().trim()).isPresent()
                | membroRepository.findByCpf(normalizarCpf(request.cpf())).isPresent();
        if (duplicado) return;
        var membro = new Membro();
        membro.setNome(request.nome().trim());
        membro.setCpf(normalizarCpf(request.cpf()));
        membro.setEmail(request.email().trim());
        membro.setLogin(request.login().trim());
        membro.setSenha(hashSenha);
        membro.setAdministrador(false);
        membro.setAtivo(false);
        membro.setStatusCadastro("PENDENTE");
        membro.setPerfil("MUSICO");
        membro.setCriadoEm(LocalDateTime.now(clock));
        var salvo = membroRepository.save(membro);
        var musico = new Musico();
        musico.setMembro(salvo);
        musico.setInstrumento(textoOuPadrao(request.instrumento()));
        musico.setBiografia(request.biografia());
        musicoRepository.save(musico);
    }

    @Transactional
    public MembroDto decidirCadastro(Integer id, boolean aprovada, Integer codigoAdministrador) {
        if (membroRepository.bloquearAdministradoresAtivos().stream().noneMatch(m -> m.getCodigo().equals(codigoAdministrador))) {
            throw new SecurityException("Apenas administradores ativos podem analisar cadastros.");
        }
        var membro = buscarParaAtualizar(id);
        if ("APROVADO".equals(membro.getStatusCadastro()) || (!aprovada && "RECUSADO".equals(membro.getStatusCadastro()))) {
            throw new IllegalArgumentException("O cadastro já foi analisado.");
        }
        membro.setStatusCadastro(aprovada ? "APROVADO" : "RECUSADO");
        membro.setAtivo(aprovada);
        membro.setAdministrador(false);
        membro.setCadastroDecididoEm(LocalDateTime.now(clock));
        membro.setCadastroDecididoPor(codigoAdministrador);
        return mapperService.membroDto(membroRepository.save(membro));
    }

    @Transactional(readOnly = true)
    public MembroDto obterProprio(Integer codigoMembro) {
        return mapperService.membroDto(buscarEntidade(codigoMembro));
    }

    @Transactional
    public MembroDto atualizarProprio(Integer codigoMembro, PerfilRequest request) {
        var membro = buscarParaAtualizar(codigoMembro);
        validarUnicidade(request.cpf(), request.email(), request.login(), codigoMembro);
        membro.setNome(request.nome().trim());
        membro.setCpf(normalizarCpf(request.cpf()));
        membro.setEmail(request.email().trim());
        membro.setLogin(request.login().trim());
        if (request.novaSenha() != null && !request.novaSenha().isBlank()) {
            if (request.senhaAtual() == null || !passwordEncoder.matches(request.senhaAtual(), membro.getSenha())) {
                throw new IllegalArgumentException("A senha atual informada é inválida.");
            }
            aplicarNovaSenha(membro, request.novaSenha());
        }
        var salvo = membroRepository.save(membro);
        musicoRepository.findById(codigoMembro).ifPresent(musico -> {
            if (request.instrumento() != null) musico.setInstrumento(request.instrumento().trim());
            if (request.biografia() != null) musico.setBiografia(request.biografia().trim());
            musicoRepository.save(musico);
        });
        return mapperService.membroDto(salvo);
    }

    @Transactional
    public void redefinirSenha(Membro membro, String novaSenha) {
        buscarParaAtualizar(membro.getCodigo());
        aplicarNovaSenha(membro, novaSenha);
        membroRepository.save(membro);
    }

    private Membro buscarParaAtualizar(Integer id) {
        return membroRepository.bloquearPorCodigo(id).orElseThrow(() -> new IllegalArgumentException("Membro não encontrado."));
    }

    private void aplicarNovaSenha(Membro membro, String novaSenha) {
        membro.setSenha(passwordEncoder.encode(novaSenha));
        cicloRecuperacao.cancelarAbertas(membro.getCodigo(), "Senha alterada por outro caminho.");
        tokenService.removerSessoesDoMembro(membro.getCodigo());
    }

    private void aplicarDados(Membro membro, MembroRequest request) {
        membro.setNome(request.nome().trim());
        membro.setCpf(normalizarCpf(request.cpf()));
        membro.setEmail(request.email().trim());
        membro.setLogin(request.login().trim());
        membro.setAdministrador(Boolean.TRUE.equals(request.administrador()));
        membro.setAtivo(request.ativo() == null || request.ativo());
    }

    private void validarUnicidade(String cpf, String email, String login, Integer codigoAtual) {
        membroRepository.findByLogin(login.trim())
                .filter(membro -> !membro.getCodigo().equals(codigoAtual))
                .ifPresent(membro -> { throw new IllegalArgumentException("Login já cadastrado."); });
        membroRepository.findByEmailIgnoreCase(email.trim())
                .filter(membro -> !membro.getCodigo().equals(codigoAtual))
                .ifPresent(membro -> { throw new IllegalArgumentException("E-mail já cadastrado."); });
        membroRepository.findByCpf(normalizarCpf(cpf))
                .filter(membro -> !membro.getCodigo().equals(codigoAtual))
                .ifPresent(membro -> { throw new IllegalArgumentException("CPF já cadastrado."); });
    }

    private void salvarPerfil(Membro membro, MembroRequest request) {
        String perfil = request.perfil() == null ? "MEMBRO" : request.perfil().trim().toUpperCase(Locale.ROOT);
        if (!List.of("MEMBRO", "MUSICO", "PRODUTOR").contains(perfil)) {
            throw new IllegalArgumentException("Perfil inválido.");
        }
        var musicoExistente = musicoRepository.findById(membro.getCodigo());
        var produtorExistente = produtorRepository.findById(membro.getCodigo());
        if (!"MUSICO".equals(perfil) && musicoExistente.isPresent()) {
            exigirMusicoSemVinculos(membro.getCodigo());
            musicoRepository.delete(musicoExistente.get());
        }
        if (!"PRODUTOR".equals(perfil) && produtorExistente.isPresent()) {
            produtorRepository.delete(produtorExistente.get());
        }
        // Remove o subtipo antigo antes de alterar o discriminador referenciado pela FK composta.
        musicoRepository.flush();
        produtorRepository.flush();
        membro.setPerfil(perfil);
        membroRepository.saveAndFlush(membro);
        if ("MUSICO".equals(perfil)) {
            var musico = musicoExistente.orElseGet(Musico::new);
            musico.setMembro(membro);
            musico.setInstrumento(textoOuPadrao(request.instrumento()));
            musico.setBiografia(request.biografia());
            musicoRepository.save(musico);
        }
        if ("PRODUTOR".equals(perfil)) {
            var produtor = produtorExistente.orElseGet(Produtor::new);
            produtor.setMembro(membro);
            produtor.setEspecialidade(textoOuPadrao(request.especialidade()));
            produtorRepository.save(produtor);
        }
    }

    private void exigirMusicoSemVinculos(Integer id) {
        if (participacaoRepository.existsByMusicoCodigo(id)) {
            throw new IllegalArgumentException("O músico possui participações em eventos. Desative o membro em vez de remover seu perfil de músico.");
        }
    }

    private void protegerUltimoAdministrador(Membro membro, boolean administrador, boolean ativo, List<Membro> administradoresAtivos) {
        if (Boolean.TRUE.equals(membro.getAdministrador()) && Boolean.TRUE.equals(membro.getAtivo())
                && (!administrador || !ativo) && administradoresAtivos.size() <= 1) {
            throw new IllegalArgumentException("É necessário manter pelo menos um administrador ativo. Cadastre ou ative outro administrador primeiro.");
        }
    }

    private String normalizarCpf(String cpf) {
        return cpf.replaceAll("[^0-9]", "");
    }

    private String textoOuPadrao(String valor) {
        return valor == null || valor.isBlank() ? "Não informado" : valor.trim();
    }
}
