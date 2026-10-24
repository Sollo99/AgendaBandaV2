package br.com.fatec.agendabanda.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "redefinicoes_senha")
public class RedefinicaoSenha {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo_redefinicao")
    private Integer codigoRedefinicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_membro")
    private Membro membro;

    @Column(name = "status")
    private String status;

    @Column(name = "chave_hash")
    private String chaveHash;

    @Column(name = "solicitado_em")
    private LocalDateTime solicitadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_administrador")
    private Membro administrador;

    @Column(name = "decidido_em")
    private LocalDateTime decididoEm;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    @Column(name = "expira_em")
    private LocalDateTime expiraEm;
    @Column(name = "encerrado_em")
    private LocalDateTime encerradoEm;
    @Column(name = "motivo_encerramento")
    private String motivoEncerramento;

    public LocalDateTime getExpiraEm() { return expiraEm; }
    public void setExpiraEm(LocalDateTime expiraEm) { this.expiraEm = expiraEm; }
    public LocalDateTime getEncerradoEm() { return encerradoEm; }
    public void setEncerradoEm(LocalDateTime encerradoEm) { this.encerradoEm = encerradoEm; }
    public String getMotivoEncerramento() { return motivoEncerramento; }
    public void setMotivoEncerramento(String motivoEncerramento) { this.motivoEncerramento = motivoEncerramento; }

    public Integer getCodigoRedefinicao() { return codigoRedefinicao; }
    public void setCodigoRedefinicao(Integer codigoRedefinicao) { this.codigoRedefinicao = codigoRedefinicao; }
    public Membro getMembro() { return membro; }
    public void setMembro(Membro membro) { this.membro = membro; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getChaveHash() { return chaveHash; }
    public void setChaveHash(String chaveHash) { this.chaveHash = chaveHash; }
    public LocalDateTime getSolicitadoEm() { return solicitadoEm; }
    public void setSolicitadoEm(LocalDateTime solicitadoEm) { this.solicitadoEm = solicitadoEm; }
    public Membro getAdministrador() { return administrador; }
    public void setAdministrador(Membro administrador) { this.administrador = administrador; }
    public LocalDateTime getDecididoEm() { return decididoEm; }
    public void setDecididoEm(LocalDateTime decididoEm) { this.decididoEm = decididoEm; }
    public LocalDateTime getConcluidoEm() { return concluidoEm; }
    public void setConcluidoEm(LocalDateTime concluidoEm) { this.concluidoEm = concluidoEm; }
}
