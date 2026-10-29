package br.com.fatec.agendabanda.exception;

public class ConflitoException extends IllegalArgumentException {
    public ConflitoException(String mensagem) { super(mensagem); }
}
