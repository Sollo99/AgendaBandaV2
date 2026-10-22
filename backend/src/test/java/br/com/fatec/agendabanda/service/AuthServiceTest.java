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
        when(membroRepository.findByLogin("ana")).thenReturn(Optional.of(membro));
        when(membroRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(membro));
        when(redefinicaoRepository.findTopByMembroCodigoOrderBySolicitadoEmDesc(7)).thenReturn(Optional.of(solicitacao));

        var porLogin = service.solicitarRedefinicaoSenha(new RecuperarSenhaRequest("ana", CHAVE));
        var porEmail = service.solicitarRedefinicaoSenha(new RecuperarSenhaRequest("ana@example.com", CHAVE));

        assertTrue(porLogin.podeRedefinir());
        assertEquals(porLogin.codigoSolicitacao(), porEmail.codigoSolicitacao());
        assertTrue(porEmail.podeRedefinir());
        verify(redefinicaoRepository, never()).save(any());
    }

    @Test
    void outraChaveNaoSubstituiPedidoAprovadoNemRecebeCodigo() {
        when(membroRepository.findByLogin("ana")).thenReturn(Optional.of(membro));
        when(redefinicaoRepository.findTopByMembroCodigoOrderBySolicitadoEmDesc(7)).thenReturn(Optional.of(solicitacao));

        var resposta = service.solicitarRedefinicaoSenha(new RecuperarSenhaRequest("ana", "outra-chave-sem-permissao"));

        assertFalse(resposta.podeRedefinir());
        assertNull(resposta.codigoSolicitacao());
        assertNull(resposta.codigoRecuperacao());
        verify(redefinicaoRepository, never()).save(any());
    }

    @Test
    void idSequencialSemChaveCorretaNaoRedefineSenha() {
        when(redefinicaoRepository.findByCodigoRedefinicaoAndStatus(12, "APROVADA")).thenReturn(Optional.of(solicitacao));

        assertThrows(IllegalArgumentException.class, () -> service.redefinirSenha(new RedefinirSenhaRequest(12, "incorreta", "novaSenha")));
        verify(membroService, never()).redefinirSenha(any(), any());
    }

    @Test
    void conclusaoInvalidaSessoesEConsomeSolicitacao() {
        when(redefinicaoRepository.findByCodigoRedefinicaoAndStatus(12, "APROVADA")).thenReturn(Optional.of(solicitacao));

        service.redefinirSenha(new RedefinirSenhaRequest(12, CHAVE, "novaSenha"));

        assertEquals("CONCLUIDA", solicitacao.getStatus());
        assertNotNull(solicitacao.getConcluidoEm());
        verify(membroService).redefinirSenha(membro, "novaSenha");
        verify(tokenService).removerSessoesDoMembro(7);
    }
}
