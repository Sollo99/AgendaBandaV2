package br.com.fatec.agendabanda.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public final class BackupDtos {
    private BackupDtos() {}
    public enum Tipo { BACKUP, RESTORE }
    public enum Origem { MANUAL, AGENDADO, INICIALIZACAO, PRE_RESTAURACAO }
    public enum Estado { EM_FILA, EXECUTANDO, CONCLUIDO, FALHOU, INTERROMPIDO }
    public record Preferencias(boolean aoIniciar, boolean agendado, String horario, Set<Integer> diasSemana) {
        public static Preferencias padrao() { return new Preferencias(true, false, "02:00", Set.of(1,2,3,4,5,6,7)); }
    }
    public record ConfiguracaoRequest(@NotNull Boolean aoIniciar, @NotNull Boolean agendado,
            @NotBlank @Pattern(regexp="(?:[01][0-9]|2[0-3]):[0-5][0-9]") String horario,
            @NotNull @Size(min=1,max=7) Set<@NotNull @Min(1) @Max(7) Integer> diasSemana) {
        public Preferencias preferencias() { return new Preferencias(aoIniciar,agendado,horario,Set.copyOf(diasSemana)); }
    }
    public record RestaurarRequest(@NotBlank @Size(max=72) String senhaAtual,
                                   @NotBlank @Size(max=128) String confirmacao) {}
    public record Registro(UUID codigo, Tipo tipo, Origem origem, UUID backupOrigem,
            Integer responsavel, String nomeResponsavel, LocalDateTime inicio, LocalDateTime fim,
            Estado estado, String arquivo, Long tamanhoBytes, String assinaturaSchema, String mensagem) {
        public boolean finalizado() { return estado!=Estado.EM_FILA && estado!=Estado.EXECUTANDO; }
        public Registro atualizar(Estado novo, String mensagem, LocalDateTime fim, Long tamanho, String assinatura) {
            return new Registro(codigo,tipo,origem,backupOrigem,responsavel,nomeResponsavel,inicio,fim,novo,
                    arquivo,tamanho,assinatura,mensagem);
        }
    }
    public record Painel(boolean habilitado, boolean configurado, boolean downloadDisponivel,
            String banco, String aviso, Preferencias preferencias, LocalDateTime proximaExecucao,
            Registro emAndamento, boolean manutencao) {}
    public record OperacaoAceita(Registro registro, String acompanhamento) {}
    public record Progresso(Registro registro, boolean manutencao) {}
    public record Ator(Integer codigo, String nome) {}
}
