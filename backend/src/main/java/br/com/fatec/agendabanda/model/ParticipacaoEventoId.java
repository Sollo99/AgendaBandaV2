package br.com.fatec.agendabanda.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ParticipacaoEventoId implements Serializable {
    @Column(name = "codigo_agenda")
    private Integer codigoAgenda;

    @Column(name = "cod_musico")
    private Integer codMusico;

    public ParticipacaoEventoId() {}

    public ParticipacaoEventoId(Integer codigoAgenda, Integer codMusico) {
        this.codigoAgenda = codigoAgenda;
        this.codMusico = codMusico;
    }

    public Integer getCodigoAgenda() { return codigoAgenda; }
    public Integer getCodMusico() { return codMusico; }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) return true;
        if (!(objeto instanceof ParticipacaoEventoId outro)) return false;
        return Objects.equals(codigoAgenda, outro.codigoAgenda) && Objects.equals(codMusico, outro.codMusico);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoAgenda, codMusico);
    }
}
