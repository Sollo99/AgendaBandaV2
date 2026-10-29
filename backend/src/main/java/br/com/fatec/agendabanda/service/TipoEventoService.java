package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.exception.RecursoNaoEncontradoException;

import br.com.fatec.agendabanda.exception.RegraNegocioException;

import br.com.fatec.agendabanda.dto.TipoEventoDto;
import br.com.fatec.agendabanda.dto.TipoEventoRequest;
import br.com.fatec.agendabanda.model.TipoEvento;
import br.com.fatec.agendabanda.repository.AgendaRepository;
import br.com.fatec.agendabanda.repository.TipoEventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class TipoEventoService {
    private final TipoEventoRepository tipoEventoRepository;
    private final AgendaRepository agendaRepository;
    private final MapperService mapper;

    public TipoEventoService(TipoEventoRepository tipoEventoRepository, AgendaRepository agendaRepository, MapperService mapper) {
        this.tipoEventoRepository = tipoEventoRepository;
        this.agendaRepository = agendaRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<TipoEventoDto> listarAtivos() {
        return tipoEventoRepository.findByAtivoTrueOrderByNomeAsc().stream().map(mapper::tipoEventoDto).toList();
    }

    @Transactional(readOnly = true)
    public List<TipoEventoDto> listarTodos() {
        return tipoEventoRepository.findAllByOrderByNomeAsc().stream().map(mapper::tipoEventoDto).toList();
    }

    @Transactional
    public TipoEventoDto criar(TipoEventoRequest request) {
        var tipo = new TipoEvento();
        aplicar(tipo, request, null);
        return mapper.tipoEventoDto(tipoEventoRepository.save(tipo));
    }

    @Transactional
    public TipoEventoDto atualizar(Integer id, TipoEventoRequest request) {
        var tipo = buscar(id);
        boolean tipoEmUso = agendaRepository.existsByTipoEventoCodigoTipo(id);
        if (tipoEmUso && !tipo.getNome().equalsIgnoreCase(request.nome().trim())) {
            throw new RegraNegocioException("Não é possível renomear um tipo já usado em eventos.");
        }
        if (tipoEmUso && Boolean.FALSE.equals(request.ativo())) {
            throw new RegraNegocioException("Não é possível desativar um tipo já usado em eventos.");
        }
        if (tipoEmUso && !tipo.getCategoriaDetalhe().equals(categoria(request.categoriaDetalhe()))) {
            throw new RegraNegocioException("Não é possível alterar a categoria de detalhe de um tipo já usado.");
        }
        aplicar(tipo, request, id);
        return mapper.tipoEventoDto(tipoEventoRepository.save(tipo));
    }

    @Transactional
    public void excluir(Integer id) {
        buscar(id);
        if (agendaRepository.existsByTipoEventoCodigoTipo(id)) {
            throw new RegraNegocioException("O tipo está vinculado a eventos e não pode ser excluído.");
        }
        tipoEventoRepository.deleteById(id);
    }

    private TipoEvento buscar(Integer id) {
        return tipoEventoRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de evento não encontrado."));
    }

    private void aplicar(TipoEvento tipo, TipoEventoRequest request, Integer idAtual) {
        String nome = request.nome().trim().toUpperCase(Locale.ROOT);
        tipoEventoRepository.findByNomeIgnoreCase(nome)
                .filter(existente -> !existente.getCodigoTipo().equals(idAtual))
                .ifPresent(existente -> { throw new RegraNegocioException("Tipo de evento já cadastrado."); });
        tipo.setNome(nome);
        tipo.setDescricao(request.descricao() == null || request.descricao().isBlank() ? null : request.descricao().trim());
        tipo.setCategoriaDetalhe(categoria(request.categoriaDetalhe()));
        tipo.setAtivo(request.ativo() == null || request.ativo());
    }

    private String categoria(String valor) {
        String categoria = valor == null || valor.isBlank() ? "EVENTO" : valor.trim().toUpperCase(Locale.ROOT);
        if (!List.of("EVENTO", "ENSAIO", "SHOW", "REUNIAO").contains(categoria)) {
            throw new RegraNegocioException("Categoria de detalhe inválida.");
        }
        return categoria;
    }
}
