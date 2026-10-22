package br.com.fatec.agendabanda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record LoginRequest(@NotBlank String login, @NotBlank String senha) {}
    public record LoginResponse(String token, MembroDto usuario) {}
    public record RecuperarSenhaRequest(@NotBlank String identificador, @NotBlank String chaveSolicitacao) {}
    public record RecuperarSenhaResponse(boolean podeRedefinir, Integer codigoSolicitacao, String mensagem) {}
    public record RedefinirSenhaRequest(@NotNull Integer codigoSolicitacao, @NotBlank String chaveSolicitacao, @NotBlank @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres.") String novaSenha) {}
    public record DecisaoRedefinicaoSenhaRequest(@NotNull Boolean aprovada) {}
    public record SolicitacaoRedefinicaoSenhaDto(Integer codigoSolicitacao, Integer codigoMembro, String nomeMembro, String loginMembro, String emailMembro, java.time.LocalDateTime solicitadoEm) {}
}
