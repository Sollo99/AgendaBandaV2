package br.com.fatec.agendabanda.controller;

import br.com.fatec.agendabanda.dto.AgendaDto;
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
    public List<AgendaDto> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public AgendaDto detalhar(@PathVariable Integer id) {
        return service.detalhar(id);
    }

    @PostMapping
    public AgendaDto criar(@Valid @RequestBody AgendaRequest request, HttpServletRequest http) {
        return service.criar(request, tokenService.sessaoObrigatoria(http));
    }

    @PutMapping("/{id}")
    public AgendaDto atualizar(@PathVariable Integer id, @Valid @RequestBody AgendaRequest request, HttpServletRequest http) {
        return service.atualizar(id, request, tokenService.sessaoObrigatoria(http));
    }

    @DeleteMapping("/{id}")
    public Map<String, String> excluir(@PathVariable Integer id, HttpServletRequest http) {
        service.excluir(id, tokenService.sessaoObrigatoria(http));
        return Map.of("mensagem", "Evento excluído com sucesso.");
    }

    @GetMapping("/{id}/participacoes")
    public List<ParticipacaoDto> listarParticipacoes(@PathVariable Integer id) {
        return participacaoService.listar(id);
    }

    @PatchMapping("/{id}/participacoes/minha")
    public ParticipacaoDto atualizarMinhaParticipacao(@PathVariable Integer id, @Valid @RequestBody ParticipacaoAtualizacaoRequest request, HttpServletRequest http) {
        return participacaoService.atualizarPropria(id, request, tokenService.sessaoObrigatoria(http));
    }

    @PatchMapping("/{id}/participacoes/{musicoId}")
    public ParticipacaoDto atualizarParticipacao(@PathVariable Integer id, @PathVariable Integer musicoId,
                                                   @Valid @RequestBody ParticipacaoAtualizacaoRequest request, HttpServletRequest http) {
        permissaoService.exigirAdmin(tokenService.sessaoObrigatoria(http));
        return participacaoService.atualizarComoAdministrador(id, musicoId, request);
    }
}
