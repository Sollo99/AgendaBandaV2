package br.com.fatec.agendabanda.dto;

import br.com.fatec.agendabanda.exception.RegraNegocioException;

import java.util.List;

public record PaginaDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static void validar(int page, int size) {
        if (page < 0 || page > 1_000_000 || size < 1 || size > 100) {
            throw new RegraNegocioException("Informe page entre 0 e 1000000 e size entre 1 e 100.");
        }
    }

    public static <T> PaginaDto<T> de(List<T> content, int page, int size, long total) {
        return new PaginaDto<>(List.copyOf(content), page, size, total, (int) Math.min(Integer.MAX_VALUE, (total + size - 1) / size));
    }
}
