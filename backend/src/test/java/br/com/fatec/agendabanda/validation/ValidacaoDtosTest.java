package br.com.fatec.agendabanda.validation;

import br.com.fatec.agendabanda.dto.*;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class ValidacaoDtosTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    @AfterAll static void fechar() { FACTORY.close(); }
    private Set<String> campos(Object dto) {
        return FACTORY.getValidator().validate(dto).stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    void cadastroAdministrativoRecusaSenhaCurtaCpfInvalidoECamposLongos() {
        var dto = new MembroRequest("n".repeat(101), "11111111111", "e".repeat(110) + "@example.com", "u".repeat(51),
                "12345", false, true, "MUSICO", "i".repeat(81), "b".repeat(501), "e".repeat(101));
        assertTrue(campos(dto).containsAll(Set.of("nome", "cpf", "email", "login", "senha", "instrumento", "biografia", "especialidade")));
    }

    @Test
    void cpfValidoComOuSemPontuacaoESenhaOpcionalSaoAceitosNaEdicao() {
        for (String cpf : Set.of("52998224725", "529.982.247-25")) {
            assertTrue(campos(new MembroRequest("Ana", cpf, "ana@example.com", "ana", "", false, true, "MUSICO", null, null, null)).isEmpty());
        }
    }

    @Test
    void cadastroPublicoExigeSenhaENaoAceitaCpfSemDigitosVerificadoresValidos() {
        assertTrue(campos(new CadastroRequest("Ana", "123", "ana@example.com", "ana", "", null, null)).containsAll(Set.of("cpf", "senha")));
    }

    @Test
    void limitesDeAgendaCorrespondemAsColunasSql() {
        var agora = LocalDateTime.now();
        var dto = new AgendaRequest("t".repeat(121), "d".repeat(801), agora, agora.plusHours(1), 1, 1,
                "AGENDADO", "r".repeat(801), "o".repeat(501), "p".repeat(801), null, null);
        assertTrue(campos(dto).containsAll(Set.of("titulo", "descricao", "repertorio", "observacao", "pauta")));
    }

    @Test
    void enderecoUfETipoTambemSaoValidados() {
        assertTrue(campos(new LocalRequest("l".repeat(121), "e".repeat(181), "c".repeat(121), 1))
                .containsAll(Set.of("nome", "endereco", "complemento")));
        assertTrue(campos(new CidadeRequest("Cidade", "ZZ")).contains("uf"));
        assertTrue(campos(new TipoEventoRequest("t".repeat(51), "d".repeat(301), "OUTRO", true))
                .containsAll(Set.of("nome", "descricao", "categoriaDetalhe")));
    }

    @Test
    void senhaTemLimiteEmBytesCompativelComBcrypt() {
        var curta = new AuthDtos.RedefinirSenhaRequest(1, null, "abcde");
        var unicode = new AuthDtos.RedefinirSenhaRequest(1, null, "á".repeat(37));
        var limite = new AuthDtos.RedefinirSenhaRequest(1, null, "á".repeat(36));
        assertTrue(campos(curta).contains("novaSenha"));
        assertTrue(campos(unicode).contains("novaSenha"));
        assertTrue(campos(limite).isEmpty());
    }
}
