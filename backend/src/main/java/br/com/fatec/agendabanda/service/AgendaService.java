package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.exception.RegraNegocioException;

import br.com.fatec.agendabanda.dto.AgendaDto;
import br.com.fatec.agendabanda.dto.AgendaRequest;
import br.com.fatec.agendabanda.dto.PaginaDto;
import java.time.LocalDate;
import br.com.fatec.agendabanda.repository.AgendaSqlRepository;
import br.com.fatec.agendabanda.security.SessaoUsuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AgendaService {
    private final AgendaSqlRepository repository;
    private final AuditoriaContexto auditoria;

    public AgendaService(AgendaSqlRepository repository, AuditoriaContexto auditoria) {
        this.repository = repository;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public PaginaDto<AgendaDto> listar(int page, int size, LocalDate inicio, LocalDate fim) { return repository.listar(page, size, inicio, fim); }

    @Transactional(readOnly = true)
    public AgendaDto detalhar(Integer id) { return repository.detalhar(id); }

    @Transactional
    public AgendaDto criar(AgendaRequest request, SessaoUsuario sessao) {
        validarPeriodo(request);
        auditoria.preparar(sessao.codigo());
        repository.validarConflitos(request, null);
        return repository.detalhar(repository.criar(request));
    }

    @Transactional
    public AgendaDto atualizar(Integer id, AgendaRequest request, SessaoUsuario sessao) {
        validarPeriodo(request);
        auditoria.preparar(sessao.codigo());
        repository.validarConflitos(request, id);
        repository.atualizar(id, request);
        return repository.detalhar(id);
    }

    @Transactional
    public void excluir(Integer id, SessaoUsuario sessao) {
        auditoria.preparar(sessao.codigo());
        repository.excluir(id, sessao);
    }

    private void validarPeriodo(AgendaRequest request) {
        if (request.dataHoraInicio() == null || request.dataHoraFim() == null
                || !request.dataHoraFim().isAfter(request.dataHoraInicio())) {
            throw new RegraNegocioException("A data/hora final deve ser maior que a inicial.");
        }
        var regras = java.time.ZoneId.of("America/Sao_Paulo").getRules();
        for (var horario : List.of(request.dataHoraInicio(), request.dataHoraFim())) {
            if (horario.getYear() < 1 || horario.getYear() > 9999 || regras.getValidOffsets(horario).isEmpty()) {
                throw new RegraNegocioException("Informe horários válidos de São Paulo, entre os anos 1 e 9999.");
            }
        }
    }
}
