package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.*;
import br.com.fatec.agendabanda.model.*;
import br.com.fatec.agendabanda.repository.MusicoRepository;
import br.com.fatec.agendabanda.repository.ProdutorRepository;
import org.springframework.stereotype.Service;

@Service
public class MapperService {
    private final MusicoRepository musicoRepository;
    private final ProdutorRepository produtorRepository;

    public MapperService(MusicoRepository musicoRepository, ProdutorRepository produtorRepository) {
        this.musicoRepository = musicoRepository;
        this.produtorRepository = produtorRepository;
    }

    public MembroDto membroDto(Membro membro) {
        var musico = musicoRepository.findById(membro.getCodigo()).orElse(null);
        var produtor = produtorRepository.findById(membro.getCodigo()).orElse(null);
        String perfil = membro.getPerfil();
        return new MembroDto(
                membro.getCodigo(), membro.getNome(), membro.getCpf(), membro.getEmail(), membro.getLogin(),
                membro.getAdministrador(), membro.getAtivo(), perfil,
                musico != null ? musico.getInstrumento() : null,
                musico != null ? musico.getBiografia() : null,
                produtor != null ? produtor.getEspecialidade() : null,
                membro.getStatusCadastro()
        );
    }

    public CidadeDto cidadeDto(Cidade cidade) {
        if (cidade == null) return null;
        return new CidadeDto(cidade.getCodigoCidade(), cidade.getNome(), cidade.getUf());
    }

    public MembroResumoDto membroResumoDto(Membro membro) {
        return new MembroResumoDto(membro.getCodigo(), membro.getNome());
    }

    public MusicoResumoDto musicoResumoDto(Musico musico) {
        return new MusicoResumoDto(musico.getCodigo(), musico.getMembro().getNome(), musico.getInstrumento());
    }

    public LocalDto localDto(LocalEvento local) {
        if (local == null) return null;
        var cidade = local.getCidade();
        return new LocalDto(local.getCodigoLocal(), local.getNome(), local.getEndereco(), local.getComplemento(),
                cidade.getCodigoCidade(), cidade.getNome(), cidade.getUf());
    }

    public TipoEventoDto tipoEventoDto(TipoEvento tipoEvento) {
        if (tipoEvento == null) return null;
        return new TipoEventoDto(tipoEvento.getCodigoTipo(), tipoEvento.getNome(), tipoEvento.getDescricao(), tipoEvento.getCategoriaDetalhe(), tipoEvento.getAtivo());
    }

    public ParticipacaoDto participacaoDto(ParticipacaoEvento participacao) {
        return new ParticipacaoDto(
                participacao.getAgenda().getCodigoAgenda(),
                musicoResumoDto(participacao.getMusico()),
                participacao.getStatusConfirmacao(),
                participacao.getPresente(),
                participacao.getDataResposta(),
                participacao.getObservacao()
        );
    }

}
