package br.com.fatec.agendabanda.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AgendaDto(
        Integer codigoAgenda,
        String titulo,
        String descricao,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        LocalDto local,
        MembroResumoDto criador,
        TipoEventoDto tipoEvento,
        String status,
        String tipoEspecifico,
        String repertorio,
        String observacao,
        String pauta,
        CidadeDto cidadeShow,
        List<MusicoResumoDto> musicos,
        List<ParticipacaoDto> participacoes,
        Integer totalParticipantes,
        Integer totalConfirmados
) {
    public AgendaDto comParticipacoes(List<ParticipacaoDto> lista) {
        return new AgendaDto(codigoAgenda, titulo, descricao, dataHoraInicio, dataHoraFim, local, criador,
                tipoEvento, status, tipoEspecifico, repertorio, observacao, pauta, cidadeShow,
                lista.stream().map(ParticipacaoDto::musico).toList(), lista, totalParticipantes, totalConfirmados);
    }
}
