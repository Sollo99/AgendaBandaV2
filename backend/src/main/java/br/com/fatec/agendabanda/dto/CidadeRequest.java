package br.com.fatec.agendabanda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CidadeRequest(
        @NotBlank String nome,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "UF deve conter duas letras.") String uf
) {
}
