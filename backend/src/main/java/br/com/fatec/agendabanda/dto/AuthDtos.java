package br.com.fatec.agendabanda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record LoginRequest(@NotBlank String login, @NotBlank String senha) {}
    public record LoginResponse(String token, MembroDto usuario) {}
    public record RecuperarSenhaRequest(@NotBlank String identificador, @Pattern(regexp = "[A-Za-z0-9_-]{32,128}", message = "Código de recuperação inválido.") String chaveSolicitacao) {}
    public record RecuperarSenhaResponse(boolean podeRedefinir, Integer codigoSolicitacao, String mensagem, String codigoRecuperacao) {}
    public record RedefinirSenhaRequest(@NotNull Integer codigoSolicitacao, @Pattern(regexp = "[A-Za-z0-9_-]{32,128}", message = "Código de recuperação inválido.") String chaveSolicitacao, @NotBlank @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres.") String novaSenha) {}
    public record DecisaoRedefinicaoSenhaRequest(@NotNull Boolean aprovada) {}
    public record SolicitacaoRedefinicaoSenhaDto(Integer codigoSolicitacao, Integer codigoMembro, String nomeMembro, String loginMembro, String emailMembro, java.time.LocalDateTime solicitadoEm) {}
}
