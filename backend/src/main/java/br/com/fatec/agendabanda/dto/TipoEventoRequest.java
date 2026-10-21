package br.com.fatec.agendabanda.dto;

import jakarta.validation.constraints.NotBlank;

public record TipoEventoRequest(
        @NotBlank String nome,
        String descricao,
        Boolean ativo
) {
}
