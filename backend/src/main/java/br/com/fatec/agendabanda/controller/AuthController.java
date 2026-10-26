package br.com.fatec.agendabanda.controller;

import br.com.fatec.agendabanda.dto.AuthDtos.LoginRequest;
import br.com.fatec.agendabanda.dto.AuthDtos.LoginResponse;
import br.com.fatec.agendabanda.dto.AuthDtos.RecuperarSenhaRequest;
import br.com.fatec.agendabanda.dto.AuthDtos.RecuperarSenhaResponse;
import br.com.fatec.agendabanda.dto.AuthDtos.RedefinirSenhaRequest;
import br.com.fatec.agendabanda.dto.CadastroRequest;
import br.com.fatec.agendabanda.security.TokenService;
import br.com.fatec.agendabanda.service.AuthService;
import br.com.fatec.agendabanda.service.CadastroPublicoService;
import br.com.fatec.agendabanda.exception.RecuperacaoExpiradaException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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
    private final CadastroPublicoService cadastroPublicoService;

    public AuthController(AuthService authService, TokenService tokenService, CadastroPublicoService cadastroPublicoService) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.cadastroPublicoService = cadastroPublicoService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/cadastro")
    public ResponseEntity<Map<String, String>> cadastro(@Valid @RequestBody CadastroRequest request) {
        cadastroPublicoService.solicitar(request);
        return ResponseEntity.accepted().body(Map.of("mensagem", "Se os dados forem válidos e ainda não estiverem cadastrados, a solicitação será analisada por um administrador. "
                + "CPF, e-mail ou login já utilizados impedem um novo cadastro. Caso já tenha conta, tente entrar ou recuperar a senha. "
                + "Se seu cadastro foi recusado ou você não conseguir prosseguir, contate um administrador para revisar o pedido e, quando cabível, liberar os dados para novo cadastro."));
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
        try {
            authService.redefinirSenha(new RedefinirSenhaRequest(request.codigoSolicitacao(), chave, request.novaSenha()));
        } catch (RecuperacaoExpiradaException ex) {
            salvarCookie(response, http.isSecure(), "", Duration.ZERO);
            throw ex;
        }
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
