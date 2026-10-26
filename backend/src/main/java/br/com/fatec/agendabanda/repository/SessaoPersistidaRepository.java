package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.model.SessaoPersistida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SessaoPersistidaRepository extends JpaRepository<SessaoPersistida, Integer> {
    Optional<SessaoPersistida> findByTokenHashAndExpiraEmAfter(String tokenHash, LocalDateTime agora);
    void deleteByTokenHash(String tokenHash);
    void deleteByCodigoMembro(Integer codigoMembro);
    @Modifying
    @Query("delete from SessaoPersistida s where s.expiraEm <= :agora")
    int excluirVencidas(@Param("agora") LocalDateTime agora);
}
