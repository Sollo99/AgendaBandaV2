package br.com.fatec.agendabanda.controller;

import br.com.fatec.agendabanda.dto.MembroDto;
import br.com.fatec.agendabanda.dto.PerfilRequest;
import br.com.fatec.agendabanda.security.TokenService;
import br.com.fatec.agendabanda.service.MembroService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/membros/me")
public class PerfilController {
    private final MembroService membroService;
    private final TokenService tokenService;

    public PerfilController(MembroService membroService, TokenService tokenService) {
        this.membroService = membroService;
        this.tokenService = tokenService;
    }

    @GetMapping
    public MembroDto obter(HttpServletRequest http) {
        return membroService.obterProprio(tokenService.sessaoObrigatoria(http).codigo());
    }

    @PutMapping
    public MembroDto atualizar(@Valid @RequestBody PerfilRequest request, HttpServletRequest http) {
        return membroService.atualizarProprio(tokenService.sessaoObrigatoria(http).codigo(), request);
    }
}
