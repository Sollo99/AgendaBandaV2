package br.com.fatec.agendabanda.service;

import br.com.fatec.agendabanda.model.*;
import br.com.fatec.agendabanda.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PrivacidadeAgendaTest {
    @Test
    void resumosDeCriadorEMusicoNaoContemDadosPessoaisOuCredenciais() {
        var membro = new Membro(); membro.setCodigo(7); membro.setNome("Ana");
        membro.setCpf("52998224725"); membro.setEmail("ana@example.com"); membro.setLogin("ana"); membro.setSenha("segredo");
        var musico = new Musico(); musico.setCodigo(7); musico.setMembro(membro); musico.setInstrumento("Violão");
        var agenda = new Agenda(); agenda.setCodigoAgenda(10);
        var participacao = new ParticipacaoEvento(); participacao.setAgenda(agenda); participacao.setMusico(musico);
        var mapper = new MapperService(mock(MusicoRepository.class), mock(ProdutorRepository.class));
        var jackson = new ObjectMapper();
        var criadorJson = jackson.valueToTree(mapper.membroResumoDto(membro));
        var musicoJson = jackson.valueToTree(mapper.participacaoDto(participacao)).get("musico");
        assertEquals(2, criadorJson.size());
        assertEquals(3, musicoJson.size());
        for (String campo : Set.of("cpf", "email", "login", "senha", "administrador", "biografia")) {
            assertFalse(criadorJson.has(campo)); assertFalse(musicoJson.has(campo));
        }
        assertEquals("Ana", musicoJson.get("nome").asText());
    }
}
