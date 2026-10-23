package br.com.fatec.agendabanda.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final LimitadorRecuperacao limitadorRecuperacao;

    public AuthFilter(TokenService tokenService, LimitadorRecuperacao limitadorRecuperacao) {
        this.tokenService = tokenService;
        this.limitadorRecuperacao = limitadorRecuperacao;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if ("POST".equalsIgnoreCase(request.getMethod())
                && (path.equals("/api/auth/recuperar-senha") || path.equals("/api/auth/redefinir-senha"))) {
            long espera = limitadorRecuperacao.segundosDeEspera(request.getRemoteAddr());
            if (espera > 0) {
                response.setStatus(429);
                response.setHeader("Retry-After", Long.toString(espera));
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"erro\":\"Muitas tentativas de recuperação. Aguarde alguns minutos e tente novamente.\"}");
                return;
            }
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || !path.startsWith("/api/") ||
                path.equals("/api/auth/login") || path.equals("/api/auth/cadastro") ||
                path.equals("/api/auth/recuperar-senha") || path.equals("/api/auth/redefinir-senha")) {
            filterChain.doFilter(request, response);
            return;
        }

        var sessao = tokenService.buscarSessao(tokenService.extrairToken(request));
        if (sessao.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"erro\":\"Acesso não autenticado.\"}");
            return;
        }

        request.setAttribute("sessao", sessao.get());
        filterChain.doFilter(request, response);
    }
}
