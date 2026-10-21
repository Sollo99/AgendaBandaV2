USE AgendaBanda;
GO

IF OBJECT_ID('dbo.tipos_evento', 'U') IS NULL
BEGIN
    CREATE TABLE tipos_evento (
        codigo_tipo INT IDENTITY(1,1) PRIMARY KEY,
        nome NVARCHAR(50) NOT NULL UNIQUE,
        descricao NVARCHAR(300) NULL,
        ativo BIT NOT NULL DEFAULT 1
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM tipos_evento WHERE nome = N'ENSAIO')
    INSERT INTO tipos_evento (nome, descricao, ativo) VALUES (N'ENSAIO', N'Encontro para prática e preparação musical.', 1);
IF NOT EXISTS (SELECT 1 FROM tipos_evento WHERE nome = N'SHOW')
    INSERT INTO tipos_evento (nome, descricao, ativo) VALUES (N'SHOW', N'Apresentação pública da banda.', 1);
IF NOT EXISTS (SELECT 1 FROM tipos_evento WHERE nome = N'REUNIAO')
    INSERT INTO tipos_evento (nome, descricao, ativo) VALUES (N'REUNIAO', N'Encontro de organização e planejamento.', 1);
GO

IF COL_LENGTH('agenda', 'codigo_tipo') IS NULL
BEGIN
    ALTER TABLE agenda ADD codigo_tipo INT NULL;
    UPDATE agenda SET codigo_tipo = (SELECT codigo_tipo FROM tipos_evento WHERE nome = N'ENSAIO') WHERE codigo_agenda IN (SELECT codigo_agenda FROM ensaios);
    UPDATE agenda SET codigo_tipo = (SELECT codigo_tipo FROM tipos_evento WHERE nome = N'SHOW') WHERE codigo_agenda IN (SELECT codigo_agenda FROM shows);
    UPDATE agenda SET codigo_tipo = (SELECT codigo_tipo FROM tipos_evento WHERE nome = N'REUNIAO') WHERE codigo_agenda IN (SELECT codigo_agenda FROM reunioes);
    UPDATE agenda SET codigo_tipo = (SELECT codigo_tipo FROM tipos_evento WHERE nome = N'ENSAIO') WHERE codigo_tipo IS NULL;
    ALTER TABLE agenda ALTER COLUMN codigo_tipo INT NOT NULL;
    ALTER TABLE agenda ADD CONSTRAINT fk_agenda_tipo FOREIGN KEY (codigo_tipo) REFERENCES tipos_evento(codigo_tipo);
END
GO

IF COL_LENGTH('agenda', 'status') IS NULL
BEGIN
    ALTER TABLE agenda ADD status NVARCHAR(20) NOT NULL CONSTRAINT df_agenda_status DEFAULT N'AGENDADO' WITH VALUES;
    ALTER TABLE agenda ADD CONSTRAINT ck_agenda_status CHECK (status IN (N'AGENDADO', N'CANCELADO', N'REALIZADO'));
END
GO

IF OBJECT_ID('dbo.participacao_evento', 'U') IS NULL
BEGIN
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
END
GO

INSERT INTO participacao_evento (codigo_agenda, cod_musico, status_confirmacao)
SELECT em.codigo_agenda, em.cod_musico, N'PENDENTE'
FROM ensaio_musicos em
WHERE NOT EXISTS (
    SELECT 1 FROM participacao_evento pe
    WHERE pe.codigo_agenda = em.codigo_agenda AND pe.cod_musico = em.cod_musico
);
GO
