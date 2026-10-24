package br.com.fatec.agendabanda.exception;

public class RecuperacaoExpiradaException extends IllegalArgumentException {
    public RecuperacaoExpiradaException() {
        super("A solicitação de recuperação expirou. Solicite novamente.");
    }
}
