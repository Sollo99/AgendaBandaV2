package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.model.LocalEvento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalEventoRepository extends JpaRepository<LocalEvento, Integer> {
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "cidade")
    java.util.List<LocalEvento> findAll();

    boolean existsByCidadeCodigoCidade(Integer codigoCidade);

}
