package br.com.fatec.agendabanda.controller;

import br.com.fatec.agendabanda.dto.AuthDtos.DecisaoRedefinicaoSenhaRequest;
import br.com.fatec.agendabanda.dto.AuthDtos.SolicitacaoRedefinicaoSenhaDto;
import br.com.fatec.agendabanda.security.TokenService;
import br.com.fatec.agendabanda.service.AuthService;
import br.com.fatec.agendabanda.service.PermissaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/redefinicoes-senha")
public class RedefinicaoSenhaController {
    private final AuthService authService;
    private final TokenService tokenService;
    private final PermissaoService permissaoService;

    public RedefinicaoSenhaController(AuthService authService, TokenService tokenService, PermissaoService permissaoService) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.permissaoService = permissaoService;
    }

    @GetMapping("/pendentes")
    public List<SolicitacaoRedefinicaoSenhaDto> listarPendentes(HttpServletRequest http) {
        permissaoService.exigirAdmin(tokenService.sessaoObrigatoria(http));
        return authService.listarSolicitacoesPendentes();
    }

    @PatchMapping("/{id}")
    public Map<String, String> decidir(@PathVariable Integer id, @Valid @RequestBody DecisaoRedefinicaoSenhaRequest request,
                                        HttpServletRequest http) {
        var sessao = tokenService.sessaoObrigatoria(http);
        permissaoService.exigirAdmin(sessao);
        authService.decidirSolicitacao(id, request.aprovada(), sessao.codigo());
        return Map.of("mensagem", request.aprovada() ? "Solicitação aprovada." : "Solicitação recusada.");
    }
}
