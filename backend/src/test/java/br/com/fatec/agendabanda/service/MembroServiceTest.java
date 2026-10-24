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
    @Mock CicloRecuperacaoService ciclo;
    @Mock br.com.fatec.agendabanda.security.TokenService tokenService;
    MembroService service;

    private Membro membro;
    private Musico musico;

    @BeforeEach
    void preparar() {
        service = new MembroService(membroRepository, musicoRepository, produtorRepository, passwordEncoder, mapperService, participacaoRepository, ciclo, tokenService,
                java.time.Clock.fixed(java.time.Instant.parse("2026-10-24T12:00:00Z"), java.time.ZoneOffset.UTC));
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
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
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
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
        when(membroRepository.save(membro)).thenReturn(membro);
        when(musicoRepository.findById(7)).thenReturn(Optional.of(musico));
        when(participacaoRepository.existsByMusicoCodigo(7)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.atualizar(7, request("MEMBRO")));
        verify(musicoRepository, never()).delete(any());
    }

    @Test
    void cadastroPublicoCriaMusicoPendenteSemAcessoOuPermissaoAdministrativa() {
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
        assertFalse(novo.getMembro().getAtivo());
        assertEquals("PENDENTE", novo.getMembro().getStatusCadastro());
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
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
        assertThrows(IllegalArgumentException.class, () -> service.alterarStatus(7, false));
        assertTrue(membro.getAtivo());
        verify(membroRepository, never()).save(any());
    }

    @Test
    void ultimoAdministradorNaoPodePerderPermissaoNaEdicao() {
        membro.setAdministrador(true);
        when(membroRepository.bloquearAdministradoresAtivos()).thenReturn(List.of(membro));
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
        assertThrows(IllegalArgumentException.class, () -> service.atualizar(7, request("MUSICO")));
        assertTrue(membro.getAdministrador());
        verify(membroRepository, never()).save(any());
    }

    @Test
    void ultimoAdministradorNaoPodeSerDesativadoPelaEdicaoCompleta() {
        membro.setAdministrador(true);
        when(membroRepository.bloquearAdministradoresAtivos()).thenReturn(List.of(membro));
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
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
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
        when(membroRepository.save(membro)).thenReturn(membro);
        service.alterarStatus(7, false);
        assertFalse(membro.getAtivo());
        assertTrue(outro.getAtivo());
    }

    @Test
    void trocaPerfilRemoveSubtipoAntesDeAlterarDiscriminador() {
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
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

    @Test
    void cadastroDuplicadoNaoAlteraContaExistenteNemCriaOutroPerfil() {
        when(membroRepository.findByCpf("52998224725")).thenReturn(Optional.of(membro));
        service.criarCadastro(new CadastroRequest("Ana", "52998224725", "ana@example.com", "ana", "senha123", null, null));
        verify(membroRepository, never()).save(any());
        verify(musicoRepository, never()).save(any());
    }

    @Test
    void statusDeAtivoNaoAprovaCadastroPendente() {
        membro.setStatusCadastro("PENDENTE"); membro.setAtivo(false);
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
        assertThrows(IllegalArgumentException.class, () -> service.alterarStatus(7, true));
        assertFalse(membro.getAtivo());
        verify(membroRepository, never()).save(any());
    }

    @Test
    void edicaoCompletaNaoBurlaAprovacaoDoCadastro() {
        membro.setStatusCadastro("PENDENTE"); membro.setAtivo(false);
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
        assertThrows(IllegalArgumentException.class, () -> service.atualizar(7, request("MUSICO")));
        assertEquals("PENDENTE", membro.getStatusCadastro());
    }

    @Test
    void decisaoAdministrativaAprovaERegistraResponsavel() {
        var admin = new Membro(); admin.setCodigo(1);
        membro.setStatusCadastro("PENDENTE"); membro.setAtivo(false);
        when(membroRepository.bloquearAdministradoresAtivos()).thenReturn(List.of(admin));
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
        when(membroRepository.save(membro)).thenReturn(membro);
        service.decidirCadastro(7, true, 1);
        assertEquals("APROVADO", membro.getStatusCadastro());
        assertTrue(membro.getAtivo());
        assertFalse(membro.getAdministrador());
        assertEquals(1, membro.getCadastroDecididoPor());
        assertNotNull(membro.getCadastroDecididoEm());
    }

    @Test
    void recusaNaoLiberaAcesso() {
        var admin = new Membro(); admin.setCodigo(1);
        membro.setStatusCadastro("PENDENTE"); membro.setAtivo(false);
        when(membroRepository.bloquearAdministradoresAtivos()).thenReturn(List.of(admin));
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
        when(membroRepository.save(membro)).thenReturn(membro);
        service.decidirCadastro(7, false, 1);
        assertEquals("RECUSADO", membro.getStatusCadastro());
        assertFalse(membro.getAtivo());
    }

    @Test
    void trocaAdministrativaDeSenhaRevogaPedidosESessoes() {
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
        when(membroRepository.save(membro)).thenReturn(membro);
        service.atualizar(7, new MembroRequest("Ana", "52998224725", "ana@example.com", "ana", "novaSenha", false, true, "MUSICO", null, null, null));
        verify(ciclo).cancelarAbertas(eq(7), anyString());
        verify(tokenService).removerSessoesDoMembro(7);
    }

    @Test
    void trocaDeSenhaPeloPerfilTambemRevogaPedidosESessoes() {
        membro.setSenha("hash-atual");
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
        when(membroRepository.save(membro)).thenReturn(membro);
        when(passwordEncoder.matches("senhaAtual", "hash-atual")).thenReturn(true);
        service.atualizarProprio(7, new br.com.fatec.agendabanda.dto.PerfilRequest("Ana", "52998224725", "ana@example.com", "ana", "senhaAtual", "novaSenha", null, null));
        verify(ciclo).cancelarAbertas(eq(7), anyString());
        verify(tokenService).removerSessoesDoMembro(7);
    }

    @Test
    void redefinicaoPeloPedidoUsaMesmaRevogacaoCentral() {
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
        service.redefinirSenha(membro, "novaSenha");
        verify(ciclo).cancelarAbertas(eq(7), anyString());
        verify(tokenService).removerSessoesDoMembro(7);
    }
}
