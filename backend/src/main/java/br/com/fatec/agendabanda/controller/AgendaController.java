package br.com.fatec.agendabanda.controller;

import br.com.fatec.agendabanda.dto.AgendaDto;
import br.com.fatec.agendabanda.dto.PaginaDto;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import br.com.fatec.agendabanda.dto.AgendaRequest;
import br.com.fatec.agendabanda.dto.ParticipacaoAtualizacaoRequest;
import br.com.fatec.agendabanda.dto.ParticipacaoDto;
import br.com.fatec.agendabanda.security.TokenService;
import br.com.fatec.agendabanda.service.AgendaService;
import br.com.fatec.agendabanda.service.ParticipacaoEventoService;
import br.com.fatec.agendabanda.service.PermissaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agenda")
public class AgendaController {
    private final AgendaService service;
    private final TokenService tokenService;
    private final ParticipacaoEventoService participacaoService;
    private final PermissaoService permissaoService;

    public AgendaController(AgendaService service, TokenService tokenService, ParticipacaoEventoService participacaoService, PermissaoService permissaoService) {
        this.service = service;
        this.tokenService = tokenService;
        this.participacaoService = participacaoService;
        this.permissaoService = permissaoService;
    }

    @GetMapping
    public PaginaDto<AgendaDto> listar(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "50") int size,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return service.listar(page, size, inicio, fim);
    }

    @GetMapping("/{id}")
    public AgendaDto detalhar(@PathVariable @jakarta.validation.constraints.Positive Integer id) {
        return service.detalhar(id);
    }

    @PostMapping
    public AgendaDto criar(@Valid @RequestBody AgendaRequest request, HttpServletRequest http) {
        return service.criar(request, tokenService.sessaoObrigatoria(http));
    }

    @PutMapping("/{id}")
    public AgendaDto atualizar(@PathVariable @jakarta.validation.constraints.Positive Integer id, @Valid @RequestBody AgendaRequest request, HttpServletRequest http) {
        return service.atualizar(id, request, tokenService.sessaoObrigatoria(http));
    }

    @DeleteMapping("/{id}")
    public Map<String, String> excluir(@PathVariable @jakarta.validation.constraints.Positive Integer id, HttpServletRequest http) {
        service.excluir(id, tokenService.sessaoObrigatoria(http));
        return Map.of("mensagem", "Evento excluído com sucesso.");
    }

    @GetMapping("/{id}/participacoes")
    public List<ParticipacaoDto> listarParticipacoes(@PathVariable @jakarta.validation.constraints.Positive Integer id) {
        return participacaoService.listar(id);
    }

    @PatchMapping("/{id}/participacoes/minha")
    public ParticipacaoDto atualizarMinhaParticipacao(@PathVariable @jakarta.validation.constraints.Positive Integer id, @Valid @RequestBody ParticipacaoAtualizacaoRequest request, HttpServletRequest http) {
        return participacaoService.atualizarPropria(id, request, tokenService.sessaoObrigatoria(http));
    }

    @PatchMapping("/{id}/participacoes/{musicoId}")
    public ParticipacaoDto atualizarParticipacao(@PathVariable @jakarta.validation.constraints.Positive Integer id, @PathVariable @jakarta.validation.constraints.Positive Integer musicoId,
                                                   @Valid @RequestBody ParticipacaoAtualizacaoRequest request, HttpServletRequest http) {
        var sessao = tokenService.sessaoObrigatoria(http);
        permissaoService.exigirAdmin(sessao);
        return participacaoService.atualizarComoAdministrador(id, musicoId, request, sessao);
    }
}
