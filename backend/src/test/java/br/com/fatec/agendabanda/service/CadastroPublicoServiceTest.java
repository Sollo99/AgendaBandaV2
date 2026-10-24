package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.dto.CadastroRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CadastroPublicoServiceTest {
    private final MembroService membros = mock(MembroService.class);
    private final CadastroPublicoService service = new CadastroPublicoService(membros);
    private final CadastroRequest request = new CadastroRequest("Ana", "52998224725", "ana@example.com", "ana", "senha123", null, null);

    @Test
    void colisaoConcorrenteDeUnicidadeNaoRevelaCampoExistente() {
        doThrow(new DataIntegrityViolationException("CPF já existe", new SQLException("UNIQUE", "23000", 2627)))
                .when(membros).criarCadastro(request);
        assertDoesNotThrow(() -> service.solicitar(request));
    }

    @Test
    void falhaDePersistenciaSemDuplicidadeNaoESilenciada() {
        doThrow(new DataIntegrityViolationException("Outro erro", new SQLException("erro", "23000", 547)))
                .when(membros).criarCadastro(request);
        assertThrows(DataIntegrityViolationException.class, () -> service.solicitar(request));
    }
}
