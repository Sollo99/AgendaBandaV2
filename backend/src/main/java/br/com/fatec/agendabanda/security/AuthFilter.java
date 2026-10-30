package br.com.fatec.agendabanda.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import br.com.fatec.agendabanda.dto.ErroApi;
import br.com.fatec.agendabanda.service.AuditoriaAutenticacaoService;
import br.com.fatec.agendabanda.security.EventoAutenticacao.Motivo;

@Component
public class AuthFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final LimitadorAutenticacao limitador;
    private final SegurancaHttp seguranca;
    private final AuditoriaAutenticacaoService auditoria;

    public AuthFilter(TokenService tokenService, LimitadorAutenticacao limitador, SegurancaHttp seguranca, AuditoriaAutenticacaoService auditoria) {
        this.tokenService = tokenService;
        this.limitador = limitador;
        this.seguranca = seguranca;
        this.auditoria = auditoria;
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
                auditoria.negar(null, Motivo.CSRF_INVALIDO, EventoAutenticacao.recursoDaRota(path));
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
                auditoria.limitar(EventoAutenticacao.recursoDaRota(path));
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
        String token = tokenService.extrairToken(request);
        try {
            sessao = tokenService.buscarSessao(token);
        } catch (org.springframework.dao.DataAccessException ex) {
            org.slf4j.LoggerFactory.getLogger(AuthFilter.class).error("Falha ao consultar sessao.", ex);
            ErroApi.escrever(response, 500, "ERRO_INTERNO", "Não foi possível verificar a sessão. Tente novamente mais tarde.");
            return;
        }
        if (sessao.isEmpty()) {
            // A visita anonima inicial consulta /me sem cookie; isso nao e uma tentativa de login.
            if (!path.equals("/api/membros/me") || (token != null && !token.isBlank())) {
                auditoria.negar(null, Motivo.SEM_SESSAO, EventoAutenticacao.recursoDaRota(path));
            }
            ErroApi.escrever(response, 401, "NAO_AUTENTICADO", "Acesso não autenticado.");
            return;
        }

        request.setAttribute("sessao", sessao.get());
        filterChain.doFilter(request, response);
    }
}
