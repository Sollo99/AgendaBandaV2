package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.CidadeDto;
import br.com.fatec.agendabanda.dto.LocalDto;
import br.com.fatec.agendabanda.dto.LocalRequest;
import br.com.fatec.agendabanda.model.LocalEvento;
import br.com.fatec.agendabanda.repository.CidadeRepository;
import br.com.fatec.agendabanda.repository.LocalEventoRepository;
import br.com.fatec.agendabanda.repository.AgendaRepository;
import br.com.fatec.agendabanda.repository.AgendaSqlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LocalService {
    private final LocalEventoRepository localRepository;
    private final CidadeRepository cidadeRepository;
    private final MapperService mapper;
    private final AgendaRepository agendaRepository;
    private final AgendaSqlRepository agendaSql;

    public LocalService(LocalEventoRepository localRepository, CidadeRepository cidadeRepository, MapperService mapper, AgendaRepository agendaRepository, AgendaSqlRepository agendaSql) {
        this.localRepository = localRepository;
        this.cidadeRepository = cidadeRepository;
        this.mapper = mapper;
        this.agendaRepository = agendaRepository;
        this.agendaSql = agendaSql;
    }

    @Transactional(readOnly = true)
    public List<LocalDto> listarLocais() {
        return localRepository.findAll().stream().map(mapper::localDto).toList();
    }

    @Transactional(readOnly = true)
    public List<CidadeDto> listarCidades() {
        return cidadeRepository.findAllByOrderByNomeAscUfAsc().stream().map(mapper::cidadeDto).toList();
    }

    public LocalEvento buscarLocal(Integer id) {
        return localRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Local não encontrado."));
    }

    @Transactional
    public LocalDto criar(LocalRequest request) {
        var local = new LocalEvento();
        aplicar(local, request);
        return mapper.localDto(localRepository.save(local));
    }

    @Transactional
    public LocalDto atualizar(Integer id, LocalRequest request) {
        var local = buscarLocal(id);
        aplicar(local, request);
        return mapper.localDto(localRepository.save(local));
    }

    @Transactional
    public void excluir(Integer id) {
        agendaSql.bloquearLocal(id);
        var local = buscarLocal(id);
        if (agendaRepository.existsByLocalCodigoLocal(id)) {
            throw new IllegalArgumentException("O local está vinculado a eventos e não pode ser excluído. Altere o local desses eventos primeiro.");
        }
        localRepository.delete(local);
    }

    private void aplicar(LocalEvento local, LocalRequest request) {
        var cidade = cidadeRepository.findById(request.cidadeId()).orElseThrow(() -> new IllegalArgumentException("Cidade não encontrada."));
        local.setNome(request.nome().trim());
        local.setEndereco(request.endereco().trim());
        local.setComplemento(request.complemento());
        local.setCidade(cidade);
    }
}
