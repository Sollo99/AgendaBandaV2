package br.com.fatec.agendabanda.controller;

import br.com.fatec.agendabanda.dto.RelatorioDtos.*;
import br.com.fatec.agendabanda.security.TokenService;
import br.com.fatec.agendabanda.service.PermissaoService;
import br.com.fatec.agendabanda.service.RelatorioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {
    private final RelatorioService service;
    private final TokenService tokens;
    private final PermissaoService permissoes;
    public RelatorioController(RelatorioService service, TokenService tokens, PermissaoService permissoes) {
        this.service = service; this.tokens = tokens; this.permissoes = permissoes;
    }

    @GetMapping
    public Relatorio consultar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                               HttpServletRequest request) {
        permissoes.exigirAdmin(tokens.sessaoObrigatoria(request));
        return service.consultar(inicio, fim);
    }

    @GetMapping("/historicos")
    public List<Historico> historicos(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                    HttpServletRequest request) {
        permissoes.exigirAdmin(tokens.sessaoObrigatoria(request));
        return service.historicos(inicio, fim);
    }
}
