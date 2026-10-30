package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.model.SessaoPersistida;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoPersistidaRepository extends JpaRepository<SessaoPersistida, Integer> {
    void deleteByTokenHash(String tokenHash);
    void deleteByCodigoMembro(Integer codigoMembro);
}
