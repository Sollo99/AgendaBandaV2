package br.com.fatec.agendabanda.controller;

import br.com.fatec.agendabanda.dto.CidadeDto;
import br.com.fatec.agendabanda.dto.CidadeRequest;
import br.com.fatec.agendabanda.security.TokenService;
import br.com.fatec.agendabanda.service.CidadeService;
import br.com.fatec.agendabanda.service.PermissaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cidades")
public class CidadeController {
    private final CidadeService service;
    private final TokenService tokenService;
    private final PermissaoService permissaoService;

    public CidadeController(CidadeService service, TokenService tokenService, PermissaoService permissaoService) {
        this.service = service;
        this.tokenService = tokenService;
        this.permissaoService = permissaoService;
    }

    @PostMapping
    public CidadeDto criar(@Valid @RequestBody CidadeRequest request, HttpServletRequest http) {
        permissaoService.exigirAdmin(tokenService.sessaoObrigatoria(http));
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public CidadeDto atualizar(@PathVariable @jakarta.validation.constraints.Positive Integer id, @Valid @RequestBody CidadeRequest request, HttpServletRequest http) {
        permissaoService.exigirAdmin(tokenService.sessaoObrigatoria(http));
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> excluir(@PathVariable @jakarta.validation.constraints.Positive Integer id, HttpServletRequest http) {
        permissaoService.exigirAdmin(tokenService.sessaoObrigatoria(http));
        service.excluir(id);
        return Map.of("mensagem", "Cidade excluída com sucesso.");
    }
}
