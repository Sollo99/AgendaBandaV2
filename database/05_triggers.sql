USE AgendaBanda;
GO

-- Auditoria automatica das alteracoes realizadas no banco.

CREATE OR ALTER TRIGGER dbo.trg_agenda_historico ON dbo.agenda
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @responsavel INT = TRY_CONVERT(INT, SESSION_CONTEXT(N'membro_responsavel'));
    DECLARE @agora DATETIME2 = CONVERT(DATETIME2, SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time');
    INSERT INTO historico_eventos (codigo_agenda, membro_responsavel, tipo_operacao, data_hora, dados_anteriores, dados_novos)
    SELECT COALESCE(i.codigo_agenda, d.codigo_agenda), @responsavel,
        CASE WHEN d.codigo_agenda IS NULL THEN N'CRIACAO'
             WHEN i.codigo_agenda IS NULL THEN N'EXCLUSAO'
             WHEN i.status = N'CANCELADO' AND d.status <> N'CANCELADO' THEN N'CANCELAMENTO'
             WHEN i.dt_hora_in <> d.dt_hora_in OR i.dt_hora_fim <> d.dt_hora_fim THEN N'REAGENDAMENTO'
             ELSE N'ALTERACAO' END, @agora,
        CASE WHEN d.codigo_agenda IS NOT NULL THEN (
            SELECT d.titulo, d.descricao, d.dt_hora_in, d.dt_hora_fim, d.codigo_local,
                d.codigo_tipo, d.status, d.cod_membro_criador, d.criado_em, d.atualizado_em
            FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.codigo_agenda IS NOT NULL THEN (
            SELECT i.titulo, i.descricao, i.dt_hora_in, i.dt_hora_fim, i.codigo_local,
                i.codigo_tipo, i.status, i.cod_membro_criador, i.criado_em, i.atualizado_em
            FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON d.codigo_agenda = i.codigo_agenda;
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_participacao_historico ON dbo.participacao_evento
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @responsavel INT = TRY_CONVERT(INT, SESSION_CONTEXT(N'membro_responsavel'));
    DECLARE @agora DATETIME2 = CONVERT(DATETIME2, SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time');
    INSERT INTO historico_operacoes (membro_responsavel, entidade_afetada, codigo_registro_afetado, tipo_operacao, data_hora, dados_anteriores, dados_novos)
    SELECT @responsavel, N'PARTICIPACAO_EVENTO',
        CONCAT(COALESCE(i.codigo_agenda, d.codigo_agenda), N':', COALESCE(i.cod_musico, d.cod_musico)),
        CASE WHEN d.codigo_agenda IS NULL THEN N'INSERT' WHEN i.codigo_agenda IS NULL THEN N'DELETE' ELSE N'UPDATE' END,
        @agora, antigo.json_dados, novo.json_dados
    FROM inserted i FULL OUTER JOIN deleted d ON d.codigo_agenda = i.codigo_agenda AND d.cod_musico = i.cod_musico
    OUTER APPLY (SELECT CASE WHEN d.codigo_agenda IS NOT NULL THEN (
        SELECT d.codigo_agenda, d.cod_musico, d.status_confirmacao, d.presente, d.data_resposta
        FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END AS json_dados) antigo
    OUTER APPLY (SELECT CASE WHEN i.codigo_agenda IS NOT NULL THEN (
        SELECT i.codigo_agenda, i.cod_musico, i.status_confirmacao, i.presente, i.data_resposta
        FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END AS json_dados) novo
    -- A alteracao de observacao gera operacao, mas seu texto nao e copiado.
    WHERE d.codigo_agenda IS NULL OR i.codigo_agenda IS NULL
       OR antigo.json_dados COLLATE Latin1_General_100_BIN2 <> novo.json_dados COLLATE Latin1_General_100_BIN2
       OR ISNULL(d.observacao,N'') COLLATE Latin1_General_100_BIN2 <> ISNULL(i.observacao,N'') COLLATE Latin1_General_100_BIN2;
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_membros_historico ON dbo.membros
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @responsavel INT = TRY_CONVERT(INT, SESSION_CONTEXT(N'membro_responsavel'));
    DECLARE @agora DATETIME2 = CONVERT(DATETIME2, SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time');
    -- Lista explicita: nunca copiar senha/hash, CPF, email, login, cookies ou tokens.
    INSERT INTO historico_operacoes (membro_responsavel, entidade_afetada, codigo_registro_afetado, tipo_operacao, data_hora, dados_anteriores, dados_novos)
    SELECT @responsavel, N'MEMBRO', CONVERT(NVARCHAR(100), COALESCE(i.codigo, d.codigo)),
        CASE WHEN d.codigo IS NULL THEN N'INSERT' WHEN i.codigo IS NULL THEN N'DELETE' ELSE N'UPDATE' END,
        @agora, antigo.json_dados, novo.json_dados
    FROM inserted i FULL OUTER JOIN deleted d ON d.codigo = i.codigo
    OUTER APPLY (SELECT CASE WHEN d.codigo IS NOT NULL THEN (
        SELECT d.codigo, d.nome, d.ativo, d.administrador, d.perfil, d.status_cadastro,
            d.cadastro_decidido_em, d.cadastro_decidido_por, d.identificadores_liberados_em, d.identificadores_liberados_por
        FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END AS json_dados) antigo
    OUTER APPLY (SELECT CASE WHEN i.codigo IS NOT NULL THEN (
        SELECT i.codigo, i.nome, i.ativo, i.administrador, i.perfil, i.status_cadastro,
            i.cadastro_decidido_em, i.cadastro_decidido_por, i.identificadores_liberados_em, i.identificadores_liberados_por
        FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END AS json_dados) novo
    WHERE d.codigo IS NULL OR i.codigo IS NULL OR antigo.json_dados COLLATE Latin1_General_100_BIN2 <> novo.json_dados COLLATE Latin1_General_100_BIN2;

    -- Audita a troca da credencial sem armazenar valores antigos, novos ou hashes.
    -- Separado do retrato cadastral para registrar tambem mudancas apenas de senha.
    INSERT INTO historico_operacoes (membro_responsavel, entidade_afetada, codigo_registro_afetado, tipo_operacao, data_hora, dados_anteriores, dados_novos)
    SELECT @responsavel, N'MEMBRO', CONVERT(NVARCHAR(100), i.codigo), N'UPDATE',
        @agora, NULL, N'{"senha_alterada":true}'
    FROM inserted i INNER JOIN deleted d ON d.codigo = i.codigo
    WHERE DATALENGTH(d.senha)<>DATALENGTH(i.senha) OR d.senha COLLATE Latin1_General_100_BIN2 <> i.senha COLLATE Latin1_General_100_BIN2;

    -- Marca quais identificadores mudaram sem copiar valores antigos, novos ou hashes.
    INSERT INTO historico_operacoes (membro_responsavel, entidade_afetada, codigo_registro_afetado, tipo_operacao, data_hora, dados_anteriores, dados_novos)
    SELECT @responsavel,N'MEMBRO',CONVERT(NVARCHAR(100),i.codigo),N'UPDATE',@agora,NULL,
        (SELECT flags.cpf_alterado,flags.email_alterado,flags.login_alterado FOR JSON PATH,WITHOUT_ARRAY_WRAPPER)
    FROM inserted i INNER JOIN deleted d ON d.codigo=i.codigo
    CROSS APPLY (SELECT
        CAST(CASE WHEN (d.cpf IS NULL AND i.cpf IS NOT NULL) OR (d.cpf IS NOT NULL AND i.cpf IS NULL)
            OR DATALENGTH(d.cpf)<>DATALENGTH(i.cpf) OR d.cpf COLLATE Latin1_General_100_BIN2<>i.cpf COLLATE Latin1_General_100_BIN2 THEN 1 ELSE 0 END AS BIT) AS cpf_alterado,
        CAST(CASE WHEN (d.email IS NULL AND i.email IS NOT NULL) OR (d.email IS NOT NULL AND i.email IS NULL)
            OR DATALENGTH(d.email)<>DATALENGTH(i.email) OR d.email COLLATE Latin1_General_100_BIN2<>i.email COLLATE Latin1_General_100_BIN2 THEN 1 ELSE 0 END AS BIT) AS email_alterado,
        CAST(CASE WHEN (d.login IS NULL AND i.login IS NOT NULL) OR (d.login IS NOT NULL AND i.login IS NULL)
            OR DATALENGTH(d.login)<>DATALENGTH(i.login) OR d.login COLLATE Latin1_General_100_BIN2<>i.login COLLATE Latin1_General_100_BIN2 THEN 1 ELSE 0 END AS BIT) AS login_alterado) flags
    WHERE flags.cpf_alterado=1 OR flags.email_alterado=1 OR flags.login_alterado=1;
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_cidades_historico ON dbo.cidades
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @responsavel INT=TRY_CONVERT(INT,SESSION_CONTEXT(N'membro_responsavel'));
    DECLARE @agora DATETIME2=CONVERT(DATETIME2,SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time');
    -- Lista explicita de campos; textos privados e segredos sao representados apenas por marcadores.
    INSERT INTO historico_operacoes (membro_responsavel,entidade_afetada,codigo_registro_afetado,tipo_operacao,data_hora,dados_anteriores,dados_novos)
    SELECT @responsavel,N'CIDADE',CONVERT(NVARCHAR(100),COALESCE(i.codigo_cidade,d.codigo_cidade)),
        CASE WHEN d.codigo_cidade IS NULL THEN N'INSERT' WHEN i.codigo_cidade IS NULL THEN N'DELETE' ELSE N'UPDATE' END,
        @agora,antigo.json_dados,novo.json_dados
    FROM inserted i FULL OUTER JOIN deleted d ON d.codigo_cidade=i.codigo_cidade
    OUTER APPLY (SELECT CASE WHEN d.codigo_cidade IS NOT NULL THEN (
        SELECT d.codigo_cidade,d.nome,d.uf
        FOR JSON PATH,INCLUDE_NULL_VALUES,WITHOUT_ARRAY_WRAPPER) END AS json_dados) antigo
    OUTER APPLY (SELECT CASE WHEN i.codigo_cidade IS NOT NULL THEN (
        SELECT i.codigo_cidade,i.nome,i.uf
        FOR JSON PATH,INCLUDE_NULL_VALUES,WITHOUT_ARRAY_WRAPPER) END AS json_dados) novo
    WHERE d.codigo_cidade IS NULL OR i.codigo_cidade IS NULL
        OR antigo.json_dados COLLATE Latin1_General_100_BIN2<>novo.json_dados COLLATE Latin1_General_100_BIN2;
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_locais_historico ON dbo.locais
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @responsavel INT=TRY_CONVERT(INT,SESSION_CONTEXT(N'membro_responsavel'));
    DECLARE @agora DATETIME2=CONVERT(DATETIME2,SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time');
    -- Lista explicita de campos; textos privados e segredos sao representados apenas por marcadores.
    INSERT INTO historico_operacoes (membro_responsavel,entidade_afetada,codigo_registro_afetado,tipo_operacao,data_hora,dados_anteriores,dados_novos)
    SELECT @responsavel,N'LOCAL',CONVERT(NVARCHAR(100),COALESCE(i.codigo_local,d.codigo_local)),
        CASE WHEN d.codigo_local IS NULL THEN N'INSERT' WHEN i.codigo_local IS NULL THEN N'DELETE' ELSE N'UPDATE' END,
        @agora,antigo.json_dados,novo.json_dados
    FROM inserted i FULL OUTER JOIN deleted d ON d.codigo_local=i.codigo_local
    OUTER APPLY (SELECT CASE WHEN d.codigo_local IS NOT NULL THEN (
        SELECT d.codigo_local,d.nome,d.endereco,d.complemento,d.codigo_cidade
        FOR JSON PATH,INCLUDE_NULL_VALUES,WITHOUT_ARRAY_WRAPPER) END AS json_dados) antigo
    OUTER APPLY (SELECT CASE WHEN i.codigo_local IS NOT NULL THEN (
        SELECT i.codigo_local,i.nome,i.endereco,i.complemento,i.codigo_cidade
        FOR JSON PATH,INCLUDE_NULL_VALUES,WITHOUT_ARRAY_WRAPPER) END AS json_dados) novo
    WHERE d.codigo_local IS NULL OR i.codigo_local IS NULL
        OR antigo.json_dados COLLATE Latin1_General_100_BIN2<>novo.json_dados COLLATE Latin1_General_100_BIN2;
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_tipos_evento_historico ON dbo.tipos_evento
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @responsavel INT=TRY_CONVERT(INT,SESSION_CONTEXT(N'membro_responsavel'));
    DECLARE @agora DATETIME2=CONVERT(DATETIME2,SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time');
    -- Lista explicita de campos; textos privados e segredos sao representados apenas por marcadores.
    INSERT INTO historico_operacoes (membro_responsavel,entidade_afetada,codigo_registro_afetado,tipo_operacao,data_hora,dados_anteriores,dados_novos)
    SELECT @responsavel,N'TIPO_EVENTO',CONVERT(NVARCHAR(100),COALESCE(i.codigo_tipo,d.codigo_tipo)),
        CASE WHEN d.codigo_tipo IS NULL THEN N'INSERT' WHEN i.codigo_tipo IS NULL THEN N'DELETE' ELSE N'UPDATE' END,
        @agora,antigo.json_dados,novo.json_dados
    FROM inserted i FULL OUTER JOIN deleted d ON d.codigo_tipo=i.codigo_tipo
    OUTER APPLY (SELECT CASE WHEN d.codigo_tipo IS NOT NULL THEN (
        SELECT d.codigo_tipo,d.nome,d.descricao,d.categoria_detalhe,d.ativo
        FOR JSON PATH,INCLUDE_NULL_VALUES,WITHOUT_ARRAY_WRAPPER) END AS json_dados) antigo
    OUTER APPLY (SELECT CASE WHEN i.codigo_tipo IS NOT NULL THEN (
        SELECT i.codigo_tipo,i.nome,i.descricao,i.categoria_detalhe,i.ativo
        FOR JSON PATH,INCLUDE_NULL_VALUES,WITHOUT_ARRAY_WRAPPER) END AS json_dados) novo
    WHERE d.codigo_tipo IS NULL OR i.codigo_tipo IS NULL
        OR antigo.json_dados COLLATE Latin1_General_100_BIN2<>novo.json_dados COLLATE Latin1_General_100_BIN2;
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_musicos_historico ON dbo.musicos
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @responsavel INT=TRY_CONVERT(INT,SESSION_CONTEXT(N'membro_responsavel'));
    DECLARE @agora DATETIME2=CONVERT(DATETIME2,SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time');
    -- Lista explicita de campos; textos privados e segredos sao representados apenas por marcadores.
    INSERT INTO historico_operacoes (membro_responsavel,entidade_afetada,codigo_registro_afetado,tipo_operacao,data_hora,dados_anteriores,dados_novos)
    SELECT @responsavel,N'MUSICO',CONVERT(NVARCHAR(100),COALESCE(i.codigo,d.codigo)),
        CASE WHEN d.codigo IS NULL THEN N'INSERT' WHEN i.codigo IS NULL THEN N'DELETE' ELSE N'UPDATE' END,
        @agora,antigo.json_dados,CASE WHEN d.codigo IS NOT NULL AND i.codigo IS NOT NULL AND ((d.biografia IS NULL AND i.biografia IS NOT NULL) OR (d.biografia IS NOT NULL AND i.biografia IS NULL) OR DATALENGTH(d.biografia)<>DATALENGTH(i.biografia) OR d.biografia COLLATE Latin1_General_100_BIN2<>i.biografia COLLATE Latin1_General_100_BIN2) THEN JSON_MODIFY(novo.json_dados,'$.biografia_alterada',CAST(1 AS BIT)) ELSE novo.json_dados END
    FROM inserted i FULL OUTER JOIN deleted d ON d.codigo=i.codigo
    OUTER APPLY (SELECT CASE WHEN d.codigo IS NOT NULL THEN (
        SELECT d.codigo,d.perfil,d.instrumento
        FOR JSON PATH,INCLUDE_NULL_VALUES,WITHOUT_ARRAY_WRAPPER) END AS json_dados) antigo
    OUTER APPLY (SELECT CASE WHEN i.codigo IS NOT NULL THEN (
        SELECT i.codigo,i.perfil,i.instrumento
        FOR JSON PATH,INCLUDE_NULL_VALUES,WITHOUT_ARRAY_WRAPPER) END AS json_dados) novo
    WHERE d.codigo IS NULL OR i.codigo IS NULL
        OR antigo.json_dados COLLATE Latin1_General_100_BIN2<>novo.json_dados COLLATE Latin1_General_100_BIN2
        OR (d.codigo IS NOT NULL AND i.codigo IS NOT NULL AND ((d.biografia IS NULL AND i.biografia IS NOT NULL) OR (d.biografia IS NOT NULL AND i.biografia IS NULL) OR DATALENGTH(d.biografia)<>DATALENGTH(i.biografia) OR d.biografia COLLATE Latin1_General_100_BIN2<>i.biografia COLLATE Latin1_General_100_BIN2));
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_produtores_historico ON dbo.produtores
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @responsavel INT=TRY_CONVERT(INT,SESSION_CONTEXT(N'membro_responsavel'));
    DECLARE @agora DATETIME2=CONVERT(DATETIME2,SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time');
    -- Lista explicita de campos; textos privados e segredos sao representados apenas por marcadores.
    INSERT INTO historico_operacoes (membro_responsavel,entidade_afetada,codigo_registro_afetado,tipo_operacao,data_hora,dados_anteriores,dados_novos)
    SELECT @responsavel,N'PRODUTOR',CONVERT(NVARCHAR(100),COALESCE(i.codigo,d.codigo)),
        CASE WHEN d.codigo IS NULL THEN N'INSERT' WHEN i.codigo IS NULL THEN N'DELETE' ELSE N'UPDATE' END,
        @agora,antigo.json_dados,novo.json_dados
    FROM inserted i FULL OUTER JOIN deleted d ON d.codigo=i.codigo
    OUTER APPLY (SELECT CASE WHEN d.codigo IS NOT NULL THEN (
        SELECT d.codigo,d.perfil,d.especialidade
        FOR JSON PATH,INCLUDE_NULL_VALUES,WITHOUT_ARRAY_WRAPPER) END AS json_dados) antigo
    OUTER APPLY (SELECT CASE WHEN i.codigo IS NOT NULL THEN (
        SELECT i.codigo,i.perfil,i.especialidade
        FOR JSON PATH,INCLUDE_NULL_VALUES,WITHOUT_ARRAY_WRAPPER) END AS json_dados) novo
    WHERE d.codigo IS NULL OR i.codigo IS NULL
        OR antigo.json_dados COLLATE Latin1_General_100_BIN2<>novo.json_dados COLLATE Latin1_General_100_BIN2;
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_recuperacoes_historico ON dbo.redefinicoes_senha
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @responsavel INT=TRY_CONVERT(INT,SESSION_CONTEXT(N'membro_responsavel'));
    DECLARE @agora DATETIME2=CONVERT(DATETIME2,SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time');
    INSERT INTO historico_operacoes (membro_responsavel,entidade_afetada,codigo_registro_afetado,tipo_operacao,data_hora,dados_anteriores,dados_novos)
    SELECT @responsavel,N'RECUPERACAO_SENHA',CONVERT(NVARCHAR(100),COALESCE(i.codigo_redefinicao,d.codigo_redefinicao)),
        CASE WHEN d.codigo_redefinicao IS NULL THEN N'INSERT' WHEN i.codigo_redefinicao IS NULL THEN N'DELETE' ELSE N'UPDATE' END,
        @agora,antigo.json_dados,auditado.json_dados
    FROM inserted i FULL OUTER JOIN deleted d ON d.codigo_redefinicao=i.codigo_redefinicao
    OUTER APPLY (SELECT CASE WHEN d.codigo_redefinicao IS NOT NULL THEN (
        SELECT d.codigo_redefinicao,d.codigo_membro,d.status,d.solicitado_em,d.expira_em,
            d.codigo_administrador,d.decidido_em,d.concluido_em,d.encerrado_em
        FOR JSON PATH,INCLUDE_NULL_VALUES,WITHOUT_ARRAY_WRAPPER) END AS json_dados) antigo
    OUTER APPLY (SELECT CASE WHEN i.codigo_redefinicao IS NOT NULL THEN (
        SELECT i.codigo_redefinicao,i.codigo_membro,i.status,i.solicitado_em,i.expira_em,
            i.codigo_administrador,i.decidido_em,i.concluido_em,i.encerrado_em
        FOR JSON PATH,INCLUDE_NULL_VALUES,WITHOUT_ARRAY_WRAPPER) END AS json_dados) novo
    CROSS APPLY (SELECT
        CAST(CASE WHEN d.codigo_redefinicao IS NOT NULL AND i.codigo_redefinicao IS NOT NULL
            AND (DATALENGTH(d.chave_hash)<>DATALENGTH(i.chave_hash)
                OR d.chave_hash COLLATE Latin1_General_100_BIN2<>i.chave_hash COLLATE Latin1_General_100_BIN2)
            THEN 1 ELSE 0 END AS BIT) AS credencial_alterada,
        CAST(CASE WHEN d.codigo_redefinicao IS NOT NULL AND i.codigo_redefinicao IS NOT NULL
            AND ((d.motivo_encerramento IS NULL AND i.motivo_encerramento IS NOT NULL)
                OR (d.motivo_encerramento IS NOT NULL AND i.motivo_encerramento IS NULL)
                OR DATALENGTH(d.motivo_encerramento)<>DATALENGTH(i.motivo_encerramento)
                OR d.motivo_encerramento COLLATE Latin1_General_100_BIN2<>i.motivo_encerramento COLLATE Latin1_General_100_BIN2)
            THEN 1 ELSE 0 END AS BIT) AS motivo_alterado) flags
    -- Somente os marcadores entram no JSON; nao copiar hash ou texto livre.
    CROSS APPLY (SELECT CASE WHEN flags.credencial_alterada=1
        THEN JSON_MODIFY(novo.json_dados,'$.credencial_alterada',CAST(1 AS BIT)) ELSE novo.json_dados END AS json_dados) credencial
    CROSS APPLY (SELECT CASE WHEN flags.motivo_alterado=1
        THEN JSON_MODIFY(credencial.json_dados,'$.motivo_alterado',CAST(1 AS BIT)) ELSE credencial.json_dados END AS json_dados) auditado
    WHERE d.codigo_redefinicao IS NULL OR i.codigo_redefinicao IS NULL
        OR antigo.json_dados COLLATE Latin1_General_100_BIN2<>novo.json_dados COLLATE Latin1_General_100_BIN2
        OR flags.credencial_alterada=1 OR flags.motivo_alterado=1;
END;
GO
