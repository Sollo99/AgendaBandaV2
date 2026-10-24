package br.com.fatec.agendabanda.controller;

import br.com.fatec.agendabanda.dto.AuthDtos.RecuperarSenhaRequest;
import br.com.fatec.agendabanda.dto.AuthDtos.RecuperarSenhaResponse;
import br.com.fatec.agendabanda.dto.AuthDtos.RedefinirSenhaRequest;
import br.com.fatec.agendabanda.security.TokenService;
import br.com.fatec.agendabanda.service.AuthService;
import br.com.fatec.agendabanda.service.MembroService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    private static final String CHAVE = "codigo-portatil-com-pelo-menos-32-caracteres";
    @Mock AuthService authService;
    @Mock TokenService tokenService;
    @Mock br.com.fatec.agendabanda.service.CadastroPublicoService cadastroPublicoService;
    @InjectMocks AuthController controller;

    @Test
    void novaSolicitacaoGeraCodigoEAtribuiCookieHttpOnly() {
        when(authService.solicitarRedefinicaoSenha(any())).thenAnswer(invocation -> {
            RecuperarSenhaRequest request = invocation.getArgument(0);
            return new RecuperarSenhaResponse(false, null, "Aguardando aprovação.", request.chaveSolicitacao());
        });
        var http = new MockHttpServletRequest();
        http.setSecure(true);
        var response = new MockHttpServletResponse();

        var resultado = controller.recuperarSenha(new RecuperarSenhaRequest("ana", null), null, http, response);

        assertNotNull(resultado.codigoRecuperacao());
        assertTrue(resultado.codigoRecuperacao().length() >= 32);
        String cookie = response.getHeader("Set-Cookie");
        assertNotNull(cookie);
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=Lax"));
    }

    @Test
    void cookieEReutilizadoAoInformarEmailEmVezDoLogin() {
        when(authService.solicitarRedefinicaoSenha(any()))
                .thenReturn(new RecuperarSenhaResponse(true, 12, "Aprovada.", CHAVE));

        controller.recuperarSenha(new RecuperarSenhaRequest("ana@example.com", null), CHAVE,
                new MockHttpServletRequest(), new MockHttpServletResponse());

        var captor = ArgumentCaptor.forClass(RecuperarSenhaRequest.class);
        verify(authService).solicitarRedefinicaoSenha(captor.capture());
        assertEquals(CHAVE, captor.getValue().chaveSolicitacao());
    }

    @Test
    void codigoPortatilPermiteConcluirSemCookie() {
        var response = new MockHttpServletResponse();

        controller.redefinirSenha(new RedefinirSenhaRequest(12, CHAVE, "novaSenha"), null,
                new MockHttpServletRequest(), response);

        verify(authService).redefinirSenha(new RedefinirSenhaRequest(12, CHAVE, "novaSenha"));
        assertTrue(response.getHeader("Set-Cookie").contains("Max-Age=0"));
    }

    @Test
    void redefinicaoSemCookieNemCodigoERecusada() {
        assertThrows(IllegalArgumentException.class, () -> controller.redefinirSenha(
                new RedefinirSenhaRequest(12, null, "novaSenha"), null,
                new MockHttpServletRequest(), new MockHttpServletResponse()));
        verifyNoInteractions(authService);
    }

    @Test
    void cadastroPublicoRetorna202EMensagemSemDadosDoMembro() {
        var request = new br.com.fatec.agendabanda.dto.CadastroRequest("Ana", "52998224725", "ana@example.com", "ana", "senha123", null, null);
        var resposta = controller.cadastro(request);
        assertEquals(202, resposta.getStatusCode().value());
        assertEquals(java.util.Set.of("mensagem"), resposta.getBody().keySet());
        verify(cadastroPublicoService).solicitar(request);
    }

    @Test
    void expiracaoDaRecuperacaoTambemRemoveCookie() {
        doThrow(new br.com.fatec.agendabanda.exception.RecuperacaoExpiradaException()).when(authService).redefinirSenha(any());
        var response = new MockHttpServletResponse();
        assertThrows(br.com.fatec.agendabanda.exception.RecuperacaoExpiradaException.class, () -> controller.redefinirSenha(
                new RedefinirSenhaRequest(12, CHAVE, "novaSenha"), null, new MockHttpServletRequest(), response));
        assertTrue(response.getHeader("Set-Cookie").contains("Max-Age=0"));
    }
}
