package br.com.fatec.agendabanda.controller;

import br.com.fatec.agendabanda.dto.AuthDtos.LoginRequest;
import br.com.fatec.agendabanda.dto.AuthDtos.SessaoResponse;
import br.com.fatec.agendabanda.dto.AuthDtos.RecuperarSenhaRequest;
import br.com.fatec.agendabanda.dto.AuthDtos.RecuperarSenhaResponse;
import br.com.fatec.agendabanda.dto.AuthDtos.RedefinirSenhaRequest;
import br.com.fatec.agendabanda.dto.CadastroRequest;
import br.com.fatec.agendabanda.security.TokenService;
import br.com.fatec.agendabanda.security.SegurancaHttp;
import br.com.fatec.agendabanda.service.AuthService;
import br.com.fatec.agendabanda.service.CadastroPublicoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final SegurancaHttp seguranca;
    private final AuthService authService;
    private final TokenService tokenService;
    private final CadastroPublicoService cadastroPublicoService;

    public AuthController(AuthService authService, TokenService tokenService, CadastroPublicoService cadastroPublicoService, SegurancaHttp seguranca) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.cadastroPublicoService = cadastroPublicoService;
        this.seguranca = seguranca;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(HttpServletRequest http, HttpServletResponse response) {
        return Map.of("token", seguranca.fornecerCsrf(http, response));
    }

    @PostMapping("/login")
    public SessaoResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http, HttpServletResponse response) {
        var resultado = authService.login(request, http.getRemoteAddr());
        seguranca.abrirSessao(http, response, resultado.token());
        return new SessaoResponse(resultado.usuario());
    }

    @PostMapping("/cadastro")
    public ResponseEntity<Map<String, String>> cadastro(@Valid @RequestBody CadastroRequest request, HttpServletRequest http) {
        cadastroPublicoService.solicitar(request, http.getRemoteAddr());
        return ResponseEntity.accepted().body(Map.of("mensagem", "Se os dados forem válidos e ainda não estiverem cadastrados, a solicitação será analisada por um administrador. "
                + "CPF, e-mail ou login já utilizados impedem um novo cadastro. Caso já tenha conta, tente entrar ou recuperar a senha. "
                + "Se seu cadastro foi recusado ou você não conseguir prosseguir, contate um administrador para revisar o pedido e, quando cabível, liberar os dados para novo cadastro."));
    }

    @PostMapping("/recuperar-senha")
    public RecuperarSenhaResponse recuperarSenha(@Valid @RequestBody RecuperarSenhaRequest request, HttpServletRequest http) {
        return authService.solicitarRedefinicaoSenha(request, http.getRemoteAddr());
    }

    @PostMapping("/redefinir-senha")
    public Map<String, String> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request, HttpServletRequest http) {
        authService.redefinirSenha(request, http.getRemoteAddr());
        return Map.of("mensagem", "Senha redefinida com sucesso. Faca login com a nova senha.");
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(tokenService.extrairToken(request), tokenService.sessaoObrigatoria(request));
        seguranca.fecharSessao(request, response);
        return Map.of("mensagem", "Logout realizado com sucesso.");
    }
}
