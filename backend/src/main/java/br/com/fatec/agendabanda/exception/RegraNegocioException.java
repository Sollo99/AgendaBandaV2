package br.com.fatec.agendabanda.exception;

/** Mensagem controlada pela aplicacao que pode ser apresentada ao usuario. */
public class RegraNegocioException extends IllegalArgumentException {
    public RegraNegocioException(String mensagem) { super(mensagem); }
}
