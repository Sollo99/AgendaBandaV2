package br.com.fatec.agendabanda.dto;

import jakarta.validation.constraints.Pattern;

public record ParticipacaoAtualizacaoRequest(
        @Pattern(regexp = "PENDENTE|CONFIRMADO|RECUSADO", message = "Status de confirmação inválido.") String statusConfirmacao,
        Boolean presente,
        String observacao
) {
}
