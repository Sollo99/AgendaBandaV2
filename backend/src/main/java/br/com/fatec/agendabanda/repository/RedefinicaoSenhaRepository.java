package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.model.RedefinicaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.List;

public interface RedefinicaoSenhaRepository extends JpaRepository<RedefinicaoSenha, Integer> {
    Optional<RedefinicaoSenha> findTopByMembroCodigoOrderBySolicitadoEmDesc(Integer codigoMembro);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RedefinicaoSenha> findByCodigoRedefinicaoAndStatus(Integer codigoRedefinicao, String status);
    List<RedefinicaoSenha> findByStatusOrderBySolicitadoEmAsc(String status);
    boolean existsByAdministradorCodigo(Integer codigoAdministrador);
}
