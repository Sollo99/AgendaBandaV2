package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.model.LocalEvento;
import br.com.fatec.agendabanda.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalServiceTest {
    @Mock LocalEventoRepository localRepository;
    @Mock CidadeRepository cidadeRepository;
    @Mock AgendaRepository agendaRepository;
    @Mock MapperService mapper;
    @InjectMocks LocalService service;

    @Test
    void recusaExclusaoDeLocalVinculadoComMensagemClara() {
        when(localRepository.findById(1)).thenReturn(Optional.of(new LocalEvento()));
        when(agendaRepository.existsByLocalCodigoLocal(1)).thenReturn(true);
        var erro = assertThrows(IllegalArgumentException.class, () -> service.excluir(1));
        assertTrue(erro.getMessage().contains("vinculado a eventos"));
        verify(localRepository, never()).delete(any());
    }

    @Test
    void excluiLocalSemVinculos() {
        var local = new LocalEvento(); local.setCodigoLocal(1);
        when(localRepository.findById(1)).thenReturn(Optional.of(local));
        service.excluir(1);
        var ordem = inOrder(localRepository, agendaRepository);
        ordem.verify(localRepository).bloquearAgendamento();
        ordem.verify(agendaRepository).existsByLocalCodigoLocal(1);
        ordem.verify(localRepository).delete(local);
    }
}
