package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.model.LocalEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LocalEventoRepository extends JpaRepository<LocalEvento, Integer> {
    boolean existsByCidadeCodigoCidade(Integer codigoCidade);

    // SQL Server: serializa as escritas da agenda e a exclusão de locais na mesma transação.
    // Evita que duas requisições validem um horário livre antes de ambas gravarem.
    @Query(value = "SELECT codigo_local FROM locais WITH (TABLOCKX, HOLDLOCK)", nativeQuery = true)
    List<Integer> bloquearAgendamento();
}
