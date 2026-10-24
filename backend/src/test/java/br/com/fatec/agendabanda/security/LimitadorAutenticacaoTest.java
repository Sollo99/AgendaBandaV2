package br.com.fatec.agendabanda.security;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LimitadorAutenticacaoTest {
    @Test
    void limitaPorIpERenovaAJanelaAposQuinzeMinutos() {
        Clock clock = mock(Clock.class);
        var agora = Instant.parse("2026-10-23T12:00:00Z");
        when(clock.instant()).thenReturn(agora);
        var limitador = new LimitadorAutenticacao(clock);
        for (int i = 0; i < 10; i++) assertEquals(0, limitador.segundosDeEspera(LimitadorAutenticacao.Tipo.RECUPERACAO, "127.0.0.1"));
        assertEquals(900, limitador.segundosDeEspera(LimitadorAutenticacao.Tipo.RECUPERACAO, "127.0.0.1"));
        assertEquals(0, limitador.segundosDeEspera(LimitadorAutenticacao.Tipo.RECUPERACAO, "127.0.0.2"));
        when(clock.instant()).thenReturn(agora.plusSeconds(900));
        assertEquals(0, limitador.segundosDeEspera(LimitadorAutenticacao.Tipo.RECUPERACAO, "127.0.0.1"));
    }

    @Test
    void loginCadastroERecuperacaoPossuemContadoresIndependentes() {
        var limitador = new LimitadorAutenticacao(Clock.systemUTC());
        for (int i = 0; i < 5; i++) assertEquals(0, limitador.segundosDeEspera(LimitadorAutenticacao.Tipo.CADASTRO, "192.0.2.1"));
        assertTrue(limitador.segundosDeEspera(LimitadorAutenticacao.Tipo.CADASTRO, "192.0.2.1") > 0);
        for (int i = 0; i < 20; i++) assertEquals(0, limitador.segundosDeEspera(LimitadorAutenticacao.Tipo.LOGIN, "192.0.2.1"));
        assertTrue(limitador.segundosDeEspera(LimitadorAutenticacao.Tipo.LOGIN, "192.0.2.1") > 0);
        assertEquals(0, limitador.segundosDeEspera(LimitadorAutenticacao.Tipo.RECUPERACAO, "192.0.2.1"));
    }
}
