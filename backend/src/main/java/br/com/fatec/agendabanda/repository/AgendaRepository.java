package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.model.Agenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendaRepository extends JpaRepository<Agenda, Integer> {
    List<Agenda> findAllByOrderByDataHoraInicioAsc();
    boolean existsByTipoEventoCodigoTipo(Integer codigoTipo);
    boolean existsByCriadorCodigo(Integer codigoMembro);
    boolean existsByLocalCodigoLocal(Integer codigoLocal);

    @Query("""
            select a from Agenda a
            where a.status <> 'CANCELADO'
              and (:ignorarId is null or a.codigoAgenda <> :ignorarId)
              and a.dataHoraInicio < :fim and a.dataHoraFim > :inicio
            """)
    List<Agenda> buscarSobrepostos(@Param("inicio") LocalDateTime inicio,
                                  @Param("fim") LocalDateTime fim,
                                  @Param("ignorarId") Integer ignorarId);
}
