package br.com.fatec.agendabanda.exception;

public class SessaoExpiradaException extends IllegalArgumentException {
    public SessaoExpiradaException() { super("Sessão inválida ou expirada."); }
}
