package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.model.RedefinicaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import java.util.List;

public interface RedefinicaoSenhaRepository extends JpaRepository<RedefinicaoSenha, Integer> {
    Optional<RedefinicaoSenha> findByMembroCodigoAndStatusIn(Integer codigoMembro, List<String> status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RedefinicaoSenha> findByCodigoRedefinicaoAndStatus(Integer codigoRedefinicao, String status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RedefinicaoSenha> findByCodigoRedefinicaoAndStatusIn(Integer codigoRedefinicao, List<String> status);
    @Query("select r.membro.codigo from RedefinicaoSenha r where r.codigoRedefinicao = :codigo")
    Optional<Integer> buscarCodigoMembro(@Param("codigo") Integer codigo);
    List<RedefinicaoSenha> findByStatusOrderBySolicitadoEmAsc(String status);
    @EntityGraph(attributePaths = "membro")
    List<RedefinicaoSenha> findByStatusInOrderBySolicitadoEmAsc(List<String> status);
    @Query("select distinct r.membro.codigo from RedefinicaoSenha r where r.status in ('PENDENTE', 'APROVADA') and r.expiraEm <= :agora order by r.membro.codigo")
    List<Integer> buscarMembrosComPedidosVencidos(@Param("agora") LocalDateTime agora);
    boolean existsByAdministradorCodigo(Integer codigoAdministrador);
}
