package br.com.fatec.agendabanda.service;

import java.time.*;
import java.util.Set;

/** Uma execucao por dia selecionado, sempre no fuso usado pela agenda. */
public final class PlanejamentoBackup {
    private PlanejamentoBackup() {}
    public static LocalDateTime proxima(LocalDateTime agora,String horario,Set<Integer> dias) {
        LocalTime hora=LocalTime.parse(horario);
        if (dias==null || dias.isEmpty() || dias.stream().anyMatch(d -> d==null || d<1 || d>7))
            throw new IllegalArgumentException("Selecione dias válidos para o backup.");
        for(int i=0;i<=7;i++) {
            LocalDate dia=agora.toLocalDate().plusDays(i);
            LocalDateTime candidata=dia.atTime(hora);
            if (dias.contains(dia.getDayOfWeek().getValue()) && candidata.isAfter(agora)) return candidata;
        }
        throw new IllegalStateException("Não foi possível calcular o próximo backup.");
    }
}
