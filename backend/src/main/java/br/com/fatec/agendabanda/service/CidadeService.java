package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.exception.RecursoNaoEncontradoException;

import br.com.fatec.agendabanda.exception.RegraNegocioException;

import br.com.fatec.agendabanda.dto.CidadeDto;
import br.com.fatec.agendabanda.dto.CidadeRequest;
import br.com.fatec.agendabanda.model.Cidade;
import br.com.fatec.agendabanda.repository.CidadeRepository;
import br.com.fatec.agendabanda.repository.LocalEventoRepository;
import br.com.fatec.agendabanda.repository.ShowEventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CidadeService {
    private final CidadeRepository cidadeRepository;
    private final LocalEventoRepository localRepository;
    private final ShowEventoRepository showRepository;
    private final MapperService mapper;
    private final AuditoriaContexto auditoria;

    public CidadeService(CidadeRepository cidadeRepository, LocalEventoRepository localRepository, ShowEventoRepository showRepository, MapperService mapper, AuditoriaContexto auditoria) {
        this.cidadeRepository = cidadeRepository;
        this.localRepository = localRepository;
        this.showRepository = showRepository;
        this.mapper = mapper;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<CidadeDto> listar() {
        return cidadeRepository.findAllByOrderByNomeAscUfAsc().stream().map(mapper::cidadeDto).toList();
    }

    @Transactional
    public CidadeDto criar(CidadeRequest request) {
        auditoria.prepararDoRequest();
        var cidade = new Cidade();
        aplicar(cidade, request, null);
        return mapper.cidadeDto(cidadeRepository.save(cidade));
    }

    @Transactional
    public CidadeDto atualizar(Integer id, CidadeRequest request) {
        auditoria.prepararDoRequest();
        var cidade = buscar(id);
        aplicar(cidade, request, id);
        return mapper.cidadeDto(cidadeRepository.save(cidade));
    }

    @Transactional
    public void excluir(Integer id) {
        auditoria.prepararDoRequest();
        buscar(id);
        if (localRepository.existsByCidadeCodigoCidade(id) || showRepository.existsByCidadeCodigoCidade(id)) {
            throw new RegraNegocioException("A cidade está vinculada a locais ou shows e não pode ser excluída.");
        }
        cidadeRepository.deleteById(id);
    }

    private Cidade buscar(Integer id) {
        return cidadeRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Cidade não encontrada."));
    }

    private void aplicar(Cidade cidade, CidadeRequest request, Integer idAtual) {
        String nome = request.nome().trim();
        String uf = request.uf().trim().toUpperCase(Locale.ROOT);
        cidadeRepository.findByNomeIgnoreCaseAndUfIgnoreCase(nome, uf)
                .filter(existente -> !existente.getCodigoCidade().equals(idAtual))
                .ifPresent(existente -> { throw new RegraNegocioException("Cidade já cadastrada para esta UF."); });
        cidade.setNome(nome);
        cidade.setUf(uf);
    }
}
