package br.com.fatec.agendabanda.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ParticipacaoAtualizacaoRequest(
        @Pattern(regexp = "PENDENTE|CONFIRMADO|RECUSADO", message = "Status de confirmação inválido.") String statusConfirmacao,
        Boolean presente,
        @Size(max = 500) String observacao
) {
}
