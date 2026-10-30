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
            "Um participante presente deve permanecer confirmado.",
            "Nao e possivel excluir um evento realizado ou com presenca registrada.",
            "Membro nao encontrado.",
            "O cadastro precisa ser aprovado antes de ativar a conta ou conceder administracao.",
            "E necessario manter pelo menos um administrador ativo.",
            "O cadastro ja foi analisado.",
            "Os identificadores foram liberados. E necessario fazer um novo cadastro.",
            "Somente cadastros recusados podem ter identificadores liberados.",
            "O local esta vinculado a eventos e nao pode ser excluido.");

    static IllegalArgumentException excecao(String mensagem) {
        if (mensagem == null || !PERMITIDAS.contains(mensagem)) return new RegraNegocioException("Dados incompatíveis com as regras da operação.");
        if (mensagem.equals("Evento nao encontrado.") || mensagem.equals("Participacao nao encontrada.") || mensagem.equals("Membro nao encontrado.") || mensagem.equals("Local nao encontrado."))
            return new RecursoNaoEncontradoException(mensagem);
        return new RegraNegocioException(mensagem);
    }
}
