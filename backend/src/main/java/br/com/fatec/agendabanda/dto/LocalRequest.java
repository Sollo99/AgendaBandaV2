package br.com.fatec.agendabanda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;

public record LocalRequest(@NotBlank @Size(max = 120) String nome,
                           @NotBlank @Size(max = 180) String endereco,
                           @Size(max = 120) String complemento,
                           @NotNull @Positive Integer cidadeId) {
}
