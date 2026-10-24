package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.model.Membro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface MembroRepository extends JpaRepository<Membro, Integer> {
    Optional<Membro> findByLogin(String login);
    @Query("select m.codigo from Membro m where m.login = :login")
    Optional<Integer> buscarCodigoPorLogin(@Param("login") String login);
    Optional<Membro> findByEmailIgnoreCase(String email);
    Optional<Membro> findByCpf(String cpf);
    boolean existsByLogin(String login);
    boolean existsByEmail(String email);
    boolean existsByCpf(String cpf);

    @Query("select m.codigo from Membro m where m.login = :identificador or lower(m.email) = lower(:identificador)")
    Optional<Integer> buscarCodigoPorIdentificador(@Param("identificador") String identificador);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Membro m where m.codigo = :codigo")
    Optional<Membro> bloquearPorCodigo(@Param("codigo") Integer codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Membro m where m.administrador = true and m.ativo = true order by m.codigo")
    List<Membro> bloquearAdministradoresAtivos();
}
