package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.model.Musico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MusicoRepository extends JpaRepository<Musico, Integer> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "membro")
    List<Musico> findByMembroAtivoTrueOrderByMembroNomeAsc();
}
