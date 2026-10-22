package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.CadastroRequest;
import br.com.fatec.agendabanda.dto.MembroRequest;
import br.com.fatec.agendabanda.model.Membro;
import br.com.fatec.agendabanda.model.Musico;
import br.com.fatec.agendabanda.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembroServiceTest {
    @Mock MembroRepository membroRepository;
    @Mock MusicoRepository musicoRepository;
    @Mock ProdutorRepository produtorRepository;
    @Mock BCryptPasswordEncoder passwordEncoder;
    @Mock MapperService mapperService;
    @Mock AgendaRepository agendaRepository;
    @Mock ParticipacaoEventoRepository participacaoRepository;
    @Mock EnsaioMusicoRepository ensaioMusicoRepository;
    @Mock RedefinicaoSenhaRepository redefinicaoSenhaRepository;
    @InjectMocks MembroService service;

    private Membro membro;
    private Musico musico;

    @BeforeEach
    void preparar() {
        membro = new Membro();
        membro.setCodigo(7);
        membro.setNome("Ana");
        membro.setAtivo(true);
        musico = new Musico();
        musico.setCodigo(7);
        musico.setMembro(membro);
        musico.setInstrumento("Violão");
    }

    @Test
    void editarMusicoPreservaAEntidadeReferenciadaPelasParticipacoes() {
        when(membroRepository.findById(7)).thenReturn(Optional.of(membro));
        when(membroRepository.save(membro)).thenReturn(membro);
        when(musicoRepository.findById(7)).thenReturn(Optional.of(musico));

        service.atualizar(7, request("MUSICO"));

        verify(musicoRepository).save(same(musico));
        verify(musicoRepository, never()).delete(any());
        verify(musicoRepository, never()).deleteById(any());
        assertEquals("Teclado", musico.getInstrumento());
    }

    @Test
    void impedeRemoverPerfilComParticipacoes() {
        when(membroRepository.findById(7)).thenReturn(Optional.of(membro));
        when(membroRepository.save(membro)).thenReturn(membro);
        when(musicoRepository.findById(7)).thenReturn(Optional.of(musico));
        when(participacaoRepository.existsByMusicoCodigo(7)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.atualizar(7, request("MEMBRO")));
        verify(musicoRepository, never()).delete(any());
    }

    @Test
    void impedeExcluirMusicoVinculadoAEnsaio() {
        when(membroRepository.findById(7)).thenReturn(Optional.of(membro));
        when(ensaioMusicoRepository.existsByMusicoCodigo(7)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.excluir(7));
        verify(membroRepository, never()).delete(any());
    }

    @Test
    void impedeExcluirCriadorDeEventos() {
        when(membroRepository.findById(7)).thenReturn(Optional.of(membro));
        when(agendaRepository.existsByCriadorCodigo(7)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.excluir(7));
        verify(membroRepository, never()).delete(any());
    }

    @Test
    void impedeExcluirAdministradorComDecisoesRegistradas() {
        when(membroRepository.findById(7)).thenReturn(Optional.of(membro));
        when(redefinicaoSenhaRepository.existsByAdministradorCodigo(7)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.excluir(7));
        verify(membroRepository, never()).delete(any());
    }

    @Test
    void excluiMembroSemHistoricoAposRemoverPerfis() {
        when(membroRepository.findById(7)).thenReturn(Optional.of(membro));
        when(musicoRepository.findById(7)).thenReturn(Optional.of(musico));

        service.excluir(7);

        var ordem = inOrder(musicoRepository, membroRepository);
        ordem.verify(musicoRepository).delete(musico);
        ordem.verify(musicoRepository).flush();
        ordem.verify(membroRepository).delete(membro);
    }

    @Test
    void cadastroPublicoCriaMusicoAtivoSemPermissaoAdministrativa() {
        when(membroRepository.save(any())).thenAnswer(invocation -> {
            Membro novo = invocation.getArgument(0);
            novo.setCodigo(8);
            return novo;
        });
        when(passwordEncoder.encode("senha123")).thenReturn("hash-da-senha");

        service.criarCadastro(new CadastroRequest("Ana", "123", "ana@example.com", "ana", "senha123", "Violão", "Bio"));

        var captor = ArgumentCaptor.forClass(Musico.class);
        verify(musicoRepository).save(captor.capture());
        var novo = captor.getValue();
        assertEquals("Violão", novo.getInstrumento());
        assertTrue(novo.getMembro().getAtivo());
        assertFalse(novo.getMembro().getAdministrador());
        assertEquals("hash-da-senha", novo.getMembro().getSenha());
    }

    @Test
    void listaUsaConsultaDeMusicosAtivos() {
        when(musicoRepository.findByMembroAtivoTrueOrderByMembroNomeAsc()).thenReturn(List.of(musico));

        var lista = service.listarMusicos();

        assertEquals(1, lista.size());
        assertEquals(7, lista.get(0).codigo());
        verify(musicoRepository, never()).findAll();
    }

    private MembroRequest request(String perfil) {
        return new MembroRequest("Ana", "123", "ana@example.com", "ana", null, false, true,
                perfil, "Teclado", "Bio", null);
    }
}
