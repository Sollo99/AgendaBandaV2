package br.com.fatec.agendabanda.controller;

import br.com.fatec.agendabanda.dto.TipoEventoDto;
import br.com.fatec.agendabanda.dto.TipoEventoRequest;
import br.com.fatec.agendabanda.security.TokenService;
import br.com.fatec.agendabanda.service.PermissaoService;
import br.com.fatec.agendabanda.service.TipoEventoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tipos-evento")
public class TipoEventoController {
    private final TipoEventoService service;
    private final TokenService tokenService;
    private final PermissaoService permissaoService;

    public TipoEventoController(TipoEventoService service, TokenService tokenService, PermissaoService permissaoService) {
        this.service = service;
        this.tokenService = tokenService;
        this.permissaoService = permissaoService;
    }

    @GetMapping
    public List<TipoEventoDto> listar() {
        return service.listarAtivos();
    }

    @GetMapping("/admin")
    public List<TipoEventoDto> listarTodos(HttpServletRequest http) {
        permissaoService.exigirAdmin(tokenService.sessaoObrigatoria(http));
        return service.listarTodos();
    }

    @PostMapping
    public TipoEventoDto criar(@Valid @RequestBody TipoEventoRequest request, HttpServletRequest http) {
        permissaoService.exigirAdmin(tokenService.sessaoObrigatoria(http));
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public TipoEventoDto atualizar(@PathVariable Integer id, @Valid @RequestBody TipoEventoRequest request, HttpServletRequest http) {
        permissaoService.exigirAdmin(tokenService.sessaoObrigatoria(http));
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> excluir(@PathVariable Integer id, HttpServletRequest http) {
        permissaoService.exigirAdmin(tokenService.sessaoObrigatoria(http));
        service.excluir(id);
        return Map.of("mensagem", "Tipo de evento excluído com sucesso.");
    }
}
