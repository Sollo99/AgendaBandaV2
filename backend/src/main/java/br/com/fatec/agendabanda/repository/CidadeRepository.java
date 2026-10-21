package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.model.Cidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CidadeRepository extends JpaRepository<Cidade, Integer> {
    List<Cidade> findAllByOrderByNomeAscUfAsc();
    Optional<Cidade> findByNomeIgnoreCaseAndUfIgnoreCase(String nome, String uf);
}
