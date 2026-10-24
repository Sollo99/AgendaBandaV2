IF DB_ID('AgendaBanda') IS NULL
BEGIN
    CREATE DATABASE AgendaBanda;
END
GO

USE AgendaBanda;
GO

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
    cpf NVARCHAR(14) NOT NULL UNIQUE,
    email NVARCHAR(120) NOT NULL UNIQUE,
    login NVARCHAR(50) NOT NULL UNIQUE,
    senha NVARCHAR(255) NOT NULL,
    administrador BIT NOT NULL DEFAULT 0,
    ativo BIT NOT NULL DEFAULT 1,
    perfil NVARCHAR(10) NOT NULL DEFAULT N'MEMBRO',
    status_cadastro NVARCHAR(10) NOT NULL DEFAULT N'APROVADO',
    cadastro_decidido_em DATETIME2 NULL,
    cadastro_decidido_por INT NULL,
    criado_em DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT ck_membros_perfil CHECK (perfil IN (N'MEMBRO', N'MUSICO', N'PRODUTOR')),
    CONSTRAINT ck_membros_cadastro CHECK (status_cadastro IN (N'PENDENTE', N'APROVADO', N'RECUSADO')),
    CONSTRAINT ck_membros_cadastro_acesso CHECK (status_cadastro = N'APROVADO' OR (ativo = 0 AND administrador = 0)),
    CONSTRAINT fk_membros_cadastro_administrador FOREIGN KEY (cadastro_decidido_por) REFERENCES membros(codigo),
    CONSTRAINT uq_membros_codigo_perfil UNIQUE (codigo, perfil),
    CONSTRAINT ck_membros_cpf CHECK (LEN(cpf) = 11 AND cpf NOT LIKE '%[^0-9]%')
);
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
    ativo BIT NOT NULL DEFAULT 1
);
GO

CREATE TABLE cidades (
    codigo_cidade INT IDENTITY(1,1) PRIMARY KEY,
    nome NVARCHAR(100) NOT NULL,
    uf CHAR(2) NOT NULL
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
    CONSTRAINT ck_participacao_status CHECK (status_confirmacao IN (N'PENDENTE', N'CONFIRMADO', N'RECUSADO'))
);
GO

CREATE INDEX ix_agenda_periodo ON agenda(dt_hora_in, dt_hora_fim) INCLUDE (codigo_local, status);
CREATE INDEX ix_participacao_musico ON participacao_evento(cod_musico, codigo_agenda);
GO
