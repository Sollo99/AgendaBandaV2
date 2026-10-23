package br.com.fatec.agendabanda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record TipoEventoRequest(
        @NotBlank @Size(max = 50) String nome,
        @Size(max = 300) String descricao,
        @Pattern(regexp = "EVENTO|ENSAIO|SHOW|REUNIAO") String categoriaDetalhe,
        Boolean ativo
) {
}
