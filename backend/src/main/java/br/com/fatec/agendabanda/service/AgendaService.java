package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.AgendaDto;
import br.com.fatec.agendabanda.dto.AgendaRequest;
import br.com.fatec.agendabanda.model.*;
import br.com.fatec.agendabanda.repository.*;
import br.com.fatec.agendabanda.security.SessaoUsuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class AgendaService {
    private final AgendaRepository agendaRepository;
    private final EnsaioRepository ensaioRepository;
    private final ReuniaoRepository reuniaoRepository;
    private final ShowEventoRepository showRepository;
    private final LocalEventoRepository localRepository;
    private final MusicoRepository musicoRepository;
    private final MembroService membroService;
    private final LocalService localService;
    private final CidadeRepository cidadeRepository;
    private final TipoEventoRepository tipoEventoRepository;
    private final ParticipacaoEventoRepository participacaoEventoRepository;
    private final MapperService mapper;
    private final PermissaoService permissaoService;

    public AgendaService(AgendaRepository agendaRepository, EnsaioRepository ensaioRepository, ReuniaoRepository reuniaoRepository,
                         ShowEventoRepository showRepository, LocalEventoRepository localRepository, MusicoRepository musicoRepository,
                         MembroService membroService, LocalService localService,
                         CidadeRepository cidadeRepository, TipoEventoRepository tipoEventoRepository,
                         ParticipacaoEventoRepository participacaoEventoRepository, MapperService mapper, PermissaoService permissaoService) {
        this.agendaRepository = agendaRepository;
        this.ensaioRepository = ensaioRepository;
        this.reuniaoRepository = reuniaoRepository;
        this.showRepository = showRepository;
        this.localRepository = localRepository;
        this.musicoRepository = musicoRepository;
        this.membroService = membroService;
        this.localService = localService;
        this.cidadeRepository = cidadeRepository;
        this.tipoEventoRepository = tipoEventoRepository;
        this.participacaoEventoRepository = participacaoEventoRepository;
        this.mapper = mapper;
        this.permissaoService = permissaoService;
    }

    @Transactional(readOnly = true)
    public List<AgendaDto> listar() {
        return agendaRepository.findAllByOrderByDataHoraInicioAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AgendaDto detalhar(Integer id) {
        return toDto(buscar(id));
    }

    public Agenda buscar(Integer id) {
        return agendaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Evento não encontrado."));
    }

    @Transactional
    public AgendaDto criar(AgendaRequest request, SessaoUsuario sessao) {
        validarPeriodo(request);
        localRepository.bloquearAgendamento();
        var agenda = new Agenda();
        aplicarDados(agenda, request);
        var musicosIds = idsUnicos(request.musicosIds());
        validarDisponibilidade(agenda, musicosIds);
        agenda.setCriador(membroService.buscarEntidade(sessao.codigo()));
        agenda.setCriadoEm(LocalDateTime.now());
        var salvo = agendaRepository.save(agenda);
        salvarDetalhes(salvo, request);
        sincronizarParticipacoes(salvo, musicosIds);
        return toDto(salvo);
    }

    @Transactional
    public AgendaDto atualizar(Integer id, AgendaRequest request, SessaoUsuario sessao) {
        validarPeriodo(request);
        localRepository.bloquearAgendamento();
        var agenda = buscar(id);
        permissaoService.exigirAdminOuCriador(sessao, agenda);
        var musicosIds = request.musicosIds() == null
                ? participacaoEventoRepository.findByAgendaCodigoAgendaOrderByMusicoMembroNomeAsc(id).stream()
                    .map(item -> item.getMusico().getCodigo()).toList()
                : idsUnicos(request.musicosIds());
        aplicarDados(agenda, request);
        validarDisponibilidade(agenda, musicosIds);
        agenda.setAtualizadoEm(LocalDateTime.now());
        var salvo = agendaRepository.save(agenda);
        salvarDetalhes(salvo, request);
        sincronizarParticipacoes(salvo, musicosIds);
        return toDto(salvo);
    }

    @Transactional
    public void excluir(Integer id, SessaoUsuario sessao) {
        localRepository.bloquearAgendamento();
        var agenda = buscar(id);
        permissaoService.exigirAdminOuCriador(sessao, agenda);
        agendaRepository.delete(agenda);
    }

    private void aplicarDados(Agenda agenda, AgendaRequest request) {
        agenda.setTitulo(request.titulo());
        agenda.setDescricao(request.descricao());
        agenda.setDataHoraInicio(request.dataHoraInicio());
        agenda.setDataHoraFim(request.dataHoraFim());
        agenda.setLocal(localService.buscarLocal(request.localId()));
        var tipoEvento = tipoEventoRepository.findById(request.tipoEventoId())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de evento não encontrado."));
        if (!Boolean.TRUE.equals(tipoEvento.getAtivo())) {
            throw new IllegalArgumentException("O tipo de evento informado está inativo.");
        }
        agenda.setTipoEvento(tipoEvento);
        if (request.status() != null) agenda.setStatus(request.status());
        else if (agenda.getStatus() == null) agenda.setStatus("AGENDADO");
    }

    private void salvarDetalhes(Agenda agenda, AgendaRequest request) {
        String tipo = agenda.getTipoEvento().getCategoriaDetalhe();
        Integer id = agenda.getCodigoAgenda();
        if (!"ENSAIO".equals(tipo)) ensaioRepository.deleteById(id);
        if (!"SHOW".equals(tipo)) showRepository.deleteById(id);
        if (!"REUNIAO".equals(tipo)) reuniaoRepository.deleteById(id);
        if ("ENSAIO".equals(tipo)) {
            var ensaio = ensaioRepository.findById(id).orElseGet(Ensaio::new);
            ensaio.setAgenda(agenda);
            ensaio.setRepertorio(request.repertorio() == null || request.repertorio().isBlank() ? "Repertório não informado" : request.repertorio());
            ensaio.setObservacao(request.observacao());
            ensaioRepository.save(ensaio);
        } else if ("SHOW".equals(tipo)) {
            var show = showRepository.findById(id).orElseGet(ShowEvento::new);
            show.setAgenda(agenda);
            if (request.cidadeId() == null) throw new IllegalArgumentException("Informe a cidade do show.");
            var cidade = cidadeRepository.findById(request.cidadeId()).orElseThrow(() -> new IllegalArgumentException("Cidade do show não encontrada."));
            show.setCidade(cidade);
            showRepository.save(show);
        } else if ("REUNIAO".equals(tipo)) {
            var reuniao = reuniaoRepository.findById(id).orElseGet(Reuniao::new);
            reuniao.setAgenda(agenda);
            reuniao.setPauta(request.pauta() == null || request.pauta().isBlank() ? "Pauta não informada" : request.pauta());
            reuniaoRepository.save(reuniao);
        }
    }

    private void sincronizarParticipacoes(Agenda agenda, List<Integer> musicosIds) {
        var existentes = participacaoEventoRepository.findByAgendaCodigoAgendaOrderByMusicoMembroNomeAsc(agenda.getCodigoAgenda());
        for (var participacao : existentes) {
            if (!musicosIds.contains(participacao.getMusico().getCodigo())) {
                participacaoEventoRepository.delete(participacao);
            }
        }
        for (Integer musicoId : musicosIds) {
            if (existentes.stream().anyMatch(item -> item.getMusico().getCodigo().equals(musicoId))) continue;
            var musico = buscarMusico(musicoId);
            var participacao = new ParticipacaoEvento();
            participacao.setId(new ParticipacaoEventoId(agenda.getCodigoAgenda(), musico.getCodigo()));
            participacao.setAgenda(agenda);
            participacao.setMusico(musico);
            participacao.setStatusConfirmacao("PENDENTE");
            participacaoEventoRepository.save(participacao);
        }
    }

    private List<Integer> idsUnicos(List<Integer> musicosIds) {
        if (musicosIds == null) return List.of();
        if (musicosIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("A lista de músicos não pode conter valores vazios.");
        }
        return List.copyOf(new LinkedHashSet<>(musicosIds));
    }

    private Musico buscarMusico(Integer codigoMusico) {
        var musico = musicoRepository.findById(codigoMusico).orElseThrow(() -> new IllegalArgumentException("Músico não encontrado."));
        if (!Boolean.TRUE.equals(musico.getMembro().getAtivo())) {
            throw new IllegalArgumentException("Não é possível incluir um músico inativo no evento.");
        }
        return musico;
    }

    private void validarDisponibilidade(Agenda agenda, List<Integer> musicosIds) {
        if ("CANCELADO".equals(agenda.getStatus())) return;
        for (var outro : agendaRepository.buscarSobrepostos(agenda.getDataHoraInicio(), agenda.getDataHoraFim(), agenda.getCodigoAgenda())) {
            if (outro.getLocal().getCodigoLocal().equals(agenda.getLocal().getCodigoLocal())) {
                throw new IllegalArgumentException("O local já está ocupado pelo evento \"" + outro.getTitulo() + "\" neste horário.");
            }
            var participantes = participacaoEventoRepository.findByAgendaCodigoAgendaOrderByMusicoMembroNomeAsc(outro.getCodigoAgenda());
            for (var participacao : participantes) {
                if (musicosIds.contains(participacao.getMusico().getCodigo())) {
                    throw new IllegalArgumentException("O músico " + participacao.getMusico().getMembro().getNome()
                            + " já está vinculado ao evento \"" + outro.getTitulo() + "\" neste horário.");
                }
            }
        }
    }

    private void validarPeriodo(AgendaRequest request) {
        if (!request.dataHoraFim().isAfter(request.dataHoraInicio())) {
            throw new IllegalArgumentException("A data/hora final deve ser maior que a inicial.");
        }
    }

    private AgendaDto toDto(Agenda agenda) {
        var ensaio = ensaioRepository.findById(agenda.getCodigoAgenda()).orElse(null);
        var reuniao = reuniaoRepository.findById(agenda.getCodigoAgenda()).orElse(null);
        var show = showRepository.findById(agenda.getCodigoAgenda()).orElse(null);
        String tipoEspecifico = agenda.getTipoEvento().getCategoriaDetalhe();
        var participacoes = participacaoEventoRepository.findByAgendaCodigoAgendaOrderByMusicoMembroNomeAsc(agenda.getCodigoAgenda());
        var musicos = participacoes.stream()
                .map(item -> mapper.musicoResumoDto(item.getMusico()))
                .toList();

        return new AgendaDto(
                agenda.getCodigoAgenda(), agenda.getTitulo(), agenda.getDescricao(), agenda.getDataHoraInicio(), agenda.getDataHoraFim(),
                mapper.localDto(agenda.getLocal()), mapper.membroResumoDto(agenda.getCriador()),
                mapper.tipoEventoDto(agenda.getTipoEvento()), agenda.getStatus(),
                tipoEspecifico,
                ensaio != null ? ensaio.getRepertorio() : null,
                ensaio != null ? ensaio.getObservacao() : null,
                reuniao != null ? reuniao.getPauta() : null,
                show != null ? mapper.cidadeDto(show.getCidade()) : null,
                musicos,
                participacoes.stream().map(mapper::participacaoDto).toList()
        );
    }
}
