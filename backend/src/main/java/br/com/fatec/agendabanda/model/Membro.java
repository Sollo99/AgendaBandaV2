package br.com.fatec.agendabanda.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "membros")
public class Membro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo")
    private Integer codigo;

    private String nome;
    private String cpf;
    private String email;
    private String login;
    private String senha;
    private Boolean administrador;
    private Boolean ativo;
    private String perfil = "MEMBRO";
    @Column(name = "status_cadastro")
    private String statusCadastro = "APROVADO";
    @Column(name = "cadastro_decidido_em")
    private LocalDateTime cadastroDecididoEm;
    @Column(name = "cadastro_decidido_por")
    private Integer cadastroDecididoPor;
    @Column(name = "identificadores_liberados_em")
    private LocalDateTime identificadoresLiberadosEm;
    @Column(name = "identificadores_liberados_por")
    private Integer identificadoresLiberadosPor;

    public LocalDateTime getIdentificadoresLiberadosEm() { return identificadoresLiberadosEm; }
    public void setIdentificadoresLiberadosEm(LocalDateTime valor) { this.identificadoresLiberadosEm = valor; }
    public Integer getIdentificadoresLiberadosPor() { return identificadoresLiberadosPor; }
    public void setIdentificadoresLiberadosPor(Integer valor) { this.identificadoresLiberadosPor = valor; }

    public String getStatusCadastro() { return statusCadastro; }
    public void setStatusCadastro(String statusCadastro) { this.statusCadastro = statusCadastro; }
    public LocalDateTime getCadastroDecididoEm() { return cadastroDecididoEm; }
    public void setCadastroDecididoEm(LocalDateTime cadastroDecididoEm) { this.cadastroDecididoEm = cadastroDecididoEm; }
    public Integer getCadastroDecididoPor() { return cadastroDecididoPor; }
    public void setCadastroDecididoPor(Integer cadastroDecididoPor) { this.cadastroDecididoPor = cadastroDecididoPor; }

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    public Integer getCodigo() { return codigo; }
    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
    public void setCodigo(Integer codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public Boolean getAdministrador() { return administrador; }
    public void setAdministrador(Boolean administrador) { this.administrador = administrador; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
