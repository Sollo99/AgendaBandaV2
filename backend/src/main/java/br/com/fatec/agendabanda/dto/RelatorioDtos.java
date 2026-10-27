package br.com.fatec.agendabanda.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class RelatorioDtos {
    private RelatorioDtos() {}

    public record ResumoTipo(Integer codigoTipo, String tipo, Integer total, Integer agendados,
                             Integer realizados, Integer cancelados, Long minutosProgramados) {}
    public record PresencaMusico(Integer codigoMusico, String nome, String instrumento, Integer eventos,
                                Integer realizados, Integer presencas, Integer ausencias, Integer semRegistro,
                                BigDecimal taxaPresenca) {}
    public record OcupacaoLocal(Integer codigoLocal, String nome, String cidade, String uf, Integer eventos,
                               Integer agendados, Integer realizados, Integer cancelados, Long minutosProgramados) {}
    public record Relatorio(LocalDate inicio, LocalDate fim, List<ResumoTipo> resumo,
                            List<PresencaMusico> presencas, List<OcupacaoLocal> locais) {}
    public record Historico(Long codigo, String entidade, String registro, String operacao, LocalDateTime dataHora,
                            Integer codigoResponsavel, String responsavel, String dadosAnteriores, String dadosNovos) {}
}
