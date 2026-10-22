package br.com.fatec.agendabanda.controller;

import br.com.fatec.agendabanda.dto.AuthDtos.LoginRequest;
import br.com.fatec.agendabanda.dto.AuthDtos.LoginResponse;
import br.com.fatec.agendabanda.dto.AuthDtos.RecuperarSenhaRequest;
import br.com.fatec.agendabanda.dto.AuthDtos.RecuperarSenhaResponse;
import br.com.fatec.agendabanda.dto.AuthDtos.RedefinirSenhaRequest;
import br.com.fatec.agendabanda.dto.CadastroRequest;
import br.com.fatec.agendabanda.dto.MembroDto;
import br.com.fatec.agendabanda.security.TokenService;
import br.com.fatec.agendabanda.service.AuthService;
import br.com.fatec.agendabanda.service.MembroService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final TokenService tokenService;
    private final MembroService membroService;

    public AuthController(AuthService authService, TokenService tokenService, MembroService membroService) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.membroService = membroService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/cadastro")
    public MembroDto cadastro(@Valid @RequestBody CadastroRequest request) {
        return membroService.criarCadastro(request);
    }

    @PostMapping("/recuperar-senha")
    public RecuperarSenhaResponse recuperarSenha(@Valid @RequestBody RecuperarSenhaRequest request) {
        return authService.solicitarRedefinicaoSenha(request);
    }

    @PostMapping("/redefinir-senha")
    public Map<String, String> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        authService.redefinirSenha(request);
        return Map.of("mensagem", "Senha redefinida com sucesso. Faça login com a nova senha.");
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        tokenService.removerToken(tokenService.extrairToken(request));
        return Map.of("mensagem", "Logout realizado com sucesso.");
    }
}
