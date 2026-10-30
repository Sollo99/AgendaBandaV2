package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.exception.RecursoNaoEncontradoException;
import br.com.fatec.agendabanda.exception.ConflitoException;

import br.com.fatec.agendabanda.exception.RegraNegocioException;

import br.com.fatec.agendabanda.dto.*;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import static br.com.fatec.agendabanda.repository.ExecucaoSql.executar;

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

    public PaginaDto<AgendaDto> listar(int page, int size, java.time.LocalDate inicio, java.time.LocalDate fim) {
        PaginaDto.validar(page, size);
        if ((inicio != null && (inicio.getYear() < 1900 || inicio.getYear() > 9998))
                || (fim != null && (fim.getYear() < 1900 || fim.getYear() > 9998))
                || (inicio != null && fim != null && fim.isBefore(inicio))) {
            throw new RegraNegocioException("Informe um período válido entre os anos 1900 e 9998.");
        }
        String filtro = " WHERE 1=1";
        var args = new ArrayList<Object>();
        if (inicio != null) { filtro += " AND dt_hora_in>=?"; args.add(Timestamp.valueOf(inicio.atStartOfDay())); }
        if (fim != null) { filtro += " AND dt_hora_in<?"; args.add(Timestamp.valueOf(fim.plusDays(1).atStartOfDay())); }
        Long total = jdbc.queryForObject("SELECT COUNT_BIG(*) FROM agenda" + filtro, Long.class, args.toArray());
        args.add(page * size);
        args.add(size);
        var eventos = jdbc.query("SELECT * FROM dbo.vw_agenda_detalhada" + filtro
                        + " ORDER BY dt_hora_in, codigo_agenda OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                (rs, row) -> agenda(rs, List.of()), args.toArray());
        if (eventos.isEmpty()) return PaginaDto.de(List.of(), page, size, total == null ? 0 : total);
        // Apenas os participantes da pagina, em uma consulta: nao ha consulta por evento.
        String marcadores = eventos.stream().map(e -> "?").collect(Collectors.joining(","));
        var grupos = jdbc.query(PARTICIPACOES + " WHERE p.codigo_agenda IN (" + marcadores + ") ORDER BY m.nome,mu.codigo",
                this::participacao, eventos.stream().map(AgendaDto::codigoAgenda).toArray()).stream()
                .collect(Collectors.groupingBy(ParticipacaoDto::codigoAgenda));
        return PaginaDto.de(eventos.stream().map(e -> e.comParticipacoes(grupos.getOrDefault(e.codigoAgenda(), List.of()))).toList(),
                page, size, total == null ? 0 : total);
    }

    public AgendaDto detalhar(Integer id) {
        var participacoes = listarParticipacoes(id);
        return jdbc.query("SELECT * FROM dbo.vw_agenda_detalhada WHERE codigo_agenda=?",
                (rs, row) -> agenda(rs, participacoes), id).stream().findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
    }

    public List<ParticipacaoDto> listarParticipacoes(Integer id) {
        String sql = PARTICIPACOES + (id == null ? "" : " WHERE p.codigo_agenda=?") + " ORDER BY m.nome, mu.codigo";
        Object[] args = id == null ? new Object[0] : new Object[]{id};
        return jdbc.query(sql, this::participacao, args);
    }

    private ParticipacaoDto participacao(ResultSet rs, int row) throws SQLException {
        return new ParticipacaoDto(rs.getInt("codigo_agenda"),
                new MusicoResumoDto(rs.getInt("codigo"), rs.getString("nome"), rs.getString("instrumento")),
                rs.getString("status_confirmacao"), rs.getObject("presente", Boolean.class),
                data(rs, "data_resposta"), rs.getString("observacao"));
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
        if (!conflitos.isEmpty()) throw new ConflitoException("Conflito de horário com o evento: " + conflitos.get(0));
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
                .orElseThrow(() -> new RecursoNaoEncontradoException("Participação não encontrada."));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void excluir(Integer id) {
        executar(() -> jdbc.queryForObject("EXEC dbo.usp_excluir_evento ?", Integer.class, id));
    }

    public void bloquearLocal(Integer id) { bloquearRecurso("AgendaBanda:local:" + id); }

    private void bloquearRecurso(String recurso) {
        Integer resultado = jdbc.queryForObject("""
                DECLARE @resultado INT;
                DECLARE @iniciar_transacao INT;
                IF @@TRANCOUNT=0 AND (2 & @@OPTIONS)=2
                    SELECT TOP (1) @iniciar_transacao=codigo FROM membros;
                EXEC @resultado = sys.sp_getapplock @Resource=?, @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=5000;
                SELECT @resultado;
                """, Integer.class, recurso);
        if (resultado == null || resultado < 0) throw new ConcurrencyFailureException("Recurso de agenda em uso. Tente novamente.");
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
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) throw new RegraNegocioException("Lista de músicos inválida.");
        return ids.stream().distinct().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
    }

}
