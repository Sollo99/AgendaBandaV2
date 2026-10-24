package br.com.fatec.agendabanda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import br.com.fatec.agendabanda.validation.SenhaValida;

public class AuthDtos {
    public record LoginRequest(@NotBlank @Size(max = 50) String login, @NotBlank @Size(max = 72) String senha) {}
    public record LoginResponse(String token, MembroDto usuario) {}
    public record RecuperarSenhaRequest(@NotBlank @Size(max = 120) String identificador, @Pattern(regexp = "[A-Za-z0-9_-]{32,128}", message = "Código de recuperação inválido.") String chaveSolicitacao) {}
    public record RecuperarSenhaResponse(boolean podeRedefinir, Integer codigoSolicitacao, String mensagem, String codigoRecuperacao) {}
    public record RedefinirSenhaRequest(@NotNull @Positive Integer codigoSolicitacao, @Pattern(regexp = "[A-Za-z0-9_-]{32,128}", message = "Código de recuperação inválido.") String chaveSolicitacao, @NotBlank @SenhaValida String novaSenha) {}
    public record DecisaoRedefinicaoSenhaRequest(@NotNull Boolean aprovada) {}
    public record DecisaoCadastroRequest(@NotNull Boolean aprovada) {}
    public record SolicitacaoRedefinicaoSenhaDto(Integer codigoSolicitacao, Integer codigoMembro, String nomeMembro, String loginMembro, String emailMembro, java.time.LocalDateTime solicitadoEm, String status, java.time.LocalDateTime expiraEm) {}
}
