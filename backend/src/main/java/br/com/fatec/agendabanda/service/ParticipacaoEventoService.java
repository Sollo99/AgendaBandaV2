package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.ParticipacaoAtualizacaoRequest;
import br.com.fatec.agendabanda.dto.ParticipacaoDto;
import br.com.fatec.agendabanda.model.ParticipacaoEventoId;
import br.com.fatec.agendabanda.repository.MusicoRepository;
import br.com.fatec.agendabanda.repository.AgendaRepository;
import br.com.fatec.agendabanda.repository.ParticipacaoEventoRepository;
import br.com.fatec.agendabanda.security.SessaoUsuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;

@Service
public class ParticipacaoEventoService {
    private final ParticipacaoEventoRepository participacaoRepository;
    private final MusicoRepository musicoRepository;
    private final MapperService mapper;
    private final AgendaRepository agendaRepository;
    private final Clock clock;

    public ParticipacaoEventoService(ParticipacaoEventoRepository participacaoRepository, MusicoRepository musicoRepository, MapperService mapper,
                                    AgendaRepository agendaRepository, Clock clock) {
        this.participacaoRepository = participacaoRepository;
        this.musicoRepository = musicoRepository;
        this.mapper = mapper;
        this.agendaRepository = agendaRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ParticipacaoDto> listar(Integer codigoAgenda) {
        return participacaoRepository.findByAgendaCodigoAgendaOrderByMusicoMembroNomeAsc(codigoAgenda).stream().map(mapper::participacaoDto).toList();
    }

    @Transactional
    public ParticipacaoDto atualizarPropria(Integer codigoAgenda, ParticipacaoAtualizacaoRequest request, SessaoUsuario sessao) {
        var agenda = agendaRepository.bloquearPorCodigo(codigoAgenda)
                .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado."));
        if (!"AGENDADO".equals(agenda.getStatus()) || !agenda.getDataHoraInicio().isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("A confirmação só pode ser alterada antes do início de um evento agendado.");
        }
        var musico = musicoRepository.findById(sessao.codigo())
                .orElseThrow(() -> new SecurityException("Apenas músicos podem confirmar participação."));
        var participacao = buscar(codigoAgenda, musico.getCodigo());
        if (request.statusConfirmacao() == null) {
            throw new IllegalArgumentException("Informe o status de confirmação.");
        }
        participacao.setStatusConfirmacao(request.statusConfirmacao());
        participacao.setDataResposta(LocalDateTime.now(clock));
        if (request.observacao() != null) participacao.setObservacao(request.observacao());
        return mapper.participacaoDto(participacaoRepository.save(participacao));
    }

    @Transactional
    public ParticipacaoDto atualizarComoAdministrador(Integer codigoAgenda, Integer codigoMusico, ParticipacaoAtualizacaoRequest request) {
        var agenda = agendaRepository.bloquearPorCodigo(codigoAgenda)
                .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado."));
        if (request.presente() != null && ("CANCELADO".equals(agenda.getStatus())
                || agenda.getDataHoraInicio().isAfter(LocalDateTime.now(clock)))) {
            throw new IllegalArgumentException("Presença ou ausência só pode ser registrada após o início de um evento não cancelado.");
        }
        var participacao = buscar(codigoAgenda, codigoMusico);
        var statusFinal = request.statusConfirmacao() == null ? participacao.getStatusConfirmacao() : request.statusConfirmacao();
        var presenteFinal = request.presente() == null ? participacao.getPresente() : request.presente();
        if (Boolean.TRUE.equals(presenteFinal) && !"CONFIRMADO".equals(statusFinal)) {
            throw new IllegalArgumentException("Confirme a participação antes de marcar presença. Um participante presente deve permanecer confirmado.");
        }
        if (request.statusConfirmacao() != null) {
            participacao.setStatusConfirmacao(request.statusConfirmacao());
            participacao.setDataResposta(LocalDateTime.now(clock));
        }
        if (request.presente() != null) participacao.setPresente(request.presente());
        if (request.observacao() != null) participacao.setObservacao(request.observacao());
        return mapper.participacaoDto(participacaoRepository.save(participacao));
    }

    private br.com.fatec.agendabanda.model.ParticipacaoEvento buscar(Integer codigoAgenda, Integer codigoMusico) {
        return participacaoRepository.findById(new ParticipacaoEventoId(codigoAgenda, codigoMusico))
                .orElseThrow(() -> new IllegalArgumentException("Participação não encontrada para este músico e evento."));
    }
}
