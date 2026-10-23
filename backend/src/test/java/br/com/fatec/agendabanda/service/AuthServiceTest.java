package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.AuthDtos.RecuperarSenhaRequest;
import br.com.fatec.agendabanda.dto.AuthDtos.RedefinirSenhaRequest;
import br.com.fatec.agendabanda.model.Membro;
import br.com.fatec.agendabanda.model.RedefinicaoSenha;
import br.com.fatec.agendabanda.repository.MembroRepository;
import br.com.fatec.agendabanda.repository.RedefinicaoSenhaRepository;
import br.com.fatec.agendabanda.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final String CHAVE = "codigo-de-recuperacao-aleatorio-com-mais-de-32-caracteres";
    @Mock MembroRepository membroRepository;
    @Mock BCryptPasswordEncoder passwordEncoder;
    @Mock TokenService tokenService;
    @Mock MapperService mapperService;
    @Mock RedefinicaoSenhaRepository redefinicaoRepository;
    @Mock MembroService membroService;
    @InjectMocks AuthService service;
    private Membro membro;
    private RedefinicaoSenha solicitacao;

    @BeforeEach
    void preparar() throws Exception {
        membro = new Membro();
        membro.setCodigo(7);
        membro.setAtivo(true);
        solicitacao = new RedefinicaoSenha();
        solicitacao.setCodigoRedefinicao(12);
        solicitacao.setMembro(membro);
        solicitacao.setStatus("APROVADA");
        solicitacao.setChaveHash(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(CHAVE.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void loginEEmailAcessamAMesmaSolicitacaoComAMesmaChave() {
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
        when(membroRepository.buscarCodigoPorIdentificador("ana")).thenReturn(Optional.of(7));
        when(membroRepository.buscarCodigoPorIdentificador("ana@example.com")).thenReturn(Optional.of(7));
        when(redefinicaoRepository.findByMembroCodigoAndStatusIn(7, java.util.List.of("PENDENTE", "APROVADA"))).thenReturn(Optional.of(solicitacao));

        var porLogin = service.solicitarRedefinicaoSenha(new RecuperarSenhaRequest("ana", CHAVE));
        var porEmail = service.solicitarRedefinicaoSenha(new RecuperarSenhaRequest("ana@example.com", CHAVE));

        assertTrue(porLogin.podeRedefinir());
        assertEquals(porLogin.codigoSolicitacao(), porEmail.codigoSolicitacao());
        assertTrue(porEmail.podeRedefinir());
        verify(redefinicaoRepository, never()).save(any());
    }

    @Test
    void outraChaveNaoSubstituiPedidoAprovadoNemRecebeAutorizacao() {
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
        when(membroRepository.buscarCodigoPorIdentificador("ana")).thenReturn(Optional.of(7));
        when(redefinicaoRepository.findByMembroCodigoAndStatusIn(7, java.util.List.of("PENDENTE", "APROVADA"))).thenReturn(Optional.of(solicitacao));

        var resposta = service.solicitarRedefinicaoSenha(new RecuperarSenhaRequest("ana", "outra-chave-sem-permissao"));

        assertFalse(resposta.podeRedefinir());
        assertNull(resposta.codigoSolicitacao());
        assertEquals("outra-chave-sem-permissao", resposta.codigoRecuperacao());
        verify(redefinicaoRepository, never()).save(any());
    }

    @Test
    void idSequencialSemChaveCorretaNaoRedefineSenha() {
        when(redefinicaoRepository.findById(12)).thenReturn(Optional.of(solicitacao));
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
        when(redefinicaoRepository.findByCodigoRedefinicaoAndStatus(12, "APROVADA")).thenReturn(Optional.of(solicitacao));

        assertThrows(IllegalArgumentException.class, () -> service.redefinirSenha(new RedefinirSenhaRequest(12, "incorreta", "novaSenha")));
        verify(membroService, never()).redefinirSenha(any(), any());
    }

    @Test
    void conclusaoInvalidaSessoesEConsomeSolicitacao() {
        when(redefinicaoRepository.findById(12)).thenReturn(Optional.of(solicitacao));
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
        when(redefinicaoRepository.findByCodigoRedefinicaoAndStatus(12, "APROVADA")).thenReturn(Optional.of(solicitacao));

        service.redefinirSenha(new RedefinirSenhaRequest(12, CHAVE, "novaSenha"));

        assertEquals("CONCLUIDA", solicitacao.getStatus());
        assertNotNull(solicitacao.getConcluidoEm());
        verify(membroService).redefinirSenha(membro, "novaSenha");
        verify(tokenService).removerSessoesDoMembro(7);
    }

    @Test
    void respostasSaoIguaisParaContaInexistentePendenteInativaEPedidoDeOutroNavegador() {
        var inexistente = service.solicitarRedefinicaoSenha(new RecuperarSenhaRequest("nao-existe", CHAVE));
        when(membroRepository.buscarCodigoPorIdentificador("ana")).thenReturn(Optional.of(7));
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
        solicitacao.setStatus("PENDENTE");
        when(redefinicaoRepository.findByMembroCodigoAndStatusIn(7, java.util.List.of("PENDENTE", "APROVADA"))).thenReturn(Optional.of(solicitacao));
        var pendente = service.solicitarRedefinicaoSenha(new RecuperarSenhaRequest("ana", CHAVE));
        solicitacao.setChaveHash("hash-de-outro-navegador");
        var outroNavegador = service.solicitarRedefinicaoSenha(new RecuperarSenhaRequest("ana", CHAVE));
        membro.setAtivo(false);
        var inativa = service.solicitarRedefinicaoSenha(new RecuperarSenhaRequest("ana", CHAVE));
        assertEquals(inexistente, pendente);
        assertEquals(inexistente, outroNavegador);
        assertEquals(inexistente, inativa);
        verify(redefinicaoRepository, never()).save(any());
    }

    @Test
    void repeticaoDePedidoNaoCriaOutraSolicitacaoPendente() {
        solicitacao.setStatus("PENDENTE");
        when(membroRepository.buscarCodigoPorIdentificador("ana")).thenReturn(Optional.of(7));
        when(membroRepository.bloquearPorCodigo(7)).thenReturn(Optional.of(membro));
        when(redefinicaoRepository.findByMembroCodigoAndStatusIn(7, java.util.List.of("PENDENTE", "APROVADA")))
                .thenReturn(Optional.empty(), Optional.of(solicitacao));
        var primeiro = service.solicitarRedefinicaoSenha(new RecuperarSenhaRequest("ana", CHAVE));
        var segundo = service.solicitarRedefinicaoSenha(new RecuperarSenhaRequest("ana", CHAVE));
        assertEquals(primeiro, segundo);
        verify(redefinicaoRepository, times(1)).save(any());
        var ordem = inOrder(membroRepository, redefinicaoRepository);
        ordem.verify(membroRepository).bloquearPorCodigo(7);
        ordem.verify(redefinicaoRepository).findByMembroCodigoAndStatusIn(7, java.util.List.of("PENDENTE", "APROVADA"));
        ordem.verify(redefinicaoRepository).save(any());
    }
}
