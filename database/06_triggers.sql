USE AgendaBanda;
GO

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
    WHERE d.senha COLLATE Latin1_General_100_BIN2 <> i.senha COLLATE Latin1_General_100_BIN2;
END;
GO
