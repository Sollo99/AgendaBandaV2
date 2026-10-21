package br.com.fatec.agendabanda.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.List;

public record AgendaRequest(
        @NotBlank String titulo,
        String descricao,
        @NotNull LocalDateTime dataHoraInicio,
        @NotNull LocalDateTime dataHoraFim,
        @NotNull Integer localId,
        @NotNull Integer tipoEventoId,
        @Pattern(regexp = "AGENDADO|CANCELADO|REALIZADO", message = "Status do evento inválido.") String status,
        String repertorio,
        String observacao,
        String pauta,
        Integer cidadeId,
        List<@NotNull Integer> musicosIds
) {
}
