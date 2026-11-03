package br.com.fatec.agendabanda.security;

import br.com.fatec.agendabanda.dto.ErroApi;
import br.com.fatec.agendabanda.service.BackupManutencao;
import br.com.fatec.agendabanda.service.BackupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/** Antes da autenticacao: protege tambem consultas de sessao durante o RESTORE. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE+20)
public class BackupManutencaoFilter extends OncePerRequestFilter {
    private static final Pattern OPERACAO=Pattern.compile("/api/backups/operacoes/([0-9a-fA-F-]{36})");
    private final BackupManutencao manutencao;
    private final BackupService backups;
    private final ObjectMapper json;
    public BackupManutencaoFilter(BackupManutencao manutencao,BackupService backups,ObjectMapper json) {
        this.manutencao=manutencao;this.backups=backups;this.json=json;
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        String caminho=request.getRequestURI().substring(request.getContextPath().length());
        // As rotas da API nao possuem nomes livres no caminho. Evita diferencas de interpretacao com o MVC.
        if (caminho.indexOf('%')>=0 || caminho.indexOf(';')>=0 || caminho.indexOf('\\')>=0 || caminho.contains("//")) {
            ErroApi.escrever(response,400,"CAMINHO_INVALIDO","Caminho de requisição inválido.");return;
        }
        if (!caminho.startsWith("/api/") || "OPTIONS".equals(request.getMethod())) { chain.doFilter(request,response);return; }
        response.setHeader("Cache-Control","no-store");
        String credencial=request.getHeader("X-Backup-Acompanhamento");
        if (credencial!=null) {
            var correspondencia=OPERACAO.matcher(caminho);
            UUID id=null;
            if ("GET".equals(request.getMethod()) && correspondencia.matches())
                try { id=UUID.fromString(correspondencia.group(1)); } catch(IllegalArgumentException ignored) {}
            if (id==null || !backups.podeAcompanhar(id,credencial)) {
                ErroApi.escrever(response,401,"ACOMPANHAMENTO_INVALIDO","O acompanhamento expirou. Entre novamente para consultar o histórico.");return;
            }
            response.setContentType("application/json;charset=UTF-8");
            json.writeValue(response.getWriter(),backups.progresso(id));return;
        }
        try(var permissao=manutencao.tentarLeitura()) {
            if (permissao==null) {
                response.setHeader("Retry-After","5");
                ErroApi.escrever(response,503,"BANCO_EM_MANUTENCAO","O banco está em manutenção para restauração. Aguarde e tente novamente.");return;
            }
            chain.doFilter(request,response);
        }
    }
}
