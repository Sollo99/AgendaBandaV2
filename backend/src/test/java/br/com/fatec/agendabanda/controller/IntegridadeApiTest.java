package br.com.fatec.agendabanda.controller;

import br.com.fatec.agendabanda.exception.ApiExceptionHandler;
import br.com.fatec.agendabanda.security.TokenService;
import br.com.fatec.agendabanda.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IntegridadeApiTest {
    @Test
    void exclusaoFisicaDeMembrosNaoTemRota() throws Exception {
        var controller = new MembroController(mock(MembroService.class), mock(TokenService.class), mock(PermissaoService.class));
        MockMvcBuilders.standaloneSetup(controller).build().perform(delete("/api/membros/7"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void violacaoDeIntegridadeRetornaConflitoSemExporDetalheSql() {
        var response = new ApiExceptionHandler().erroIntegridade(new DataIntegrityViolationException("senha e SQL internos"));
        assertEquals(409, response.getStatusCode().value());
        assertFalse(response.getBody().get("erro").contains("SQL internos"));
    }
}
