package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.model.*;
import br.com.fatec.agendabanda.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CicloRecuperacaoServiceTest {
    @Mock RedefinicaoSenhaRepository repository;
    @Mock MembroRepository membros;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC);
    private CicloRecuperacaoService service;
    private RedefinicaoSenha pedido;

    @BeforeEach
    void preparar() {
        service = new CicloRecuperacaoService(repository, membros, clock, 24, 2);
        var membro = new Membro(); membro.setCodigo(7);
        pedido = new RedefinicaoSenha(); pedido.setMembro(membro); pedido.setCodigoRedefinicao(12);
        when(membros.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
        when(repository.findByMembroCodigoAndStatusIn(7, CicloRecuperacaoService.ABERTAS)).thenReturn(Optional.of(pedido));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDENTE", "APROVADA"})
    void venceNoInstanteDoPrazoELiberaIndiceAntesDeOutroPedido(String status) {
        pedido.setStatus(status); pedido.setExpiraEm(LocalDateTime.now(clock));
        assertTrue(service.abertaValida(7).isEmpty());
        assertEquals("EXPIRADA", pedido.getStatus());
        assertEquals(LocalDateTime.now(clock), pedido.getEncerradoEm());
        verify(repository).saveAndFlush(pedido);
    }

    @Test
    void pedidoDentroDoPrazoContinuaAberto() {
        pedido.setStatus("APROVADA"); pedido.setExpiraEm(LocalDateTime.now(clock).plusSeconds(1));
        assertSame(pedido, service.abertaValida(7).orElseThrow());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void cancelarUmaAprovadaRevogaOEstadoAbertoEPreservaMotivo() {
        pedido.setStatus("APROVADA");
        service.cancelarAbertas(7, "Senha alterada pelo perfil.");
        assertEquals("CANCELADA", pedido.getStatus());
        assertEquals("Senha alterada pelo perfil.", pedido.getMotivoEncerramento());
        assertNotNull(pedido.getEncerradoEm());
        verify(repository).saveAndFlush(pedido);
    }
}
