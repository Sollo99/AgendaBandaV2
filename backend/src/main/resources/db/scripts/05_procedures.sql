USE AgendaBanda;
GO

IF (SELECT compatibility_level FROM sys.databases WHERE name = DB_NAME()) < 130
    THROW 50001, N'O banco exige nivel de compatibilidade 130 ou superior para OPENJSON.', 1;
GO

CREATE OR ALTER PROCEDURE dbo.usp_criar_evento
    @titulo NVARCHAR(120), @descricao NVARCHAR(800), @inicio DATETIME2, @fim DATETIME2,
    @local INT, @tipo INT, @status NVARCHAR(20), @repertorio NVARCHAR(800),
    @observacao NVARCHAR(500), @pauta NVARCHAR(800), @cidade INT, @musicos_json NVARCHAR(MAX)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    DECLARE @propria BIT = CASE WHEN @@TRANCOUNT = 0 THEN 1 ELSE 0 END;
    IF @propria = 1 BEGIN TRANSACTION; ELSE SAVE TRANSACTION criar_evento;
    BEGIN TRY
        DECLARE @responsavel INT = TRY_CONVERT(INT, SESSION_CONTEXT(N'membro_responsavel'));
        DECLARE @agora DATETIME2 = CONVERT(DATETIME2, SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time');
        DECLARE @categoria NVARCHAR(15), @id INT;
        IF NOT EXISTS (SELECT 1 FROM membros WHERE codigo = @responsavel AND ativo = 1 AND status_cadastro = N'APROVADO')
            THROW 50003, N'Apenas membros ativos e aprovados podem criar eventos.', 1;
        SET @status = COALESCE(@status, N'AGENDADO');
        IF @inicio IS NULL OR @fim IS NULL OR @fim <= @inicio
            THROW 50001, N'A data/hora final deve ser maior que a inicial.', 1;
        IF NULLIF(LTRIM(RTRIM(@titulo)), N'') IS NULL OR @status NOT IN (N'AGENDADO', N'CANCELADO', N'REALIZADO')
            THROW 50001, N'Titulo ou status do evento invalido.', 1;
        IF @status = N'REALIZADO' AND @fim > @agora
            THROW 50001, N'Um evento so pode ser marcado como realizado apos seu termino.', 1;
        SELECT @categoria = categoria_detalhe FROM tipos_evento WHERE codigo_tipo = @tipo AND ativo = 1;
        IF @categoria IS NULL THROW 50001, N'Tipo de evento inexistente ou inativo.', 1;
        IF NOT EXISTS (SELECT 1 FROM locais WHERE codigo_local = @local)
            THROW 50001, N'Local nao encontrado.', 1;
        IF @categoria = N'SHOW' AND NOT EXISTS (SELECT 1 FROM cidades WHERE codigo_cidade = @cidade)
            THROW 50001, N'Informe uma cidade valida para o show.', 1;
        SET @musicos_json = COALESCE(@musicos_json, N'[]');
        IF ISJSON(@musicos_json) <> 1 OR LEFT(LTRIM(@musicos_json), 1) <> N'['
            THROW 50001, N'Lista de musicos invalida.', 1;
        IF EXISTS (SELECT 1 FROM OPENJSON(@musicos_json) WHERE type <> 2 OR TRY_CONVERT(INT, value) IS NULL OR TRY_CONVERT(INT, value) <= 0)
            THROW 50001, N'Lista de musicos invalida.', 1;
        DECLARE @musicos TABLE (codigo INT PRIMARY KEY);
        INSERT INTO @musicos SELECT DISTINCT CONVERT(INT, value) FROM OPENJSON(@musicos_json);
        -- Ordem binaria identica no Java; locks valem ate COMMIT/ROLLBACK.
        DECLARE @recursos TABLE (nome NVARCHAR(255) COLLATE Latin1_General_100_BIN2 PRIMARY KEY);
        INSERT INTO @recursos SELECT CONCAT(N'AgendaBanda:local:', @local)
            UNION SELECT CONCAT(N'AgendaBanda:musico:', codigo) FROM @musicos;
        DECLARE @recurso NVARCHAR(255), @resultado_lock INT;
        WHILE EXISTS (SELECT 1 FROM @recursos)
        BEGIN
            SELECT @recurso = MIN(nome) FROM @recursos;
            EXEC @resultado_lock = sys.sp_getapplock @Resource=@recurso, @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=5000;
            IF @resultado_lock < 0 THROW 50004, N'Recurso de agenda em uso. Aguarde e tente novamente.', 1;
            DELETE FROM @recursos WHERE nome=@recurso;
        END;
        IF EXISTS (SELECT 1 FROM @musicos ids LEFT JOIN musicos mu ON mu.codigo = ids.codigo
            LEFT JOIN membros m ON m.codigo = mu.codigo WHERE mu.codigo IS NULL OR m.ativo = 0 OR m.status_cadastro <> N'APROVADO')
            THROW 50001, N'Nao e possivel incluir um musico inexistente, inativo ou nao aprovado.', 1;
        IF @status <> N'CANCELADO' AND EXISTS (
            SELECT 1 FROM dbo.fn_conflitos_agendamento(@inicio, @fim, NULL) f
            WHERE f.codigo_local = @local OR EXISTS (SELECT 1 FROM @musicos ids WHERE ids.codigo = f.cod_musico))
            THROW 50002, N'Existe conflito de horario para o local ou para um dos musicos.', 1;

        DECLARE @historico_limite BIGINT = (SELECT COALESCE(MAX(codigo_historico), 0) FROM historico_eventos);
        INSERT INTO agenda (titulo, descricao, dt_hora_in, dt_hora_fim, codigo_local, codigo_tipo, status, cod_membro_criador, criado_em)
            VALUES (@titulo, @descricao, @inicio, @fim, @local, @tipo, @status, @responsavel, @agora);
        SET @id = CONVERT(INT, SCOPE_IDENTITY());
        IF @categoria = N'ENSAIO'
            INSERT INTO ensaios VALUES (@id, COALESCE(NULLIF(LTRIM(RTRIM(@repertorio)), N''), N'Repertorio nao informado'), @observacao);
        IF @categoria = N'REUNIAO'
            INSERT INTO reunioes VALUES (@id, COALESCE(NULLIF(LTRIM(RTRIM(@pauta)), N''), N'Pauta nao informada'));
        IF @categoria = N'SHOW' INSERT INTO shows VALUES (@id, @cidade);
        INSERT INTO participacao_evento (codigo_agenda, cod_musico, status_confirmacao)
            SELECT @id, codigo, N'PENDENTE' FROM @musicos;
        DECLARE @detalhes_novos NVARCHAR(MAX) = (
            SELECT e.repertorio, e.observacao, r.pauta, s.codigo_cidade
            FROM agenda a LEFT JOIN ensaios e ON e.codigo_agenda = a.codigo_agenda
            LEFT JOIN reunioes r ON r.codigo_agenda = a.codigo_agenda
            LEFT JOIN shows s ON s.codigo_agenda = a.codigo_agenda
            WHERE a.codigo_agenda = @id FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER);
        UPDATE historico_eventos SET dados_novos = JSON_MODIFY(dados_novos, '$.detalhes', JSON_QUERY(@detalhes_novos))
        WHERE codigo_agenda = @id AND codigo_historico > @historico_limite;
        IF @propria = 1 COMMIT TRANSACTION;
        SELECT @id AS codigo_agenda;
    END TRY
    BEGIN CATCH
        IF @propria = 1 AND XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        ELSE IF @propria = 0 AND XACT_STATE() = 1 ROLLBACK TRANSACTION criar_evento;
        THROW;
    END CATCH
END;
GO

CREATE OR ALTER PROCEDURE dbo.usp_atualizar_evento
    @id INT, @titulo NVARCHAR(120), @descricao NVARCHAR(800), @inicio DATETIME2, @fim DATETIME2,
    @local INT, @tipo INT, @status NVARCHAR(20), @repertorio NVARCHAR(800),
    @observacao NVARCHAR(500), @pauta NVARCHAR(800), @cidade INT, @musicos_json NVARCHAR(MAX)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    DECLARE @propria BIT = CASE WHEN @@TRANCOUNT = 0 THEN 1 ELSE 0 END;
    IF @propria = 1 BEGIN TRANSACTION; ELSE SAVE TRANSACTION atualizar_evento;
    BEGIN TRY
        DECLARE @responsavel INT = TRY_CONVERT(INT, SESSION_CONTEXT(N'membro_responsavel'));
        DECLARE @agora DATETIME2 = CONVERT(DATETIME2, SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time');
        DECLARE @categoria NVARCHAR(15), @criador INT, @anterior NVARCHAR(20);
        DECLARE @recurso_evento NVARCHAR(255) = CONCAT(N'AgendaBanda:evento:', @id), @resultado_evento INT;
        EXEC @resultado_evento = sys.sp_getapplock @Resource=@recurso_evento, @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=5000;
        IF @resultado_evento < 0 THROW 50004, N'Outro usuario esta alterando este evento. Tente novamente.', 1;
        SELECT @criador = cod_membro_criador, @anterior = status FROM agenda WHERE codigo_agenda = @id;
        IF @criador IS NULL THROW 50001, N'Evento nao encontrado.', 1;
        IF NOT EXISTS (SELECT 1 FROM membros WHERE codigo = @responsavel AND ativo = 1 AND status_cadastro = N'APROVADO'
            AND (administrador = 1 OR codigo = @criador))
            THROW 50003, N'Apenas o criador ou um administrador pode alterar este evento.', 1;
        SET @status = COALESCE(@status, @anterior);
        IF @inicio IS NULL OR @fim IS NULL OR @fim <= @inicio
            THROW 50001, N'A data/hora final deve ser maior que a inicial.', 1;
        IF NULLIF(LTRIM(RTRIM(@titulo)), N'') IS NULL OR @status NOT IN (N'AGENDADO', N'CANCELADO', N'REALIZADO')
            THROW 50001, N'Titulo ou status do evento invalido.', 1;
        IF @anterior = N'REALIZADO' AND @status <> N'REALIZADO'
            THROW 50001, N'Um evento realizado deve permanecer realizado para preservar seu historico.', 1;
        IF @status = N'REALIZADO' AND @fim > @agora
            THROW 50001, N'Um evento so pode ser marcado como realizado apos seu termino.', 1;
        IF EXISTS (SELECT 1 FROM participacao_evento WHERE codigo_agenda = @id AND presente IS NOT NULL)
            AND (@status = N'CANCELADO' OR @inicio > @agora)
            THROW 50001, N'Um evento com presenca registrada nao pode ser cancelado ou movido para o futuro.', 1;
        SELECT @categoria = categoria_detalhe FROM tipos_evento WHERE codigo_tipo = @tipo AND ativo = 1;
        IF @categoria IS NULL THROW 50001, N'Tipo de evento inexistente ou inativo.', 1;
        IF NOT EXISTS (SELECT 1 FROM locais WHERE codigo_local = @local)
            THROW 50001, N'Local nao encontrado.', 1;
        IF @categoria = N'SHOW' AND NOT EXISTS (SELECT 1 FROM cidades WHERE codigo_cidade = @cidade)
            THROW 50001, N'Informe uma cidade valida para o show.', 1;

        DECLARE @musicos TABLE (codigo INT PRIMARY KEY);
        IF @musicos_json IS NULL
            INSERT INTO @musicos SELECT cod_musico FROM participacao_evento WHERE codigo_agenda = @id;
        ELSE
        BEGIN
            IF ISJSON(@musicos_json) <> 1 OR LEFT(LTRIM(@musicos_json), 1) <> N'['
                THROW 50001, N'Lista de musicos invalida.', 1;
            IF EXISTS (SELECT 1 FROM OPENJSON(@musicos_json) WHERE type <> 2 OR TRY_CONVERT(INT, value) IS NULL OR TRY_CONVERT(INT, value) <= 0)
                THROW 50001, N'Lista de musicos invalida.', 1;
            INSERT INTO @musicos SELECT DISTINCT CONVERT(INT, value) FROM OPENJSON(@musicos_json);
        END;
        -- Ordem binaria identica no Java; locks valem ate COMMIT/ROLLBACK.
        DECLARE @recursos TABLE (nome NVARCHAR(255) COLLATE Latin1_General_100_BIN2 PRIMARY KEY);
        INSERT INTO @recursos SELECT CONCAT(N'AgendaBanda:local:', @local)
            UNION SELECT CONCAT(N'AgendaBanda:musico:', codigo) FROM @musicos
            UNION SELECT CONCAT(N'AgendaBanda:local:', codigo_local) FROM agenda WHERE codigo_agenda=@id
            UNION SELECT CONCAT(N'AgendaBanda:musico:', cod_musico) FROM participacao_evento WHERE codigo_agenda=@id;
        DECLARE @recurso NVARCHAR(255), @resultado_lock INT;
        WHILE EXISTS (SELECT 1 FROM @recursos)
        BEGIN
            SELECT @recurso = MIN(nome) FROM @recursos;
            EXEC @resultado_lock = sys.sp_getapplock @Resource=@recurso, @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=5000;
            IF @resultado_lock < 0 THROW 50004, N'Recurso de agenda em uso. Aguarde e tente novamente.', 1;
            DELETE FROM @recursos WHERE nome=@recurso;
        END;
        SELECT @criador = cod_membro_criador FROM agenda WITH (UPDLOCK,HOLDLOCK) WHERE codigo_agenda=@id;
        IF EXISTS (SELECT 1 FROM participacao_evento p WHERE p.codigo_agenda = @id
            AND NOT EXISTS (SELECT 1 FROM @musicos ids WHERE ids.codigo = p.cod_musico)
            AND (@anterior = N'REALIZADO' OR @status = N'REALIZADO' OR p.presente IS NOT NULL))
            THROW 50001, N'Nao e possivel remover participantes de evento realizado ou com presenca registrada.', 1;
        -- Musicos inativos podem permanecer no historico; apenas novas inclusoes exigem atividade.
        IF EXISTS (SELECT 1 FROM @musicos ids LEFT JOIN musicos mu ON mu.codigo = ids.codigo
            LEFT JOIN membros m ON m.codigo = mu.codigo WHERE mu.codigo IS NULL OR
            ((m.ativo = 0 OR m.status_cadastro <> N'APROVADO') AND NOT EXISTS (
                SELECT 1 FROM participacao_evento p WHERE p.codigo_agenda = @id AND p.cod_musico = ids.codigo)))
            THROW 50001, N'Nao e possivel incluir um musico inexistente, inativo ou nao aprovado.', 1;
        IF @status <> N'CANCELADO' AND EXISTS (
            SELECT 1 FROM dbo.fn_conflitos_agendamento(@inicio, @fim, @id) f
            WHERE f.codigo_local = @local OR EXISTS (SELECT 1 FROM @musicos ids WHERE ids.codigo = f.cod_musico))
            THROW 50002, N'Existe conflito de horario para o local ou para um dos musicos.', 1;

        DECLARE @historico_limite BIGINT = (SELECT COALESCE(MAX(codigo_historico), 0) FROM historico_eventos);
        DECLARE @detalhes_anteriores NVARCHAR(MAX) = (
            SELECT e.repertorio, e.observacao, r.pauta, s.codigo_cidade
            FROM agenda a LEFT JOIN ensaios e ON e.codigo_agenda = a.codigo_agenda
            LEFT JOIN reunioes r ON r.codigo_agenda = a.codigo_agenda
            LEFT JOIN shows s ON s.codigo_agenda = a.codigo_agenda
            WHERE a.codigo_agenda = @id FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER);
        UPDATE agenda SET titulo = @titulo, descricao = @descricao, dt_hora_in = @inicio, dt_hora_fim = @fim,
            codigo_local = @local, codigo_tipo = @tipo, status = @status, atualizado_em = @agora WHERE codigo_agenda = @id;
        DELETE FROM ensaios WHERE codigo_agenda = @id AND @categoria <> N'ENSAIO';
        DELETE FROM reunioes WHERE codigo_agenda = @id AND @categoria <> N'REUNIAO';
        DELETE FROM shows WHERE codigo_agenda = @id AND @categoria <> N'SHOW';
        IF @categoria = N'ENSAIO'
        BEGIN
            UPDATE ensaios SET repertorio = COALESCE(NULLIF(LTRIM(RTRIM(@repertorio)), N''), N'Repertorio nao informado'), observacao = @observacao WHERE codigo_agenda = @id;
            IF @@ROWCOUNT = 0 INSERT INTO ensaios VALUES (@id, COALESCE(NULLIF(LTRIM(RTRIM(@repertorio)), N''), N'Repertorio nao informado'), @observacao);
        END;
        IF @categoria = N'REUNIAO'
        BEGIN
            UPDATE reunioes SET pauta = COALESCE(NULLIF(LTRIM(RTRIM(@pauta)), N''), N'Pauta nao informada') WHERE codigo_agenda = @id;
            IF @@ROWCOUNT = 0 INSERT INTO reunioes VALUES (@id, COALESCE(NULLIF(LTRIM(RTRIM(@pauta)), N''), N'Pauta nao informada'));
        END;
        IF @categoria = N'SHOW'
        BEGIN
            UPDATE shows SET codigo_cidade = @cidade WHERE codigo_agenda = @id;
            IF @@ROWCOUNT = 0 INSERT INTO shows VALUES (@id, @cidade);
        END;
        DELETE p FROM participacao_evento p WHERE p.codigo_agenda = @id AND NOT EXISTS (SELECT 1 FROM @musicos ids WHERE ids.codigo = p.cod_musico);
        INSERT INTO participacao_evento (codigo_agenda, cod_musico, status_confirmacao)
            SELECT @id, ids.codigo, N'PENDENTE' FROM @musicos ids
            WHERE NOT EXISTS (SELECT 1 FROM participacao_evento p WHERE p.codigo_agenda = @id AND p.cod_musico = ids.codigo);
        DECLARE @detalhes_novos NVARCHAR(MAX) = (
            SELECT e.repertorio, e.observacao, r.pauta, s.codigo_cidade
            FROM agenda a LEFT JOIN ensaios e ON e.codigo_agenda = a.codigo_agenda
            LEFT JOIN reunioes r ON r.codigo_agenda = a.codigo_agenda
            LEFT JOIN shows s ON s.codigo_agenda = a.codigo_agenda
            WHERE a.codigo_agenda = @id FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER);
        UPDATE historico_eventos SET dados_novos = JSON_MODIFY(dados_novos, '$.detalhes', JSON_QUERY(@detalhes_novos)),
            dados_anteriores = JSON_MODIFY(dados_anteriores, '$.detalhes', JSON_QUERY(@detalhes_anteriores))
        WHERE codigo_agenda = @id AND codigo_historico > @historico_limite;
        IF @propria = 1 COMMIT TRANSACTION;
        SELECT @id AS codigo_agenda;
    END TRY
    BEGIN CATCH
        IF @propria = 1 AND XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        ELSE IF @propria = 0 AND XACT_STATE() = 1 ROLLBACK TRANSACTION atualizar_evento;
        THROW;
    END CATCH
END;
GO

CREATE OR ALTER PROCEDURE dbo.usp_atualizar_participacao
    @agenda INT, @musico INT, @status NVARCHAR(20), @presente BIT, @observacao NVARCHAR(500), @propria BIT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    DECLARE @transacao_propria BIT = CASE WHEN @@TRANCOUNT = 0 THEN 1 ELSE 0 END;
    IF @transacao_propria = 1 BEGIN TRANSACTION; ELSE SAVE TRANSACTION atualizar_participacao;
    BEGIN TRY
        DECLARE @responsavel INT = TRY_CONVERT(INT, SESSION_CONTEXT(N'membro_responsavel'));
        DECLARE @agora DATETIME2 = CONVERT(DATETIME2, SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time');
        DECLARE @inicio DATETIME2, @evento_status NVARCHAR(20), @confirmacao NVARCHAR(20), @presenca BIT;
        DECLARE @recurso_evento NVARCHAR(255) = CONCAT(N'AgendaBanda:evento:', @agenda), @resultado_evento INT;
        EXEC @resultado_evento = sys.sp_getapplock @Resource=@recurso_evento, @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=5000;
        IF @resultado_evento < 0 THROW 50004, N'Outro usuario esta alterando este evento. Tente novamente.', 1;
        SELECT @inicio = dt_hora_in, @evento_status = status FROM agenda WITH (UPDLOCK, HOLDLOCK) WHERE codigo_agenda = @agenda;
        -- O inicio pode ter passado enquanto a requisicao aguardava o lock.
        SET @agora = CONVERT(DATETIME2, SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time');
        IF @inicio IS NULL THROW 50001, N'Evento nao encontrado.', 1;
        IF @propria IS NULL THROW 50001, N'Informe o modo de atualizacao da participacao.', 1;
        IF NOT EXISTS (SELECT 1 FROM membros m WHERE m.codigo = @responsavel AND m.ativo = 1 AND m.status_cadastro = N'APROVADO'
            AND ((@propria = 1 AND m.codigo = @musico) OR (@propria = 0 AND m.administrador = 1)))
            THROW 50003, N'Voce nao tem permissao para alterar esta participacao.', 1;
        IF @status IS NOT NULL AND @status NOT IN (N'PENDENTE', N'CONFIRMADO', N'RECUSADO')
            THROW 50001, N'Status de confirmacao invalido.', 1;
        IF @propria = 1 AND (@evento_status <> N'AGENDADO' OR @inicio <= @agora OR @status IS NULL)
            THROW 50001, N'A confirmacao so pode ser alterada antes do inicio de um evento agendado.', 1;
        -- O modo proprio ignora o campo de presenca enviado pelo cliente.
        IF @propria = 1 SET @presente = NULL;
        IF @presente IS NOT NULL AND (@evento_status = N'CANCELADO' OR @inicio > @agora)
            THROW 50001, N'Presenca ou ausencia so pode ser registrada apos o inicio de evento nao cancelado.', 1;
        SELECT @confirmacao = status_confirmacao, @presenca = presente FROM participacao_evento WITH (UPDLOCK, HOLDLOCK)
            WHERE codigo_agenda = @agenda AND cod_musico = @musico;
        IF @confirmacao IS NULL THROW 50001, N'Participacao nao encontrada.', 1;
        IF COALESCE(@presente, @presenca) = 1 AND COALESCE(@status, @confirmacao) <> N'CONFIRMADO'
            THROW 50001, N'Um participante presente deve permanecer confirmado.', 1;
        UPDATE participacao_evento SET status_confirmacao = COALESCE(@status, status_confirmacao),
            presente = COALESCE(@presente, presente), observacao = COALESCE(@observacao, observacao),
            data_resposta = CASE WHEN @status IS NULL THEN data_resposta ELSE @agora END
            WHERE codigo_agenda = @agenda AND cod_musico = @musico;
        IF @transacao_propria = 1 COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @transacao_propria = 1 AND XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        ELSE IF @transacao_propria = 0 AND XACT_STATE() = 1 ROLLBACK TRANSACTION atualizar_participacao;
        THROW;
    END CATCH
END;
GO
