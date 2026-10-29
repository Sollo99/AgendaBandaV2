package br.com.fatec.agendabanda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

public record AgendaRequest(
        @NotBlank @Size(max = 120) String titulo,
        @Size(max = 800) String descricao,
        @NotNull LocalDateTime dataHoraInicio,
        @NotNull LocalDateTime dataHoraFim,
        @NotNull @Positive Integer localId,
        @NotNull @Positive Integer tipoEventoId,
        @Pattern(regexp = "AGENDADO|CANCELADO|REALIZADO", message = "Status do evento inválido.") String status,
        @Size(max = 800) String repertorio,
        @Size(max = 500) String observacao,
        @Size(max = 800) String pauta,
        @Positive Integer cidadeId,
        @Size(max = 500) List<@NotNull @Positive Integer> musicosIds
) {
}
