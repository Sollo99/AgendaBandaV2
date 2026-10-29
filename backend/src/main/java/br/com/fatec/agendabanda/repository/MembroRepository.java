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
    @Query(value = """
        select new br.com.fatec.agendabanda.dto.MembroDto(
            m.codigo,m.nome,m.cpf,m.email,m.login,m.administrador,m.ativo,m.perfil,
            mu.instrumento,mu.biografia,p.especialidade,m.statusCadastro)
        from Membro m left join Musico mu on mu.codigo=m.codigo left join Produtor p on p.codigo=m.codigo
        """, countQuery = "select count(m) from Membro m")
    org.springframework.data.domain.Page<br.com.fatec.agendabanda.dto.MembroDto> listarPagina(org.springframework.data.domain.Pageable pagina);

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

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    @Query(value = """
        DECLARE @resultado INT;
        EXEC @resultado = sys.sp_getapplock @Resource=N'AgendaBanda:administradores',
            @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=5000;
        SELECT @resultado;
        """, nativeQuery = true)
    Integer adquirirBloqueioAdministracao();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Membro m where m.administrador = true and m.ativo = true order by m.codigo")
    List<Membro> buscarAdministradoresComLock();

    default List<Membro> bloquearAdministradoresAtivos() {
        Integer resultado = adquirirBloqueioAdministracao();
        if (resultado == null || resultado < 0) {
            throw new org.springframework.dao.ConcurrencyFailureException("Administração em uso. Tente novamente.");
        }
        return buscarAdministradoresComLock();
    }
}
