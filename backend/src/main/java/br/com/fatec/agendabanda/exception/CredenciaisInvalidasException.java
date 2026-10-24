package br.com.fatec.agendabanda.exception;

public class CredenciaisInvalidasException extends IllegalArgumentException {
    public CredenciaisInvalidasException() { super("Login ou senha inválidos."); }
}
