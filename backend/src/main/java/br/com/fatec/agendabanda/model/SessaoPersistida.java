package br.com.fatec.agendabanda.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessoes")
public class SessaoPersistida {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "codigo_sessao") private Integer codigoSessao;
    @Column(name = "token_hash") private String tokenHash;
    @Column(name = "codigo_membro") private Integer codigoMembro;
    @Column(name = "criado_em") private LocalDateTime criadoEm;
    @Column(name = "expira_em") private LocalDateTime expiraEm;
    public Integer getCodigoSessao() { return codigoSessao; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public Integer getCodigoMembro() { return codigoMembro; }
    public void setCodigoMembro(Integer codigoMembro) { this.codigoMembro = codigoMembro; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public void setExpiraEm(LocalDateTime expiraEm) { this.expiraEm = expiraEm; }
}
