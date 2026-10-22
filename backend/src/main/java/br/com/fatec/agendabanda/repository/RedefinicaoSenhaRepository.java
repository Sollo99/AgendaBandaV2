package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.model.RedefinicaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface RedefinicaoSenhaRepository extends JpaRepository<RedefinicaoSenha, Integer> {
    Optional<RedefinicaoSenha> findTopByMembroCodigoOrderBySolicitadoEmDesc(Integer codigoMembro);
    Optional<RedefinicaoSenha> findByCodigoRedefinicaoAndStatus(Integer codigoRedefinicao, String status);
    List<RedefinicaoSenha> findByStatusOrderBySolicitadoEmAsc(String status);
}
