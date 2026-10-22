package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.model.ParticipacaoEvento;
import br.com.fatec.agendabanda.model.ParticipacaoEventoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipacaoEventoRepository extends JpaRepository<ParticipacaoEvento, ParticipacaoEventoId> {
    List<ParticipacaoEvento> findByAgendaCodigoAgendaOrderByMusicoMembroNomeAsc(Integer codigoAgenda);
    void deleteByAgendaCodigoAgenda(Integer codigoAgenda);
    boolean existsByMusicoCodigo(Integer codigoMusico);
}
