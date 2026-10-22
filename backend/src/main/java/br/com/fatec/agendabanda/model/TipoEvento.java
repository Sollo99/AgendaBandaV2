package br.com.fatec.agendabanda.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tipos_evento")
public class TipoEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo_tipo")
    private Integer codigoTipo;

    private String nome;
    private String descricao;
    @Column(name = "categoria_detalhe")
    private String categoriaDetalhe;
    private Boolean ativo;

    public Integer getCodigoTipo() { return codigoTipo; }
    public void setCodigoTipo(Integer codigoTipo) { this.codigoTipo = codigoTipo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getCategoriaDetalhe() { return categoriaDetalhe; }
    public void setCategoriaDetalhe(String categoriaDetalhe) { this.categoriaDetalhe = categoriaDetalhe; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
}
