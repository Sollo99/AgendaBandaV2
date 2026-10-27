package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.RelatorioDtos.*;
import br.com.fatec.agendabanda.repository.RelatorioSqlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class RelatorioService {
    private final RelatorioSqlRepository repository;
    private final Clock clock;
    public RelatorioService(RelatorioSqlRepository repository, Clock clock) { this.repository = repository; this.clock = clock; }

    @Transactional(readOnly = true)
    public Relatorio consultar(LocalDate inicio, LocalDate fim) {
        var periodo = periodo(inicio, fim);
        var de = periodo[0].atStartOfDay();
        var ate = periodo[1].plusDays(1).atStartOfDay();
        return new Relatorio(periodo[0], periodo[1], repository.resumo(de, ate), repository.presencas(de, ate), repository.locais(de, ate));
    }

    @Transactional(readOnly = true)
    public List<Historico> historicos(LocalDate inicio, LocalDate fim) {
        var periodo = periodo(inicio, fim);
        return repository.historicos(periodo[0].atStartOfDay(), periodo[1].plusDays(1).atStartOfDay());
    }

    private LocalDate[] periodo(LocalDate inicio, LocalDate fim) {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate de = inicio == null ? hoje.withDayOfYear(1) : inicio;
        LocalDate ate = fim == null ? hoje.withMonth(12).withDayOfMonth(31) : fim;
        if (ate.isBefore(de) || de.getYear() < 1900 || ate.getYear() > 9998) {
            throw new IllegalArgumentException("Informe um período válido entre os anos 1900 e 9998.");
        }
        return new LocalDate[]{de, ate};
    }
}
