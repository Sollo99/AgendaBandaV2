package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.model.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoEventoRepository extends JpaRepository<TipoEvento, Integer> {
    List<TipoEvento> findByAtivoTrueOrderByNomeAsc();
    List<TipoEvento> findAllByOrderByNomeAsc();
    Optional<TipoEvento> findByNomeIgnoreCase(String nome);
}
