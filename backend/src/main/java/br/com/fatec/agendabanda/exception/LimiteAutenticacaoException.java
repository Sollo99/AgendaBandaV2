package br.com.fatec.agendabanda.exception;

public class LimiteAutenticacaoException extends RuntimeException {
    private final long segundos;

    public LimiteAutenticacaoException(long segundos) {
        super("Muitas tentativas. Aguarde e tente novamente.");
        this.segundos = segundos;
    }

    public long getSegundos() { return segundos; }
}
