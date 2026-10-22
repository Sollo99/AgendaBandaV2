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
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String COOKIE_RECUPERACAO = "agendabanda_recuperacao";
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
    public RecuperarSenhaResponse recuperarSenha(@Valid @RequestBody RecuperarSenhaRequest request,
                                                 @CookieValue(name = COOKIE_RECUPERACAO, required = false) String cookie,
                                                 HttpServletRequest http, HttpServletResponse response) {
        String chave = request.chaveSolicitacao() != null ? request.chaveSolicitacao() : cookie;
        if (chave == null || !chave.matches("[A-Za-z0-9_-]{32,128}")) {
            byte[] bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            chave = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }
        var resultado = authService.solicitarRedefinicaoSenha(new RecuperarSenhaRequest(request.identificador(), chave));
        if (resultado.codigoRecuperacao() != null) {
            salvarCookie(response, http.isSecure(), chave, Duration.ofDays(7));
        }
        return resultado;
    }

    @PostMapping("/redefinir-senha")
    public Map<String, String> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request,
                                            @CookieValue(name = COOKIE_RECUPERACAO, required = false) String cookie,
                                            HttpServletRequest http, HttpServletResponse response) {
        String chave = request.chaveSolicitacao() != null ? request.chaveSolicitacao() : cookie;
        if (chave == null || chave.isBlank()) throw new IllegalArgumentException("Informe o código de recuperação ou use o navegador da solicitação.");
        authService.redefinirSenha(new RedefinirSenhaRequest(request.codigoSolicitacao(), chave, request.novaSenha()));
        salvarCookie(response, http.isSecure(), "", Duration.ZERO);
        return Map.of("mensagem", "Senha redefinida com sucesso. Faça login com a nova senha.");
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        tokenService.removerToken(tokenService.extrairToken(request));
        return Map.of("mensagem", "Logout realizado com sucesso.");
    }

    private void salvarCookie(HttpServletResponse response, boolean seguro, String chave, Duration duracao) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(COOKIE_RECUPERACAO, chave)
                .httpOnly(true).secure(seguro).sameSite("Lax").path("/api/auth").maxAge(duracao).build().toString());
    }
}
