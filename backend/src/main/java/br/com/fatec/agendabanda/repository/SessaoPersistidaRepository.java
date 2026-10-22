package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.model.SessaoPersistida;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SessaoPersistidaRepository extends JpaRepository<SessaoPersistida, Integer> {
    Optional<SessaoPersistida> findByTokenHashAndExpiraEmAfter(String tokenHash, LocalDateTime agora);
    void deleteByTokenHash(String tokenHash);
    void deleteByCodigoMembro(Integer codigoMembro);
}
