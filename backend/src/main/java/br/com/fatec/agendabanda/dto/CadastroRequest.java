package br.com.fatec.agendabanda.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroRequest(
        @NotBlank String nome,
        @NotBlank String cpf,
        @Email @NotBlank String email,
        @NotBlank String login,
        @NotBlank @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres.") String senha,
        @Size(max = 80) String instrumento,
        @Size(max = 500) String biografia
) {
}
