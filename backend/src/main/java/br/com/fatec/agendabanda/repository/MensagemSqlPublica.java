package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.exception.RegraNegocioException;

import java.util.Set;
import br.com.fatec.agendabanda.exception.RecursoNaoEncontradoException;

/** Somente mensagens de negocio conhecidas podem sair do driver SQL para a API. */
final class MensagemSqlPublica {
    private static final Set<String> PERMITIDAS = Set.of(
            "A data/hora final deve ser maior que a inicial.",
            "Titulo ou status do evento invalido.",
            "Um evento so pode ser marcado como realizado apos seu termino.",
            "Tipo de evento inexistente ou inativo.",
            "Local nao encontrado.",
            "Informe uma cidade valida para o show.",
            "Lista de musicos invalida.",
            "Nao e possivel incluir um musico inexistente, inativo ou nao aprovado.",
            "Evento nao encontrado.",
            "Um evento realizado deve permanecer realizado para preservar seu historico.",
            "Um evento com presenca registrada nao pode ser cancelado ou movido para o futuro.",
            "Nao e possivel remover participantes de evento realizado ou com presenca registrada.",
            "Informe o modo de atualizacao da participacao.",
            "Status de confirmacao invalido.",
            "A confirmacao so pode ser alterada antes do inicio de um evento agendado.",
            "Presenca ou ausencia so pode ser registrada apos o inicio de evento nao cancelado.",
            "Participacao nao encontrada.",
            "Um participante presente deve permanecer confirmado.");

    static IllegalArgumentException excecao(String mensagem) {
        if (mensagem == null || !PERMITIDAS.contains(mensagem)) return new RegraNegocioException("Dados incompatíveis com as regras do evento.");
        if (mensagem.equals("Evento nao encontrado.") || mensagem.equals("Participacao nao encontrada."))
            return new RecursoNaoEncontradoException(mensagem);
        return new RegraNegocioException(mensagem);
    }
}
