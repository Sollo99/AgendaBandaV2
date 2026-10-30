package br.com.fatec.agendabanda.repository;

import br.com.fatec.agendabanda.security.SessaoUsuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class SessaoSqlRepository {
    private final JdbcTemplate jdbc;
    public SessaoSqlRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    public Optional<SessaoUsuario> buscarAtiva(String tokenHash, LocalDateTime agora) {
        return jdbc.query("""
                SELECT codigo,nome,login,administrador FROM dbo.vw_sessoes_membros
                WHERE token_hash=? AND expira_em>?
                """, (rs,row) -> new SessaoUsuario(rs.getInt("codigo"),rs.getString("nome"),rs.getString("login"),rs.getBoolean("administrador")),
                tokenHash, Timestamp.valueOf(agora)).stream().findFirst();
    }
}
