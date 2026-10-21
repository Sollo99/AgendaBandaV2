package br.com.fatec.agendabanda.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "participacao_evento")
public class ParticipacaoEvento {
    @EmbeddedId
    private ParticipacaoEventoId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("codigoAgenda")
    @JoinColumn(name = "codigo_agenda")
    private Agenda agenda;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("codMusico")
    @JoinColumn(name = "cod_musico")
    private Musico musico;

    @Column(name = "status_confirmacao")
    private String statusConfirmacao;

    private Boolean presente;

    @Column(name = "data_resposta")
    private LocalDateTime dataResposta;

    private String observacao;

    public ParticipacaoEventoId getId() { return id; }
    public void setId(ParticipacaoEventoId id) { this.id = id; }
    public Agenda getAgenda() { return agenda; }
    public void setAgenda(Agenda agenda) { this.agenda = agenda; }
    public Musico getMusico() { return musico; }
    public void setMusico(Musico musico) { this.musico = musico; }
    public String getStatusConfirmacao() { return statusConfirmacao; }
    public void setStatusConfirmacao(String statusConfirmacao) { this.statusConfirmacao = statusConfirmacao; }
    public Boolean getPresente() { return presente; }
    public void setPresente(Boolean presente) { this.presente = presente; }
    public LocalDateTime getDataResposta() { return dataResposta; }
    public void setDataResposta(LocalDateTime dataResposta) { this.dataResposta = dataResposta; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
