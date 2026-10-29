package br.com.fatec.agendabanda.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import br.com.fatec.agendabanda.dto.ErroApi;

@Component
public class AuthFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final LimitadorAutenticacao limitador;
    private final SegurancaHttp seguranca;

    public AuthFilter(TokenService tokenService, LimitadorAutenticacao limitador, SegurancaHttp seguranca) {
        this.tokenService = tokenService;
        this.limitador = limitador;
        this.seguranca = seguranca;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
            String metodo = request.getMethod();
            boolean segura = "GET".equals(metodo) || "HEAD".equals(metodo) || "OPTIONS".equals(metodo);
            if (!segura && !seguranca.csrfValido(request)) {
                ErroApi.escrever(response, 403, "CSRF_INVALIDO", "Recarregue a página e tente novamente.");
                return;
            }
        }
        var tipo = switch (path) {
            case "/api/auth/login" -> LimitadorAutenticacao.Tipo.LOGIN;
            case "/api/auth/cadastro" -> LimitadorAutenticacao.Tipo.CADASTRO;
            case "/api/auth/recuperar-senha", "/api/auth/redefinir-senha" -> LimitadorAutenticacao.Tipo.RECUPERACAO;
            default -> null;
        };
        if ("POST".equalsIgnoreCase(request.getMethod()) && tipo != null) {
            long espera = limitador.segundosDeEspera(tipo, request.getRemoteAddr());
            if (espera > 0) {
                response.setHeader("Retry-After", Long.toString(espera));
                ErroApi.escrever(response, 429, "LIMITE_REQUISICOES", "Muitas tentativas. Aguarde e tente novamente.");
                return;
            }
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || !path.startsWith("/api/") ||
                path.equals("/api/auth/csrf") || path.equals("/api/auth/login") || path.equals("/api/auth/cadastro") ||
                path.equals("/api/auth/recuperar-senha") || path.equals("/api/auth/redefinir-senha")) {
            filterChain.doFilter(request, response);
            return;
        }

        java.util.Optional<SessaoUsuario> sessao;
        try {
            sessao = tokenService.buscarSessao(tokenService.extrairToken(request));
        } catch (org.springframework.dao.DataAccessException ex) {
            org.slf4j.LoggerFactory.getLogger(AuthFilter.class).error("Falha ao consultar sessao.", ex);
            ErroApi.escrever(response, 500, "ERRO_INTERNO", "Não foi possível verificar a sessão. Tente novamente mais tarde.");
            return;
        }
        if (sessao.isEmpty()) {
            ErroApi.escrever(response, 401, "NAO_AUTENTICADO", "Acesso não autenticado.");
            return;
        }

        request.setAttribute("sessao", sessao.get());
        filterChain.doFilter(request, response);
    }
}
