package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.TipoEventoDto;
import br.com.fatec.agendabanda.repository.TipoEventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TipoEventoService {
    private final TipoEventoRepository tipoEventoRepository;
    private final MapperService mapper;

    public TipoEventoService(TipoEventoRepository tipoEventoRepository, MapperService mapper) {
        this.tipoEventoRepository = tipoEventoRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<TipoEventoDto> listarAtivos() {
        return tipoEventoRepository.findByAtivoTrueOrderByNomeAsc().stream().map(mapper::tipoEventoDto).toList();
    }
}
