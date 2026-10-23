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
    @Mock ParticipacaoEventoRepository participacaoRepository;
    @InjectMocks MembroService service;

    private Membro membro;
    private Musico musico;

    @BeforeEach
    void preparar() {
        membro = new Membro();
        membro.setCodigo(7);
        membro.setNome("Ana");
        membro.setAtivo(true);
        membro.setPerfil("MUSICO");
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
    void cadastroPublicoCriaMusicoAtivoSemPermissaoAdministrativa() {
        when(membroRepository.save(any())).thenAnswer(invocation -> {
            Membro novo = invocation.getArgument(0);
            novo.setCodigo(8);
            return novo;
        });
        when(passwordEncoder.encode("senha123")).thenReturn("hash-da-senha");

        service.criarCadastro(new CadastroRequest("Ana", "52998224725", "ana@example.com", "ana", "senha123", "Violão", "Bio"));

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
        return new MembroRequest("Ana", "52998224725", "ana@example.com", "ana", null, false, true,
                perfil, "Teclado", "Bio", null);
    }

    @Test
    void ultimoAdministradorNaoPodeSerDesativadoPeloPatch() {
        membro.setAdministrador(true);
        when(membroRepository.bloquearAdministradoresAtivos()).thenReturn(List.of(membro));
        when(membroRepository.findById(7)).thenReturn(Optional.of(membro));
        assertThrows(IllegalArgumentException.class, () -> service.alterarStatus(7, false));
        assertTrue(membro.getAtivo());
        verify(membroRepository, never()).save(any());
    }

    @Test
    void ultimoAdministradorNaoPodePerderPermissaoNaEdicao() {
        membro.setAdministrador(true);
        when(membroRepository.bloquearAdministradoresAtivos()).thenReturn(List.of(membro));
        when(membroRepository.findById(7)).thenReturn(Optional.of(membro));
        assertThrows(IllegalArgumentException.class, () -> service.atualizar(7, request("MUSICO")));
        assertTrue(membro.getAdministrador());
        verify(membroRepository, never()).save(any());
    }

    @Test
    void ultimoAdministradorNaoPodeSerDesativadoPelaEdicaoCompleta() {
        membro.setAdministrador(true);
        when(membroRepository.bloquearAdministradoresAtivos()).thenReturn(List.of(membro));
        when(membroRepository.findById(7)).thenReturn(Optional.of(membro));
        var request = new MembroRequest("Ana", "52998224725", "ana@example.com", "ana", null,
                true, false, "MUSICO", "Teclado", null, null);
        assertThrows(IllegalArgumentException.class, () -> service.atualizar(7, request));
        assertTrue(membro.getAtivo());
    }

    @Test
    void permiteDesativacaoQuandoOutroAdministradorContinuaAtivo() {
        membro.setAdministrador(true);
        var outro = new Membro(); outro.setCodigo(8); outro.setAtivo(true); outro.setAdministrador(true);
        when(membroRepository.bloquearAdministradoresAtivos()).thenReturn(List.of(membro, outro));
        when(membroRepository.findById(7)).thenReturn(Optional.of(membro));
        when(membroRepository.save(membro)).thenReturn(membro);
        service.alterarStatus(7, false);
        assertFalse(membro.getAtivo());
        assertTrue(outro.getAtivo());
    }

    @Test
    void trocaPerfilRemoveSubtipoAntesDeAlterarDiscriminador() {
        when(membroRepository.findById(7)).thenReturn(Optional.of(membro));
        when(membroRepository.save(membro)).thenReturn(membro);
        when(musicoRepository.findById(7)).thenReturn(Optional.of(musico));
        service.atualizar(7, request("PRODUTOR"));
        var ordem = inOrder(musicoRepository, membroRepository, produtorRepository);
        ordem.verify(musicoRepository).delete(musico);
        ordem.verify(musicoRepository).flush();
        ordem.verify(membroRepository).saveAndFlush(membro);
        ordem.verify(produtorRepository).save(any());
        assertEquals("PRODUTOR", membro.getPerfil());
    }

    @Test
    void cpfFormatadoENormalizadoAntesDeValidarUnicidade() {
        when(membroRepository.save(any())).thenAnswer(invocation -> {
            Membro novo = invocation.getArgument(0); novo.setCodigo(8); return novo;
        });
        service.criarCadastro(new CadastroRequest("Ana", "529.982.247-25", "ana@example.com", "ana", "senha123", null, null));
        verify(membroRepository).findByCpf("52998224725");
        var captor = ArgumentCaptor.forClass(Musico.class);
        verify(musicoRepository).save(captor.capture());
        assertEquals("52998224725", captor.getValue().getMembro().getCpf());
        assertEquals("MUSICO", captor.getValue().getMembro().getPerfil());
    }
}
