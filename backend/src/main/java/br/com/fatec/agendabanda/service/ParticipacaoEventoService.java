package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.ParticipacaoAtualizacaoRequest;
import br.com.fatec.agendabanda.dto.ParticipacaoDto;
import br.com.fatec.agendabanda.repository.AgendaSqlRepository;
import br.com.fatec.agendabanda.security.SessaoUsuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ParticipacaoEventoService {
    private final AgendaSqlRepository repository;
    private final AuditoriaContexto auditoria;

    public ParticipacaoEventoService(AgendaSqlRepository repository, AuditoriaContexto auditoria) {
        this.repository = repository;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<ParticipacaoDto> listar(Integer codigoAgenda) { return repository.listarParticipacoes(codigoAgenda); }

    @Transactional
    public ParticipacaoDto atualizarPropria(Integer codigoAgenda, ParticipacaoAtualizacaoRequest request, SessaoUsuario sessao) {
        auditoria.preparar(sessao.codigo());
        return repository.atualizarParticipacao(codigoAgenda, sessao.codigo(), request, true);
    }

    @Transactional
    public ParticipacaoDto atualizarComoAdministrador(Integer codigoAgenda, Integer codigoMusico,
                                                     ParticipacaoAtualizacaoRequest request, SessaoUsuario sessao) {
        auditoria.preparar(sessao.codigo());
        return repository.atualizarParticipacao(codigoAgenda, codigoMusico, request, false);
    }
}
