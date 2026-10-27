package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.dto.*;
import br.com.fatec.agendabanda.security.SessaoUsuario;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Repository
public class AgendaSqlRepository {
    private final JdbcTemplate jdbc;
    private static final String PARTICIPACOES = """
        SELECT p.codigo_agenda, mu.codigo, m.nome, mu.instrumento,
               p.status_confirmacao, p.presente, p.data_resposta, p.observacao
        FROM participacao_evento p JOIN musicos mu ON mu.codigo=p.cod_musico
        JOIN membros m ON m.codigo=mu.codigo
        """;

    public AgendaSqlRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<AgendaDto> listar() {
        var participacoes = listarParticipacoes(null).stream().collect(Collectors.groupingBy(ParticipacaoDto::codigoAgenda));
        return jdbc.query("SELECT * FROM dbo.vw_agenda_detalhada ORDER BY dt_hora_in, codigo_agenda",
                (rs, row) -> agenda(rs, participacoes.getOrDefault(rs.getInt("codigo_agenda"), List.of())));
    }

    public AgendaDto detalhar(Integer id) {
        var participacoes = listarParticipacoes(id);
        return jdbc.query("SELECT * FROM dbo.vw_agenda_detalhada WHERE codigo_agenda=?",
                (rs, row) -> agenda(rs, participacoes), id).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado."));
    }

    public List<ParticipacaoDto> listarParticipacoes(Integer id) {
        String sql = PARTICIPACOES + (id == null ? "" : " WHERE p.codigo_agenda=?") + " ORDER BY m.nome, mu.codigo";
        Object[] args = id == null ? new Object[0] : new Object[]{id};
        return jdbc.query(sql, (rs, row) -> new ParticipacaoDto(rs.getInt("codigo_agenda"),
                new MusicoResumoDto(rs.getInt("codigo"), rs.getString("nome"), rs.getString("instrumento")),
                rs.getString("status_confirmacao"), rs.getObject("presente", Boolean.class),
                data(rs, "data_resposta"), rs.getString("observacao")), args);
    }

    public void validarConflitos(AgendaRequest request, Integer id) {
        String status = request.status();
        List<Integer> ids = request.musicosIds();
        if (id != null) {
            var anterior = detalhar(id);
            if (status == null) status = anterior.status();
            if (ids == null) ids = anterior.musicos().stream().map(MusicoResumoDto::codigo).toList();
        }
        if ("CANCELADO".equals(status)) return;
        var conflitos = jdbc.query("""
                SELECT DISTINCT f.titulo FROM dbo.fn_conflitos_agendamento(?,?,?) f
                WHERE f.codigo_local=? OR f.cod_musico IN (SELECT CONVERT(INT,value) FROM OPENJSON(?))
                """, (rs, row) -> rs.getString(1), Timestamp.valueOf(request.dataHoraInicio()),
                Timestamp.valueOf(request.dataHoraFim()), id, request.localId(), jsonIds(ids == null ? List.of() : ids));
        if (!conflitos.isEmpty()) throw new IllegalArgumentException("Conflito de horário com o evento: " + conflitos.get(0));
    }

    public Integer criar(AgendaRequest r) {
        return executar(() -> jdbc.queryForObject("EXEC dbo.usp_criar_evento ?,?,?,?,?,?,?,?,?,?,?,?", Integer.class,
                nv(r.titulo()), nv(r.descricao()), Timestamp.valueOf(r.dataHoraInicio()), Timestamp.valueOf(r.dataHoraFim()),
                r.localId(), r.tipoEventoId(), nv(r.status()), nv(r.repertorio()), nv(r.observacao()), nv(r.pauta()), r.cidadeId(), nv(jsonIds(r.musicosIds()))));
    }

    public void atualizar(Integer id, AgendaRequest r) {
        executar(() -> jdbc.queryForObject("EXEC dbo.usp_atualizar_evento ?,?,?,?,?,?,?,?,?,?,?,?,?", Integer.class,
                id, nv(r.titulo()), nv(r.descricao()), Timestamp.valueOf(r.dataHoraInicio()), Timestamp.valueOf(r.dataHoraFim()),
                r.localId(), r.tipoEventoId(), nv(r.status()), nv(r.repertorio()), nv(r.observacao()), nv(r.pauta()), r.cidadeId(), nv(jsonIds(r.musicosIds()))));
    }

    public ParticipacaoDto atualizarParticipacao(Integer agenda, Integer musico, ParticipacaoAtualizacaoRequest r, boolean propria) {
        executar(() -> jdbc.update("EXEC dbo.usp_atualizar_participacao ?,?,?,?,?,?", agenda, musico,
                nv(r.statusConfirmacao()), new SqlParameterValue(Types.BIT, r.presente()), nv(r.observacao()), propria));
        return listarParticipacoes(agenda).stream().filter(p -> p.musico().codigo().equals(musico)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Participação não encontrada."));
    }

    public void excluir(Integer id, SessaoUsuario sessao) {
        executar(() -> { excluirComLocks(id, sessao); return null; });
    }

    public void bloquearLocal(Integer id) { bloquearRecurso("AgendaBanda:local:" + id); }

    private void bloquearRecurso(String recurso) {
        Integer resultado = jdbc.queryForObject("""
                DECLARE @resultado INT;
                EXEC @resultado = sys.sp_getapplock @Resource=?, @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=5000;
                SELECT @resultado;
                """, Integer.class, recurso);
        if (resultado == null || resultado < 0) throw new ConcurrencyFailureException("Recurso de agenda em uso. Tente novamente.");
    }

    private void excluirComLocks(Integer id, SessaoUsuario sessao) {
        bloquearRecurso("AgendaBanda:evento:" + id);
        var recursos = new TreeSet<String>();
        jdbc.queryForList("SELECT codigo_local FROM agenda WHERE codigo_agenda=?", Integer.class, id)
                .forEach(local -> recursos.add("AgendaBanda:local:" + local));
        jdbc.queryForList("SELECT cod_musico FROM participacao_evento WHERE codigo_agenda=?", Integer.class, id)
                .forEach(musico -> recursos.add("AgendaBanda:musico:" + musico));
        recursos.forEach(this::bloquearRecurso);
        var eventos = jdbc.query("SELECT status,cod_membro_criador FROM agenda WITH (UPDLOCK,HOLDLOCK) WHERE codigo_agenda=?",
                (rs, row) -> Map.entry(rs.getString("status"), rs.getInt("cod_membro_criador")), id);
        if (eventos.isEmpty()) throw new IllegalArgumentException("Evento não encontrado.");
        Boolean permitido = jdbc.queryForObject("""
                SELECT CAST(CASE WHEN EXISTS (SELECT 1 FROM membros WHERE codigo=? AND ativo=1 AND status_cadastro=N'APROVADO'
                    AND (administrador=1 OR codigo=?)) THEN 1 ELSE 0 END AS BIT)
                """, Boolean.class, sessao.codigo(), eventos.get(0).getValue());
        if (!Boolean.TRUE.equals(permitido)) throw new SecurityException("Apenas o criador ou um administrador pode excluir este evento.");
        if ("REALIZADO".equals(eventos.get(0).getKey()) || listarParticipacoes(id).stream().anyMatch(p -> p.presente() != null)) {
            throw new IllegalArgumentException("Não é possível excluir um evento realizado ou com presença registrada.");
        }
        // Complementa o historico do DELETE com os detalhes antes da cascata.
        String detalhes = jdbc.queryForObject("""
                SELECT (SELECT e.repertorio,e.observacao,r.pauta,s.codigo_cidade FROM agenda a
                LEFT JOIN ensaios e ON e.codigo_agenda=a.codigo_agenda
                LEFT JOIN reunioes r ON r.codigo_agenda=a.codigo_agenda LEFT JOIN shows s ON s.codigo_agenda=a.codigo_agenda
                WHERE a.codigo_agenda=? FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) AS detalhes
                """, String.class, id);
        Long limite = jdbc.queryForObject("SELECT COALESCE(MAX(codigo_historico),0) FROM historico_eventos", Long.class);
        jdbc.update("DELETE FROM agenda WHERE codigo_agenda=?", id);
        jdbc.update("""
                UPDATE historico_eventos SET dados_anteriores=JSON_MODIFY(dados_anteriores,'$.detalhes',JSON_QUERY(?))
                WHERE codigo_agenda=? AND codigo_historico>?
                """, nv(detalhes), id, limite);
    }

    private AgendaDto agenda(ResultSet rs, List<ParticipacaoDto> participacoes) throws SQLException {
        Integer cidadeShow = rs.getObject("show_cidade_codigo", Integer.class);
        return new AgendaDto(rs.getInt("codigo_agenda"), rs.getString("titulo"), rs.getString("descricao"),
                data(rs, "dt_hora_in"), data(rs, "dt_hora_fim"),
                new LocalDto(rs.getInt("codigo_local"), rs.getString("local_nome"), rs.getString("endereco"), rs.getString("complemento"),
                        rs.getInt("codigo_cidade"), rs.getString("cidade_nome"), rs.getString("uf")),
                new MembroResumoDto(rs.getInt("criador_codigo"), rs.getString("criador_nome")),
                new TipoEventoDto(rs.getInt("codigo_tipo"), rs.getString("tipo_nome"), rs.getString("tipo_descricao"),
                        rs.getString("categoria_detalhe"), rs.getBoolean("tipo_ativo")), rs.getString("status"), rs.getString("categoria_detalhe"),
                rs.getString("repertorio"), rs.getString("ensaio_observacao"), rs.getString("pauta"),
                cidadeShow == null ? null : new CidadeDto(cidadeShow, rs.getString("show_cidade_nome"), rs.getString("show_uf")),
                participacoes.stream().map(ParticipacaoDto::musico).toList(), participacoes,
                rs.getInt("total_participantes"), rs.getInt("total_confirmados"));
    }

    private LocalDateTime data(ResultSet rs, String coluna) throws SQLException {
        Timestamp valor = rs.getTimestamp(coluna);
        return valor == null ? null : valor.toLocalDateTime();
    }

    private SqlParameterValue nv(String valor) { return new SqlParameterValue(Types.NVARCHAR, valor); }

    private String jsonIds(List<Integer> ids) {
        if (ids == null) return null;
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) throw new IllegalArgumentException("Lista de músicos inválida.");
        return ids.stream().distinct().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
    }

    private <T> T executar(Supplier<T> operacao) {
        try { return operacao.get(); }
        catch (DataAccessException ex) {
            for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
                if (causa instanceof SQLException sql) {
                    for (SQLException erro = sql; erro != null; erro = erro.getNextException()) {
                        if (erro.getErrorCode() == 50004) throw new ConcurrencyFailureException("Recurso de agenda em uso. Aguarde e tente novamente.", ex);
                        if (erro.getErrorCode() == 50003) throw new SecurityException(erro.getMessage());
                        if (erro.getErrorCode() == 50001 || erro.getErrorCode() == 50002) throw new IllegalArgumentException(erro.getMessage());
                    }
                }
            }
            throw ex;
        }
    }
}
