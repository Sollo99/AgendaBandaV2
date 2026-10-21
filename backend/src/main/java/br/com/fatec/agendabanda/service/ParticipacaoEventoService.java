package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.ParticipacaoAtualizacaoRequest;
import br.com.fatec.agendabanda.dto.ParticipacaoDto;
import br.com.fatec.agendabanda.model.ParticipacaoEventoId;
import br.com.fatec.agendabanda.repository.MusicoRepository;
import br.com.fatec.agendabanda.repository.ParticipacaoEventoRepository;
import br.com.fatec.agendabanda.security.SessaoUsuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ParticipacaoEventoService {
    private final ParticipacaoEventoRepository participacaoRepository;
    private final MusicoRepository musicoRepository;
    private final MapperService mapper;

    public ParticipacaoEventoService(ParticipacaoEventoRepository participacaoRepository, MusicoRepository musicoRepository, MapperService mapper) {
        this.participacaoRepository = participacaoRepository;
        this.musicoRepository = musicoRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ParticipacaoDto> listar(Integer codigoAgenda) {
        return participacaoRepository.findByAgendaCodigoAgendaOrderByMusicoMembroNomeAsc().stream().map(mapper::participacaoDto).toList();
    }

    @Transactional
    public ParticipacaoDto atualizarPropria(Integer codigoAgenda, ParticipacaoAtualizacaoRequest request, SessaoUsuario sessao) {
        var musico = musicoRepository.findById(sessao.codigo())
                .orElseThrow(() -> new SecurityException("Apenas músicos podem confirmar participação."));
        var participacao = buscar(codigoAgenda, musico.getCodigo());
        if (request.statusConfirmacao() == null) {
            throw new IllegalArgumentException("Informe o status de confirmação.");
        }
        participacao.setStatusConfirmacao(request.statusConfirmacao());
        participacao.setDataResposta(LocalDateTime.now());
        if (request.observacao() != null) participacao.setObservacao(request.observacao());
        return mapper.participacaoDto(participacaoRepository.save(participacao));
    }

    @Transactional
    public ParticipacaoDto atualizarComoAdministrador(Integer codigoAgenda, Integer codigoMusico, ParticipacaoAtualizacaoRequest request) {
        var participacao = buscar(codigoAgenda, codigoMusico);
        if (request.statusConfirmacao() != null) {
            participacao.setStatusConfirmacao(request.statusConfirmacao());
            participacao.setDataResposta(LocalDateTime.now());
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
