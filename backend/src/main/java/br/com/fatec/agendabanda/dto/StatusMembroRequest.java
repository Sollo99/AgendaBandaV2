package br.com.fatec.agendabanda.dto;

import jakarta.validation.constraints.NotNull;

public record StatusMembroRequest(@NotNull Boolean ativo) {}
