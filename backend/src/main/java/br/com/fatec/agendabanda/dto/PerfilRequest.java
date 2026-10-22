package br.com.fatec.agendabanda.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerfilRequest(
        @NotBlank String nome,
        @NotBlank String cpf,
        @Email @NotBlank String email,
        @NotBlank String login,
        String senhaAtual,
        @Size(min = 6, message = "A nova senha deve ter pelo menos 6 caracteres.") String novaSenha,
        String instrumento,
        String biografia
) {
}
