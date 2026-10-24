package br.com.fatec.agendabanda.security;

import br.com.fatec.agendabanda.model.*;
import br.com.fatec.agendabanda.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokenServiceTest {
    private final SessaoPersistidaRepository sessoes = mock(SessaoPersistidaRepository.class);
    private final MembroRepository membros = mock(MembroRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC);
    private final TokenService service = new TokenService(sessoes, membros, clock);
    private Membro membro;

    @BeforeEach
    void preparar() {
        membro = new Membro(); membro.setCodigo(7); membro.setAtivo(true);
        var sessao = new SessaoPersistida(); sessao.setCodigoMembro(7);
        when(sessoes.findByTokenHashAndExpiraEmAfter(anyString(), eq(LocalDateTime.now(clock)))).thenReturn(Optional.of(sessao));
        when(membros.findById(7)).thenReturn(Optional.of(membro));
    }

    @Test
    void sessaoNaoLiberaMembroAindaPendenteMesmoSeAtivo() {
        membro.setStatusCadastro("PENDENTE");
        assertTrue(service.buscarSessao("token").isEmpty());
    }

    @Test
    void sessaoNaoLiberaContaDesativada() {
        membro.setAtivo(false);
        assertTrue(service.buscarSessao("token").isEmpty());
    }

    @Test
    void membroAprovadoEAtivoContinuaAutenticado() {
        assertEquals(7, service.buscarSessao("token").orElseThrow().codigo());
    }
}
