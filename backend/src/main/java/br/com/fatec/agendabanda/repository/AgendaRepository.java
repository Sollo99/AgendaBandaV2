package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.model.Agenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgendaRepository extends JpaRepository<Agenda, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Agenda a where a.codigoAgenda = :codigo")
    Optional<Agenda> bloquearPorCodigo(@Param("codigo") Integer codigo);
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
