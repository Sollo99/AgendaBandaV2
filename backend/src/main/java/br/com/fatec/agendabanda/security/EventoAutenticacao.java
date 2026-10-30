package br.com.fatec.agendabanda.security;

/** Somente metadados controlados pelo servidor; nao aceita textos ou credenciais da requisicao. */
public record EventoAutenticacao(Operacao operacao, Integer codigoMembro, Motivo motivo, Recurso recurso) {
    public enum Operacao { LOGIN, LOGOUT, LOGIN_FALHOU, ACESSO_NEGADO, LIMITE_EXCEDIDO }
    public enum Motivo { SUCESSO, CREDENCIAIS_INVALIDAS, SEM_SESSAO, SEM_PERMISSAO, CSRF_INVALIDO, LIMITE_EXCEDIDO, CODIGO_INVALIDO }
    public enum Recurso { LOGIN, LOGOUT, API, CADASTRO, RECUPERACAO }

    public static Recurso recursoDaRota(String path) {
        if (path == null) return Recurso.API;
        return switch (path) {
            case "/api/auth/login" -> Recurso.LOGIN;
            case "/api/auth/logout" -> Recurso.LOGOUT;
            case "/api/auth/cadastro" -> Recurso.CADASTRO;
            case "/api/auth/recuperar-senha", "/api/auth/redefinir-senha" -> Recurso.RECUPERACAO;
            default -> Recurso.API;
        };
    }
}
