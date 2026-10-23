package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.AgendaRequest;
import br.com.fatec.agendabanda.model.*;
import br.com.fatec.agendabanda.repository.*;
import br.com.fatec.agendabanda.security.SessaoUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {
    @Mock AgendaRepository agendaRepository;
    @Mock EnsaioRepository ensaioRepository;
    @Mock ReuniaoRepository reuniaoRepository;
    @Mock ShowEventoRepository showRepository;
    @Mock LocalEventoRepository localRepository;
    @Mock MusicoRepository musicoRepository;
    @Mock MembroService membroService;
    @Mock LocalService localService;
    @Mock CidadeRepository cidadeRepository;
    @Mock TipoEventoRepository tipoRepository;
    @Mock ParticipacaoEventoRepository participacaoRepository;
    @Mock MapperService mapper;
    @Mock PermissaoService permissaoService;
    @InjectMocks AgendaService service;
    private final SessaoUsuario sessao = new SessaoUsuario(7, "Ana", "ana", true);
    private final LocalDateTime inicio = LocalDateTime.of(2026, 11, 10, 18, 0);
    private Agenda agenda;
    private LocalEvento local;
    private TipoEvento tipo;

    @BeforeEach
    void preparar() {
        local = new LocalEvento(); local.setCodigoLocal(1);
        tipo = new TipoEvento(); tipo.setCodigoTipo(1); tipo.setCategoriaDetalhe("EVENTO"); tipo.setAtivo(true);
        agenda = new Agenda(); agenda.setCodigoAgenda(10); agenda.setLocal(local); agenda.setTipoEvento(tipo);
        when(agendaRepository.findById(10)).thenReturn(Optional.of(agenda));
        when(localService.buscarLocal(1)).thenReturn(local);
        when(tipoRepository.findById(1)).thenReturn(Optional.of(tipo));
    }

    private AgendaRequest request(List<Integer> ids, String status) {
        return new AgendaRequest("Título editado", null, inicio, inicio.plusHours(2), 1, 1, status,
                null, null, null, null, ids);
    }

    private ParticipacaoEvento participacao(int id) {
        var membro = new Membro(); membro.setCodigo(id); membro.setNome("Músico " + id); membro.setAtivo(true);
        var musico = new Musico(); musico.setCodigo(id); musico.setMembro(membro);
        var item = new ParticipacaoEvento(); item.setId(new ParticipacaoEventoId(10, id));
        item.setAgenda(agenda); item.setMusico(musico); item.setStatusConfirmacao("CONFIRMADO");
        item.setPresente(true); item.setObservacao("Chego antes"); item.setDataResposta(inicio.minusDays(1));
        return item;
    }

    private void permitirSalvar() { when(agendaRepository.save(agenda)).thenReturn(agenda); }

    @Test
    void edicaoPreservaTodosOsDadosDosParticipantesMantidos() {
        permitirSalvar();
        var mantido = participacao(7);
        when(participacaoRepository.findByAgendaCodigoAgendaOrderByMusicoMembroNomeAsc(10)).thenReturn(List.of(mantido));
        service.atualizar(10, request(List.of(7), "AGENDADO"), sessao);
        assertEquals("CONFIRMADO", mantido.getStatusConfirmacao());
        assertTrue(mantido.getPresente());
        assertEquals("Chego antes", mantido.getObservacao());
        assertEquals(inicio.minusDays(1), mantido.getDataResposta());
        verify(participacaoRepository, never()).delete(any());
        verify(participacaoRepository, never()).save(any());
    }

    @Test
    void removeSoQuemSaiECriaPendenteSoParaQuemEntra() {
        permitirSalvar();
        var mantido = participacao(7); var removido = participacao(8); var novo = participacao(9).getMusico();
        when(participacaoRepository.findByAgendaCodigoAgendaOrderByMusicoMembroNomeAsc(10)).thenReturn(List.of(mantido, removido));
        when(musicoRepository.findById(9)).thenReturn(Optional.of(novo));
        service.atualizar(10, request(List.of(7, 9, 9), "AGENDADO"), sessao);
        verify(participacaoRepository).delete(removido);
        verify(participacaoRepository, never()).delete(mantido);
        var captor = ArgumentCaptor.forClass(ParticipacaoEvento.class);
        verify(participacaoRepository).save(captor.capture());
        assertEquals(9, captor.getValue().getMusico().getCodigo());
        assertEquals("PENDENTE", captor.getValue().getStatusConfirmacao());
        assertNull(captor.getValue().getPresente());
        assertNull(captor.getValue().getDataResposta());
    }

    @Test
    void omitirListaDeParticipantesNaEdicaoPreservaVinculos() {
        permitirSalvar();
        when(participacaoRepository.findByAgendaCodigoAgendaOrderByMusicoMembroNomeAsc(10)).thenReturn(List.of(participacao(7)));
        service.atualizar(10, request(null, "AGENDADO"), sessao);
        verify(participacaoRepository, never()).delete(any());
        verify(participacaoRepository, never()).save(any());
    }

    @Test
    void bloqueiaEventoSobrepostoNoMesmoLocal() {
        var outro = new Agenda(); outro.setCodigoAgenda(20); outro.setLocal(local); outro.setTitulo("Outro evento");
        when(agendaRepository.buscarSobrepostos(inicio, inicio.plusHours(2), 10)).thenReturn(List.of(outro));
        var erro = assertThrows(IllegalArgumentException.class, () -> service.atualizar(10, request(List.of(), "AGENDADO"), sessao));
        assertTrue(erro.getMessage().contains("local"));
        verify(agendaRepository, never()).save(any());
    }

    @Test
    void bloqueiaMesmoMusicoEmLocaisDiferentes() {
        var outroLocal = new LocalEvento(); outroLocal.setCodigoLocal(2);
        var outro = new Agenda(); outro.setCodigoAgenda(20); outro.setLocal(outroLocal); outro.setTitulo("Outro evento");
        when(agendaRepository.buscarSobrepostos(inicio, inicio.plusHours(2), 10)).thenReturn(List.of(outro));
        when(participacaoRepository.findByAgendaCodigoAgendaOrderByMusicoMembroNomeAsc(20)).thenReturn(List.of(participacao(7)));
        var erro = assertThrows(IllegalArgumentException.class, () -> service.atualizar(10, request(List.of(7), "AGENDADO"), sessao));
        assertTrue(erro.getMessage().contains("Músico 7"));
        verify(agendaRepository, never()).save(any());
    }

    @Test
    void eventoCanceladoNaoOcupaHorario() {
        permitirSalvar();
        service.atualizar(10, request(List.of(), "CANCELADO"), sessao);
        verify(agendaRepository, never()).buscarSobrepostos(any(), any(), any());
    }

    @Test
    void reutilizaDetalheDeEnsaioAoEditarSemTrocarCategoria() {
        permitirSalvar(); tipo.setCategoriaDetalhe("ENSAIO");
        var detalhe = new Ensaio(); detalhe.setCodigoAgenda(10); detalhe.setAgenda(agenda); detalhe.setRepertorio("Antigo");
        when(ensaioRepository.findById(10)).thenReturn(Optional.of(detalhe));
        service.atualizar(10, request(List.of(), "AGENDADO"), sessao);
        verify(ensaioRepository).save(same(detalhe));
        verify(ensaioRepository, never()).deleteById(10);
    }
}
