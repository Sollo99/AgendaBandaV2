IF DB_ID('AgendaBanda') IS NULL
BEGIN
    CREATE DATABASE AgendaBanda;
END
GO

USE AgendaBanda;
GO

-- Recriacao completa de banco novo: dependencias antes das tabelas.
DROP PROCEDURE IF EXISTS dbo.usp_criar_evento;
DROP PROCEDURE IF EXISTS dbo.usp_atualizar_evento;
DROP PROCEDURE IF EXISTS dbo.usp_atualizar_participacao;
DROP PROCEDURE IF EXISTS dbo.usp_excluir_evento;
DROP PROCEDURE IF EXISTS dbo.usp_alterar_acesso_membro;
DROP PROCEDURE IF EXISTS dbo.usp_decidir_cadastro;
DROP PROCEDURE IF EXISTS dbo.usp_liberar_identificadores;
DROP PROCEDURE IF EXISTS dbo.usp_limpar_historicos;
DROP PROCEDURE IF EXISTS dbo.usp_limpar_sessoes;
DROP PROCEDURE IF EXISTS dbo.usp_expirar_recuperacoes;
DROP PROCEDURE IF EXISTS dbo.usp_cancelar_recuperacoes;
DROP PROCEDURE IF EXISTS dbo.usp_excluir_local;
DROP PROCEDURE IF EXISTS dbo.usp_registrar_autenticacao;
DROP VIEW IF EXISTS dbo.vw_agenda_detalhada;
DROP VIEW IF EXISTS dbo.vw_presenca_musicos;
DROP VIEW IF EXISTS dbo.vw_ocupacao_locais;
DROP VIEW IF EXISTS dbo.vw_historico_unificado;
DROP VIEW IF EXISTS dbo.vw_sessoes_membros;
DROP FUNCTION IF EXISTS dbo.fn_taxa_presenca_musico;
DROP FUNCTION IF EXISTS dbo.fn_conflitos_agendamento;
DROP FUNCTION IF EXISTS dbo.fn_resumo_periodo;
DROP TABLE IF EXISTS historico_eventos;
DROP TABLE IF EXISTS historico_operacoes;
DROP TABLE IF EXISTS sessoes;
DROP TABLE IF EXISTS redefinicoes_senha;
DROP TABLE IF EXISTS participacao_evento;
DROP TABLE IF EXISTS shows;
DROP TABLE IF EXISTS reunioes;
DROP TABLE IF EXISTS ensaios;
DROP TABLE IF EXISTS agenda;
DROP TABLE IF EXISTS produtores;
DROP TABLE IF EXISTS musicos;
DROP TABLE IF EXISTS locais;
DROP TABLE IF EXISTS cidades;
DROP TABLE IF EXISTS tipos_evento;
DROP TABLE IF EXISTS membros;
GO

CREATE TABLE membros (
    codigo INT IDENTITY(1,1) PRIMARY KEY,
    nome NVARCHAR(100) NOT NULL,
    cpf NVARCHAR(14) NULL,
    email NVARCHAR(120) NULL,
    login NVARCHAR(50) NULL,
    senha NVARCHAR(255) NOT NULL,
    administrador BIT NOT NULL DEFAULT 0,
    ativo BIT NOT NULL DEFAULT 1,
    perfil NVARCHAR(10) NOT NULL DEFAULT N'MEMBRO',
    status_cadastro NVARCHAR(10) NOT NULL DEFAULT N'APROVADO',
    cadastro_decidido_em DATETIME2 NULL,
    cadastro_decidido_por INT NULL,
    identificadores_liberados_em DATETIME2 NULL,
    identificadores_liberados_por INT NULL,
    criado_em DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT ck_membros_perfil CHECK (perfil IN (N'MEMBRO', N'MUSICO', N'PRODUTOR')),
    CONSTRAINT ck_membros_cadastro CHECK (status_cadastro IN (N'PENDENTE', N'APROVADO', N'RECUSADO')),
    CONSTRAINT ck_membros_cadastro_acesso CHECK (status_cadastro = N'APROVADO' OR (ativo = 0 AND administrador = 0)),
    CONSTRAINT fk_membros_cadastro_administrador FOREIGN KEY (cadastro_decidido_por) REFERENCES membros(codigo),
    CONSTRAINT fk_membros_liberacao_administrador FOREIGN KEY (identificadores_liberados_por) REFERENCES membros(codigo),
    CONSTRAINT ck_membros_identificadores CHECK (
        (cpf IS NOT NULL AND email IS NOT NULL AND login IS NOT NULL
            AND identificadores_liberados_em IS NULL AND identificadores_liberados_por IS NULL)
        OR (status_cadastro = N'RECUSADO' AND cpf IS NULL AND email IS NULL AND login IS NULL
            AND identificadores_liberados_em IS NOT NULL AND identificadores_liberados_por IS NOT NULL)),
    CONSTRAINT uq_membros_codigo_perfil UNIQUE (codigo, perfil),
    CONSTRAINT ck_membros_cpf CHECK (LEN(cpf) = 11 AND cpf NOT LIKE '%[^0-9]%')
);
GO

CREATE UNIQUE INDEX ux_membros_cpf ON membros(cpf) WHERE cpf IS NOT NULL;
CREATE UNIQUE INDEX ux_membros_email ON membros(email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX ux_membros_login ON membros(login) WHERE login IS NOT NULL;
GO

CREATE TABLE redefinicoes_senha (
    codigo_redefinicao INT IDENTITY(1,1) PRIMARY KEY,
    codigo_membro INT NOT NULL,
    status NVARCHAR(15) NOT NULL DEFAULT 'PENDENTE',
    chave_hash CHAR(64) NOT NULL,
    solicitado_em DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    expira_em DATETIME2 NOT NULL,
    codigo_administrador INT NULL,
    decidido_em DATETIME2 NULL,
    concluido_em DATETIME2 NULL,
    encerrado_em DATETIME2 NULL,
    motivo_encerramento NVARCHAR(200) NULL,
    CONSTRAINT ck_redefinicoes_senha_status CHECK (status IN ('PENDENTE', 'APROVADA', 'RECUSADA', 'CONCLUIDA', 'EXPIRADA', 'CANCELADA')),
    CONSTRAINT ck_redefinicoes_senha_prazo CHECK (expira_em > solicitado_em),
    CONSTRAINT ck_redefinicoes_senha_encerramento CHECK ((status IN ('PENDENTE', 'APROVADA') AND encerrado_em IS NULL) OR (status NOT IN ('PENDENTE', 'APROVADA') AND encerrado_em IS NOT NULL)),
    CONSTRAINT fk_redefinicoes_senha_membro FOREIGN KEY (codigo_membro) REFERENCES membros(codigo) ON DELETE CASCADE,
    CONSTRAINT fk_redefinicoes_senha_administrador FOREIGN KEY (codigo_administrador) REFERENCES membros(codigo)
);
GO

CREATE TABLE sessoes (
    codigo_sessao INT IDENTITY(1,1) PRIMARY KEY,
    token_hash CHAR(64) NOT NULL UNIQUE,
    codigo_membro INT NOT NULL,
    criado_em DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    expira_em DATETIME2 NOT NULL,
    CONSTRAINT fk_sessoes_membro FOREIGN KEY (codigo_membro) REFERENCES membros(codigo) ON DELETE CASCADE
);
GO

CREATE INDEX ix_sessoes_token_expiracao ON sessoes(token_hash, expira_em);
CREATE INDEX ix_sessoes_expiracao ON sessoes(expira_em);
GO

CREATE INDEX ix_redefinicoes_senha_status ON redefinicoes_senha(status, solicitado_em);
CREATE INDEX ix_redefinicoes_senha_expiracao ON redefinicoes_senha(expira_em) INCLUDE (codigo_membro, status);
GO

CREATE UNIQUE INDEX ux_redefinicoes_senha_aberta ON redefinicoes_senha(codigo_membro)
    WHERE status IN ('PENDENTE', 'APROVADA');
GO

CREATE TABLE musicos (
    codigo INT PRIMARY KEY,
    perfil NVARCHAR(10) NOT NULL DEFAULT N'MUSICO',
    instrumento NVARCHAR(80) NOT NULL,
    biografia NVARCHAR(500) NULL,
    CONSTRAINT ck_musicos_perfil CHECK (perfil = N'MUSICO'),
    CONSTRAINT fk_musicos_membros FOREIGN KEY (codigo, perfil) REFERENCES membros(codigo, perfil)
);
GO

CREATE TABLE produtores (
    codigo INT PRIMARY KEY,
    perfil NVARCHAR(10) NOT NULL DEFAULT N'PRODUTOR',
    especialidade NVARCHAR(100) NOT NULL,
    CONSTRAINT ck_produtores_perfil CHECK (perfil = N'PRODUTOR'),
    CONSTRAINT fk_produtores_membros FOREIGN KEY (codigo, perfil) REFERENCES membros(codigo, perfil)
);
GO

CREATE TABLE tipos_evento (
    codigo_tipo INT IDENTITY(1,1) PRIMARY KEY,
    nome NVARCHAR(50) NOT NULL UNIQUE,
    descricao NVARCHAR(300) NULL,
    categoria_detalhe NVARCHAR(15) NOT NULL DEFAULT 'EVENTO',
    ativo BIT NOT NULL DEFAULT 1,
    CONSTRAINT ck_tipos_evento_categoria CHECK (categoria_detalhe IN (N'EVENTO', N'ENSAIO', N'SHOW', N'REUNIAO'))
);
GO

CREATE TABLE cidades (
    codigo_cidade INT IDENTITY(1,1) PRIMARY KEY,
    nome NVARCHAR(100) COLLATE Latin1_General_100_CI_AS NOT NULL,
    uf CHAR(2) COLLATE Latin1_General_100_CI_AS NOT NULL,
    CONSTRAINT uq_cidades_nome_uf UNIQUE (nome, uf)
);
GO

CREATE TABLE locais (
    codigo_local INT IDENTITY(1,1) PRIMARY KEY,
    nome NVARCHAR(120) NOT NULL,
    endereco NVARCHAR(180) NOT NULL,
    complemento NVARCHAR(120) NULL,
    codigo_cidade INT NOT NULL,
    CONSTRAINT fk_locais_cidades FOREIGN KEY (codigo_cidade) REFERENCES cidades(codigo_cidade)
);
GO

CREATE TABLE agenda (
    codigo_agenda INT IDENTITY(1,1) PRIMARY KEY,
    titulo NVARCHAR(120) NOT NULL,
    descricao NVARCHAR(800) NULL,
    dt_hora_in DATETIME2 NOT NULL,
    dt_hora_fim DATETIME2 NOT NULL,
    codigo_local INT NOT NULL,
    cod_membro_criador INT NOT NULL,
    codigo_tipo INT NOT NULL DEFAULT 1,
    status NVARCHAR(20) NOT NULL DEFAULT N'AGENDADO',
    criado_em DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    atualizado_em DATETIME2 NULL,
    CONSTRAINT fk_agenda_local FOREIGN KEY (codigo_local) REFERENCES locais(codigo_local),
    CONSTRAINT fk_agenda_membro_criador FOREIGN KEY (cod_membro_criador) REFERENCES membros(codigo),
    CONSTRAINT fk_agenda_tipo FOREIGN KEY (codigo_tipo) REFERENCES tipos_evento(codigo_tipo),
    CONSTRAINT ck_agenda_periodo CHECK (dt_hora_fim > dt_hora_in),
    CONSTRAINT ck_agenda_status CHECK (status IN (N'AGENDADO', N'CANCELADO', N'REALIZADO'))
);
GO

CREATE TABLE ensaios (
    codigo_agenda INT PRIMARY KEY,
    repertorio NVARCHAR(800) NOT NULL,
    observacao NVARCHAR(500) NULL,
    CONSTRAINT fk_ensaios_agenda FOREIGN KEY (codigo_agenda) REFERENCES agenda(codigo_agenda) ON DELETE CASCADE
);
GO

CREATE TABLE reunioes (
    codigo_agenda INT PRIMARY KEY,
    pauta NVARCHAR(800) NOT NULL,
    CONSTRAINT fk_reunioes_agenda FOREIGN KEY (codigo_agenda) REFERENCES agenda(codigo_agenda) ON DELETE CASCADE
);
GO

CREATE TABLE shows (
    codigo_agenda INT PRIMARY KEY,
    codigo_cidade INT NOT NULL,
    CONSTRAINT fk_shows_agenda FOREIGN KEY (codigo_agenda) REFERENCES agenda(codigo_agenda) ON DELETE CASCADE,
    CONSTRAINT fk_shows_cidade FOREIGN KEY (codigo_cidade) REFERENCES cidades(codigo_cidade)
);
GO

CREATE TABLE participacao_evento (
    codigo_agenda INT NOT NULL,
    cod_musico INT NOT NULL,
    status_confirmacao NVARCHAR(20) NOT NULL DEFAULT N'PENDENTE',
    presente BIT NULL,
    data_resposta DATETIME2 NULL,
    observacao NVARCHAR(500) NULL,
    PRIMARY KEY (codigo_agenda, cod_musico),
    CONSTRAINT fk_participacao_evento_agenda FOREIGN KEY (codigo_agenda) REFERENCES agenda(codigo_agenda) ON DELETE CASCADE,
    CONSTRAINT fk_participacao_evento_musico FOREIGN KEY (cod_musico) REFERENCES musicos(codigo),
    CONSTRAINT ck_participacao_status CHECK (status_confirmacao IN (N'PENDENTE', N'CONFIRMADO', N'RECUSADO')),
    CONSTRAINT ck_participacao_presenca_confirmada CHECK (presente IS NULL OR presente = 0 OR status_confirmacao = N'CONFIRMADO')
);
GO

CREATE INDEX ix_agenda_periodo ON agenda(dt_hora_in, codigo_agenda) INCLUDE (dt_hora_fim, codigo_local, status);
CREATE INDEX ix_agenda_local_periodo ON agenda(codigo_local, dt_hora_in) INCLUDE (dt_hora_fim, status);
CREATE INDEX ix_membros_nome_codigo ON membros(nome, codigo);
CREATE INDEX ix_participacao_musico ON participacao_evento(cod_musico, codigo_agenda);
GO

CREATE TABLE historico_eventos (
    codigo_historico BIGINT IDENTITY(1,1) PRIMARY KEY,
    -- Identificador original sem FK: a exclusao do evento nao apaga a auditoria.
    codigo_agenda INT NOT NULL,
    membro_responsavel INT NULL,
    tipo_operacao NVARCHAR(20) NOT NULL,
    data_hora DATETIME2 NOT NULL,
    dados_anteriores NVARCHAR(MAX) NULL,
    dados_novos NVARCHAR(MAX) NULL,
    CONSTRAINT ck_historico_eventos_operacao CHECK (tipo_operacao IN (N'CRIACAO', N'ALTERACAO', N'REAGENDAMENTO', N'CANCELAMENTO', N'EXCLUSAO')),
    CONSTRAINT fk_historico_eventos_responsavel FOREIGN KEY (membro_responsavel) REFERENCES membros(codigo)
);
GO

CREATE TABLE historico_operacoes (
    codigo_operacao BIGINT IDENTITY(1,1) PRIMARY KEY,
    membro_responsavel INT NULL,
    entidade_afetada NVARCHAR(50) NOT NULL,
    -- Texto permite a chave composta agenda:musico.
    codigo_registro_afetado NVARCHAR(100) NOT NULL,
    tipo_operacao NVARCHAR(20) NOT NULL,
    data_hora DATETIME2 NOT NULL,
    dados_anteriores NVARCHAR(MAX) NULL,
    dados_novos NVARCHAR(MAX) NULL,
    CONSTRAINT ck_historico_operacoes_operacao CHECK (tipo_operacao IN (N'INSERT', N'UPDATE', N'DELETE',
        N'LOGIN', N'LOGOUT', N'LOGIN_FALHOU', N'ACESSO_NEGADO', N'LIMITE_EXCEDIDO')),
    CONSTRAINT fk_historico_operacoes_responsavel FOREIGN KEY (membro_responsavel) REFERENCES membros(codigo)
);
GO

CREATE INDEX ix_historico_eventos_data ON historico_eventos(data_hora DESC, codigo_historico DESC);
CREATE INDEX ix_historico_eventos_agenda ON historico_eventos(codigo_agenda, data_hora);
CREATE INDEX ix_historico_operacoes_data ON historico_operacoes(data_hora DESC, codigo_operacao DESC);
GO
