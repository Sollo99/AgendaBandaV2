package br.com.fatec.agendabanda.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.br.CPF;
import br.com.fatec.agendabanda.validation.SenhaValida;

public record MembroRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @CPF @Pattern(regexp = "[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2}", message = "Informe um CPF válido, com ou sem pontuação.") String cpf,
        @Email @NotBlank @Size(max = 120) String email,
        @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Za-z0-9_.-]+", message = "Use letras, números, ponto, hífen ou sublinhado no login.") String login,
        @SenhaValida String senha,
        Boolean administrador,
        Boolean ativo,
        @Pattern(regexp = "MEMBRO|MUSICO|PRODUTOR") String perfil,
        @Size(max = 80) String instrumento,
        @Size(max = 500) String biografia,
        @Size(max = 100) String especialidade
) {
}
