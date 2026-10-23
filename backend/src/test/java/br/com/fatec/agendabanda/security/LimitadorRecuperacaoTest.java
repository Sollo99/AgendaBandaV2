package br.com.fatec.agendabanda.security;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LimitadorRecuperacaoTest {
    @Test
    void limitaPorIpERenovaAJanelaAposQuinzeMinutos() {
        Clock clock = mock(Clock.class);
        var agora = Instant.parse("2026-10-23T12:00:00Z");
        when(clock.instant()).thenReturn(agora);
        var limitador = new LimitadorRecuperacao(clock);
        for (int i = 0; i < 10; i++) assertEquals(0, limitador.segundosDeEspera("127.0.0.1"));
        assertEquals(900, limitador.segundosDeEspera("127.0.0.1"));
        assertEquals(0, limitador.segundosDeEspera("127.0.0.2"));
        when(clock.instant()).thenReturn(agora.plusSeconds(900));
        assertEquals(0, limitador.segundosDeEspera("127.0.0.1"));
    }
}
