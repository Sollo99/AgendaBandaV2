package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.*;
import br.com.fatec.agendabanda.model.Membro;
import br.com.fatec.agendabanda.model.Musico;
import br.com.fatec.agendabanda.model.Produtor;
import br.com.fatec.agendabanda.repository.MembroRepository;
import br.com.fatec.agendabanda.repository.MusicoRepository;
import br.com.fatec.agendabanda.repository.ProdutorRepository;
import br.com.fatec.agendabanda.repository.AgendaRepository;
import br.com.fatec.agendabanda.repository.ParticipacaoEventoRepository;
import br.com.fatec.agendabanda.repository.EnsaioMusicoRepository;
import br.com.fatec.agendabanda.repository.RedefinicaoSenhaRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class MembroService {
    private final MembroRepository membroRepository;
    private final MusicoRepository musicoRepository;
    private final ProdutorRepository produtorRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MapperService mapperService;
    private final AgendaRepository agendaRepository;
    private final ParticipacaoEventoRepository participacaoRepository;
    private final EnsaioMusicoRepository ensaioMusicoRepository;
    private final RedefinicaoSenhaRepository redefinicaoSenhaRepository;

    public MembroService(MembroRepository membroRepository, MusicoRepository musicoRepository, ProdutorRepository produtorRepository,
                         BCryptPasswordEncoder passwordEncoder, MapperService mapperService,
                         AgendaRepository agendaRepository, ParticipacaoEventoRepository participacaoRepository,
                         EnsaioMusicoRepository ensaioMusicoRepository, RedefinicaoSenhaRepository redefinicaoSenhaRepository) {
        this.membroRepository = membroRepository;
        this.musicoRepository = musicoRepository;
        this.produtorRepository = produtorRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapperService = mapperService;
        this.agendaRepository = agendaRepository;
        this.participacaoRepository = participacaoRepository;
        this.ensaioMusicoRepository = ensaioMusicoRepository;
        this.redefinicaoSenhaRepository = redefinicaoSenhaRepository;
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
        membro.setCriadoEm(LocalDateTime.now());
        var salvo = membroRepository.save(membro);
        salvarPerfil(salvo, request);
        return mapperService.membroDto(salvo);
    }

    @Transactional
    public MembroDto atualizar(Integer id, MembroRequest request) {
        var membro = buscarEntidade(id);
        validarUnicidade(request.cpf(), request.email(), request.login(), id);
        aplicarDados(membro, request);
        if (request.senha() != null && !request.senha().isBlank()) {
            membro.setSenha(passwordEncoder.encode(request.senha()));
        }
        var salvo = membroRepository.save(membro);
        salvarPerfil(salvo, request);
        return mapperService.membroDto(salvo);
    }

    @Transactional
    public MembroDto alterarStatus(Integer id, boolean ativo) {
        var membro = buscarEntidade(id);
        membro.setAtivo(ativo);
        return mapperService.membroDto(membroRepository.save(membro));
    }

    @Transactional
    public MembroDto criarCadastro(CadastroRequest request) {
        validarUnicidade(request.cpf(), request.email(), request.login(), null);
        var membro = new Membro();
        membro.setNome(request.nome().trim());
        membro.setCpf(request.cpf().trim());
        membro.setEmail(request.email().trim());
        membro.setLogin(request.login().trim());
        membro.setSenha(passwordEncoder.encode(request.senha()));
        membro.setAdministrador(false);
        membro.setAtivo(true);
        membro.setCriadoEm(LocalDateTime.now());
        var salvo = membroRepository.save(membro);
        var musico = new Musico();
        musico.setMembro(salvo);
        musico.setInstrumento(textoOuPadrao(request.instrumento()));
        musico.setBiografia(request.biografia());
        musicoRepository.save(musico);
        return mapperService.membroDto(salvo);
    }

    @Transactional(readOnly = true)
    public MembroDto obterProprio(Integer codigoMembro) {
        return mapperService.membroDto(buscarEntidade(codigoMembro));
    }

    @Transactional
    public MembroDto atualizarProprio(Integer codigoMembro, PerfilRequest request) {
        var membro = buscarEntidade(codigoMembro);
        validarUnicidade(request.cpf(), request.email(), request.login(), codigoMembro);
        membro.setNome(request.nome().trim());
        membro.setCpf(request.cpf().trim());
        membro.setEmail(request.email().trim());
        membro.setLogin(request.login().trim());
        if (request.novaSenha() != null && !request.novaSenha().isBlank()) {
            if (request.senhaAtual() == null || !passwordEncoder.matches(request.senhaAtual(), membro.getSenha())) {
                throw new IllegalArgumentException("A senha atual informada é inválida.");
            }
            membro.setSenha(passwordEncoder.encode(request.novaSenha()));
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
        membro.setSenha(passwordEncoder.encode(novaSenha));
        membroRepository.save(membro);
    }

    @Transactional
    public void excluir(Integer id) {
        var membro = buscarEntidade(id);
        exigirMusicoSemVinculos(id);
        if (agendaRepository.existsByCriadorCodigo(id) || redefinicaoSenhaRepository.existsByAdministradorCodigo(id)) {
            throw new IllegalArgumentException("O membro possui eventos criados ou decisões de recuperação de senha. Desative-o para preservar o histórico.");
        }
        musicoRepository.findById(id).ifPresent(musicoRepository::delete);
        produtorRepository.findById(id).ifPresent(produtorRepository::delete);
        musicoRepository.flush();
        produtorRepository.flush();
        membroRepository.delete(membro);
    }

    private void aplicarDados(Membro membro, MembroRequest request) {
        membro.setNome(request.nome().trim());
        membro.setCpf(request.cpf().trim());
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
        membroRepository.findByCpf(cpf.trim())
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
        if (participacaoRepository.existsByMusicoCodigo(id) || ensaioMusicoRepository.existsByMusicoCodigo(id)) {
            throw new IllegalArgumentException("O músico possui participações em eventos. Desative o membro em vez de excluir ou remover seu perfil de músico.");
        }
    }

    private String textoOuPadrao(String valor) {
        return valor == null || valor.isBlank() ? "Não informado" : valor.trim();
    }
}
