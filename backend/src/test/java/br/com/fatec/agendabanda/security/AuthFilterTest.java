package br.com.fatec.agendabanda.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AuthFilterTest {
    @Test
    void pedidosPublicosEConclusoesCompartilhamLimiteERetornam429SemExigirLogin() throws Exception {
        var filter = new AuthFilter(mock(TokenService.class), new LimitadorRecuperacao());
        var chamadas = new AtomicInteger();
        for (int i = 0; i < 11; i++) {
            String rota = i % 2 == 0 ? "/api/auth/recuperar-senha" : "/api/auth/redefinir-senha";
            var request = new MockHttpServletRequest("POST", rota);
            request.setRemoteAddr("192.0.2.1");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> chamadas.incrementAndGet());
            if (i < 10) assertEquals(200, response.getStatus());
            else {
                assertEquals(429, response.getStatus());
                assertNotNull(response.getHeader("Retry-After"));
                assertTrue(response.getContentAsString().contains("Muitas tentativas"));
            }
        }
        assertEquals(10, chamadas.get());
    }
}
