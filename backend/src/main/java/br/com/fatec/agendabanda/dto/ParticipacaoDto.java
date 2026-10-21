package br.com.fatec.agendabanda.dto;

import java.time.LocalDateTime;

public record ParticipacaoDto(
        Integer codigoAgenda,
        MembroDto musico,
        String statusConfirmacao,
        Boolean presente,
        LocalDateTime dataResposta,
        String observacao
) {
}
