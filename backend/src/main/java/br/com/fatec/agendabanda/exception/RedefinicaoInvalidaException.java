package br.com.fatec.agendabanda.exception;

/** Resposta publica unica, sem revelar existencia ou estado de uma solicitacao. */
public class RedefinicaoInvalidaException extends RegraNegocioException {
    public RedefinicaoInvalidaException() {
        super("Código de recuperação inválido ou indisponível. Solicite a recuperação ou contate um administrador.");
    }
}
