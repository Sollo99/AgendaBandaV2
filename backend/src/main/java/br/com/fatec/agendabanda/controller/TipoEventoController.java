package br.com.fatec.agendabanda.controller;

import br.com.fatec.agendabanda.dto.TipoEventoDto;
import br.com.fatec.agendabanda.service.TipoEventoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-evento")
public class TipoEventoController {
    private final TipoEventoService service;

    public TipoEventoController(TipoEventoService service) {
        this.service = service;
    }

    @GetMapping
    public List<TipoEventoDto> listar() {
        return service.listarAtivos();
    }
}
