package br.com.fatec.agendabanda.controller;

import br.com.fatec.agendabanda.dto.MembroDto;
import br.com.fatec.agendabanda.dto.PerfilRequest;
import br.com.fatec.agendabanda.security.TokenService;
import br.com.fatec.agendabanda.service.PerfilPessoalService;
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
    private final PerfilPessoalService perfilService;
    private final TokenService tokenService;

    public PerfilController(PerfilPessoalService perfilService, TokenService tokenService) {
        this.perfilService = perfilService;
        this.tokenService = tokenService;
    }

    @GetMapping
    public MembroDto obter(HttpServletRequest http) {
        return perfilService.obterProprio(tokenService.sessaoObrigatoria(http).codigo());
    }

    @PutMapping
    public MembroDto atualizar(@Valid @RequestBody PerfilRequest request, HttpServletRequest http) {
        return perfilService.atualizarProprio(tokenService.sessaoObrigatoria(http).codigo(), request);
    }
}
