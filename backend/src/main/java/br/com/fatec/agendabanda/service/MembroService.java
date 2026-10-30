package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.exception.RecursoNaoEncontradoException;

import br.com.fatec.agendabanda.exception.RegraNegocioException;

import br.com.fatec.agendabanda.dto.*;
import br.com.fatec.agendabanda.model.Membro;
import br.com.fatec.agendabanda.model.Musico;
import br.com.fatec.agendabanda.model.Produtor;
import br.com.fatec.agendabanda.repository.MembroRepository;
import br.com.fatec.agendabanda.repository.MembroSqlRepository;
import br.com.fatec.agendabanda.repository.MusicoRepository;
import br.com.fatec.agendabanda.repository.ProdutorRepository;
import br.com.fatec.agendabanda.repository.ParticipacaoEventoRepository;
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
    private final MembroSqlRepository membroSql;
    private final MusicoRepository musicoRepository;
    private final ProdutorRepository produtorRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MapperService mapperService;
    private final ParticipacaoEventoRepository participacaoRepository;
    private final SenhaMembroService senhas;
    private final ValidadorMembro validador;
    private final Clock clock;
    private final AuditoriaContexto auditoria;

    public MembroService(MembroRepository membroRepository, MusicoRepository musicoRepository, ProdutorRepository produtorRepository,
                         BCryptPasswordEncoder passwordEncoder, MapperService mapperService,
                         ParticipacaoEventoRepository participacaoRepository, SenhaMembroService senhas, ValidadorMembro validador,
                         Clock clock, AuditoriaContexto auditoria, MembroSqlRepository membroSql) {
        this.membroRepository = membroRepository;
        this.membroSql = membroSql;
        this.musicoRepository = musicoRepository;
        this.produtorRepository = produtorRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapperService = mapperService;
        this.participacaoRepository = participacaoRepository;
        this.senhas = senhas;
        this.validador = validador;
        this.clock = clock;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public PaginaDto<MembroDto> listar(int page, int size) {
        PaginaDto.validar(page, size);
        var resultado = membroRepository.listarPagina(org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("nome", "codigo")));
        return PaginaDto.de(resultado.getContent(), page, size, resultado.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<MusicoResumoDto> listarMusicos() {
        return musicoRepository.findByMembroAtivoTrueOrderByMembroNomeAsc().stream()
                .map(musico -> new MusicoResumoDto(musico.getCodigo(), musico.getMembro().getNome(), musico.getInstrumento()))
                .toList();
    }

    public Membro buscarEntidade(Integer id) {
        return membroRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Membro não encontrado."));
    }

    @Transactional
    public MembroDto criar(MembroRequest request) {
        auditoria.prepararDoRequest();
        if (request.senha() == null || request.senha().isBlank()) throw new RegraNegocioException("Senha é obrigatória no cadastro.");

        var membro = new Membro();
        validador.validarUnicidade(request.cpf(), request.email(), request.login(), null);
        aplicarDados(membro, request);
        membro.setAdministrador(Boolean.TRUE.equals(request.administrador()));
        membro.setAtivo(request.ativo() == null || request.ativo());
        membro.setSenha(passwordEncoder.encode(request.senha()));
        membro.setCriadoEm(LocalDateTime.now(clock));
        var salvo = membroRepository.save(membro);
        salvarPerfil(salvo, request);
        return mapperService.membroDto(salvo);
    }

    @Transactional
    public MembroDto atualizar(Integer id, MembroRequest request) {
        auditoria.prepararDoRequest();
        // SQL altera acesso antes de carregar a entidade JPA, evitando estado antigo no contexto.
        // Se outra validacao falhar, toda a transacao (inclusive a procedure) sofre rollback.
        membroSql.alterarAcesso(id, Boolean.TRUE.equals(request.administrador()), request.ativo() == null || request.ativo());
        var membro = buscarParaAtualizar(id);
        exigirIdentificadoresReservados(membro);
        validador.validarUnicidade(request.cpf(), request.email(), request.login(), id);
        aplicarDados(membro, request);
        if (request.senha() != null && !request.senha().isBlank()) {
            senhas.alterar(membro, request.senha());
        }
        var salvo = membroRepository.save(membro);
        salvarPerfil(salvo, request);
        return mapperService.membroDto(salvo);
    }

    @Transactional
    public MembroDto alterarStatus(Integer id, boolean ativo) {
        auditoria.prepararDoRequest();
        membroSql.alterarAcesso(id, null, ativo);
        return mapperService.membroDto(buscarParaAtualizar(id));
    }

    @Transactional
    public void criarCadastro(CadastroRequest request) {
        auditoria.prepararDoRequest();
        String hashSenha = passwordEncoder.encode(request.senha());
        // Consulta os três campos mesmo se um deles já existir, sem indicar qual colidiu.
        boolean duplicado = membroRepository.findByLogin(request.login().trim()).isPresent()
                | membroRepository.findByEmailIgnoreCase(request.email().trim()).isPresent()
                | membroRepository.findByCpf(ValidadorMembro.normalizarCpf(request.cpf())).isPresent();
        if (duplicado) return;
        var membro = new Membro();
        membro.setNome(request.nome().trim());
        membro.setCpf(ValidadorMembro.normalizarCpf(request.cpf()));
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
        auditoria.preparar(codigoAdministrador);
        membroSql.decidirCadastro(id, aprovada, LocalDateTime.now(clock));
        return mapperService.membroDto(buscarParaAtualizar(id));
    }

    @Transactional
    public MembroDto liberarIdentificadores(Integer id, Integer codigoAdministrador) {
        auditoria.preparar(codigoAdministrador);
        String senhaInutilizavel = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
        membroSql.liberarIdentificadores(id, senhaInutilizavel, LocalDateTime.now(clock));
        return mapperService.membroDto(buscarParaAtualizar(id));
    }

    private void exigirIdentificadoresReservados(Membro membro) {
        if (membro.getIdentificadoresLiberadosEm() != null) {
            throw new RegraNegocioException("Os identificadores deste cadastro foram liberados. É necessário fazer um novo cadastro.");
        }
    }

    private Membro buscarParaAtualizar(Integer id) {
        return membroRepository.bloquearPorCodigo(id).orElseThrow(() -> new RecursoNaoEncontradoException("Membro não encontrado."));
    }

    private void aplicarDados(Membro membro, MembroRequest request) {
        membro.setNome(request.nome().trim());
        membro.setCpf(ValidadorMembro.normalizarCpf(request.cpf()));
        membro.setEmail(request.email().trim());
        membro.setLogin(request.login().trim());
    }

    private void salvarPerfil(Membro membro, MembroRequest request) {
        String perfil = request.perfil() == null ? "MEMBRO" : request.perfil().trim().toUpperCase(Locale.ROOT);
        if (!List.of("MEMBRO", "MUSICO", "PRODUTOR").contains(perfil)) {
            throw new RegraNegocioException("Perfil inválido.");
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
            throw new RegraNegocioException("O músico possui participações em eventos. Desative o membro em vez de remover seu perfil de músico.");
        }
    }

    private String textoOuPadrao(String valor) {
        return valor == null || valor.isBlank() ? "Não informado" : valor.trim();
    }
}
