package br.com.fatec.agendabanda.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;

@Component
public class SegurancaHttp {
    public static final String COOKIE_SESSAO = "agendabanda_sessao";
    private static final String COOKIE_CSRF = "agendabanda_csrf";
    @Value("${app.auth.cookie.secure:false}") private boolean exigirSecure;
    @Value("${app.cors.allowed-origins}") private String origensPermitidas;
    private final SecureRandom random = new SecureRandom();

    public static String cookie(HttpServletRequest request, String nome) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies()).filter(c -> nome.equals(c.getName()))
                .map(jakarta.servlet.http.Cookie::getValue).findFirst().orElse(null);
    }

    public void abrirSessao(HttpServletRequest request, HttpServletResponse response, String token) {
        salvar(request, response, COOKIE_SESSAO, token, Duration.ofHours(8));
        salvar(request, response, COOKIE_CSRF, "", Duration.ZERO);
    }

    public void fecharSessao(HttpServletRequest request, HttpServletResponse response) {
        salvar(request, response, COOKIE_SESSAO, "", Duration.ZERO);
        salvar(request, response, COOKIE_CSRF, "", Duration.ZERO);
    }

    public String fornecerCsrf(HttpServletRequest request, HttpServletResponse response) {
        String sessao = cookie(request, COOKIE_SESSAO);
        if (sessao != null && !sessao.isBlank()) return csrfDaSessao(sessao);
        String nonce = cookie(request, COOKIE_CSRF);
        if (nonce == null || !nonce.matches("[A-Za-z0-9_-]{43}")) {
            nonce = gerarSegredo();
            salvar(request, response, COOKIE_CSRF, nonce, Duration.ofHours(8));
        }
        return nonce;
    }

    public boolean csrfValido(HttpServletRequest request) {
        String origem = request.getHeader("Origin");
        if (origem != null && Arrays.stream(origensPermitidas.split(",")).map(String::trim).noneMatch(origem::equals)) return false;
        String enviado = request.getHeader("X-CSRF-Token");
        String sessao = cookie(request, COOKIE_SESSAO);
        String esperado = sessao == null || sessao.isBlank() ? cookie(request, COOKIE_CSRF) : csrfDaSessao(sessao);
        return enviado != null && esperado != null && MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8), enviado.getBytes(StandardCharsets.UTF_8));
    }

    public String gerarSegredo() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String csrfDaSessao(String token) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(("csrf:" + token).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    private void salvar(HttpServletRequest request, HttpServletResponse response, String nome, String valor, Duration prazo) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(nome, valor)
                .httpOnly(true).secure(exigirSecure || request.isSecure()).sameSite("Strict")
                .path("/api").maxAge(prazo).build().toString());
    }
}
