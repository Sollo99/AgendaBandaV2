USE AgendaBanda;
GO

-- Operacoes transacionais chamadas pelos repositorios Java.

IF (SELECT compatibility_level FROM sys.databases WHERE name = DB_NAME()) < 130
    THROW 50001, N'O banco exige nivel de compatibilidade 130 ou superior para OPENJSON.', 1;
GO

CREATE OR ALTER PROCEDURE dbo.usp_excluir_evento @id INT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    -- JDBC pode estar em transacao implicita ainda sem a primeira leitura de tabela.
    -- Inicia essa transacao antes de decidir quem controla o COMMIT.
    DECLARE @iniciar_transacao INT;
    IF @@TRANCOUNT=0 AND (2 & @@OPTIONS)=2
        SELECT TOP (1) @iniciar_transacao=codigo FROM membros;
    DECLARE @propria BIT = CASE WHEN @@TRANCOUNT = 0 THEN 1 ELSE 0 END;
    IF @propria = 1 BEGIN TRANSACTION; ELSE SAVE TRANSACTION excluir_evento;
    BEGIN TRY
        DECLARE @responsavel INT = TRY_CONVERT(INT, SESSION_CONTEXT(N'membro_responsavel'));
        DECLARE @recurso NVARCHAR(255) = CONCAT(N'AgendaBanda:evento:', @id), @resultado_lock INT;
        EXEC @resultado_lock = sys.sp_getapplock @Resource=@recurso, @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=5000;
        IF @resultado_lock < 0 THROW 50004, N'Outro usuario esta alterando este evento. Tente novamente.', 1;

        DECLARE @recursos TABLE (nome NVARCHAR(255) COLLATE Latin1_General_100_BIN2 PRIMARY KEY);
        INSERT INTO @recursos
            SELECT CONCAT(N'AgendaBanda:local:', codigo_local) FROM agenda WHERE codigo_agenda=@id
            UNION SELECT CONCAT(N'AgendaBanda:musico:', cod_musico) FROM participacao_evento WHERE codigo_agenda=@id;
        WHILE EXISTS (SELECT 1 FROM @recursos)
        BEGIN
            SELECT @recurso = MIN(nome) FROM @recursos;
            EXEC @resultado_lock = sys.sp_getapplock @Resource=@recurso, @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=5000;
            IF @resultado_lock < 0 THROW 50004, N'Recurso de agenda em uso. Aguarde e tente novamente.', 1;
            DELETE FROM @recursos WHERE nome=@recurso;
        END;

        DECLARE @status NVARCHAR(20), @criador INT;
        SELECT @status=status, @criador=cod_membro_criador FROM agenda WITH (UPDLOCK,HOLDLOCK) WHERE codigo_agenda=@id;
        IF @criador IS NULL THROW 50001, N'Evento nao encontrado.', 1;
        IF NOT EXISTS (SELECT 1 FROM membros WHERE codigo=@responsavel AND ativo=1 AND status_cadastro=N'APROVADO'
            AND (administrador=1 OR codigo=@criador))
            THROW 50003, N'Apenas o criador ou um administrador pode excluir este evento.', 1;
        IF @status=N'REALIZADO' OR EXISTS (SELECT 1 FROM participacao_evento WHERE codigo_agenda=@id AND presente IS NOT NULL)
            THROW 50001, N'Nao e possivel excluir um evento realizado ou com presenca registrada.', 1;

        -- Captura os detalhes antes do DELETE em cascata e complementa o historico da trigger.
        DECLARE @detalhes NVARCHAR(MAX) = (
            SELECT e.repertorio,e.observacao,r.pauta,s.codigo_cidade FROM agenda a
            LEFT JOIN ensaios e ON e.codigo_agenda=a.codigo_agenda
            LEFT JOIN reunioes r ON r.codigo_agenda=a.codigo_agenda
            LEFT JOIN shows s ON s.codigo_agenda=a.codigo_agenda
            WHERE a.codigo_agenda=@id FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER);
        DECLARE @limite BIGINT = (SELECT COALESCE(MAX(codigo_historico),0) FROM historico_eventos);
        DELETE FROM agenda WHERE codigo_agenda=@id;
        UPDATE historico_eventos SET dados_anteriores=JSON_MODIFY(dados_anteriores,'$.detalhes',JSON_QUERY(@detalhes))
            WHERE codigo_agenda=@id AND codigo_historico>@limite AND tipo_operacao=N'EXCLUSAO';
        IF @propria=1 COMMIT TRANSACTION;
        SELECT @id AS codigo_agenda;
    END TRY
    BEGIN CATCH
        IF @propria=1 AND XACT_STATE()<>0 ROLLBACK TRANSACTION;
        ELSE IF @propria=0 AND XACT_STATE()=1 ROLLBACK TRANSACTION excluir_evento;
        THROW;
    END CATCH
END;
GO

-- NULL preserva o valor atual; a edicao completa envia os dois valores explicitamente.
CREATE OR ALTER PROCEDURE dbo.usp_alterar_acesso_membro
    @id INT, @administrador BIT = NULL, @ativo BIT = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    DECLARE @iniciar_transacao INT;
    IF @@TRANCOUNT=0 AND (2 & @@OPTIONS)=2
        SELECT TOP (1) @iniciar_transacao=codigo FROM membros;
    DECLARE @propria BIT = CASE WHEN @@TRANCOUNT=0 THEN 1 ELSE 0 END;
    IF @propria=1 BEGIN TRANSACTION; ELSE SAVE TRANSACTION alterar_acesso_membro;
    BEGIN TRY
        DECLARE @resultado_lock INT;
        -- Mesmo recurso usado pelas decisoes de cadastro e recuperacao da aplicacao.
        EXEC @resultado_lock = sys.sp_getapplock @Resource=N'AgendaBanda:administradores',
            @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=5000;
        IF @resultado_lock<0 THROW 50004, N'Administracao em uso. Aguarde e tente novamente.', 1;
        DECLARE @responsavel INT = TRY_CONVERT(INT, SESSION_CONTEXT(N'membro_responsavel'));
        IF NOT EXISTS (SELECT 1 FROM membros WHERE codigo=@responsavel AND administrador=1 AND ativo=1 AND status_cadastro=N'APROVADO')
            THROW 50003, N'Apenas administradores ativos podem alterar o acesso de membros.', 1;

        DECLARE @admin_anterior BIT, @ativo_anterior BIT, @cadastro NVARCHAR(10);
        SELECT @admin_anterior=administrador, @ativo_anterior=ativo, @cadastro=status_cadastro
            FROM membros WITH (UPDLOCK,HOLDLOCK) WHERE codigo=@id;
        IF @cadastro IS NULL THROW 50001, N'Membro nao encontrado.', 1;
        SET @administrador=COALESCE(@administrador,@admin_anterior);
        SET @ativo=COALESCE(@ativo,@ativo_anterior);
        IF @cadastro<>N'APROVADO' AND (@administrador=1 OR @ativo=1)
            THROW 50001, N'O cadastro precisa ser aprovado antes de ativar a conta ou conceder administracao.', 1;
        IF @admin_anterior=1 AND @ativo_anterior=1 AND (@administrador=0 OR @ativo=0)
            AND NOT EXISTS (SELECT 1 FROM membros WITH (UPDLOCK,HOLDLOCK)
                WHERE codigo<>@id AND administrador=1 AND ativo=1)
            THROW 50001, N'E necessario manter pelo menos um administrador ativo.', 1;
        -- Evita um historico de UPDATE sem mudanca de acesso.
        IF @admin_anterior<>@administrador OR @ativo_anterior<>@ativo
            UPDATE membros SET administrador=@administrador, ativo=@ativo WHERE codigo=@id;
        IF @propria=1 COMMIT TRANSACTION;
        SELECT @id AS codigo;
    END TRY
    BEGIN CATCH
        IF @propria=1 AND XACT_STATE()<>0 ROLLBACK TRANSACTION;
        ELSE IF @propria=0 AND XACT_STATE()=1 ROLLBACK TRANSACTION alterar_acesso_membro;
        THROW;
    END CATCH
END;
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

CREATE OR ALTER PROCEDURE dbo.usp_decidir_cadastro
    @id INT, @aprovada BIT, @agora DATETIME2
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    DECLARE @iniciar_transacao INT;
    IF @@TRANCOUNT=0 AND (2 & @@OPTIONS)=2
        SELECT TOP (1) @iniciar_transacao=codigo FROM membros;
    DECLARE @propria BIT=CASE WHEN @@TRANCOUNT=0 THEN 1 ELSE 0 END;
    IF @propria=1 BEGIN TRANSACTION; ELSE SAVE TRANSACTION decidir_cadastro;
    BEGIN TRY
        DECLARE @responsavel INT=TRY_CONVERT(INT,SESSION_CONTEXT(N'membro_responsavel')), @resultado_lock INT;
        EXEC @resultado_lock=sys.sp_getapplock @Resource=N'AgendaBanda:administradores',
            @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=5000;
        IF @resultado_lock<0 THROW 50004,N'Administracao em uso. Aguarde e tente novamente.',1;
        IF NOT EXISTS (SELECT 1 FROM membros WHERE codigo=@responsavel AND administrador=1 AND ativo=1 AND status_cadastro=N'APROVADO')
            THROW 50003,N'Apenas administradores ativos podem executar esta operacao.',1;
        IF @aprovada IS NULL OR @agora IS NULL THROW 50001,N'Informe a decisao e sua data.',1;
        DECLARE @status NVARCHAR(10), @liberado DATETIME2;
        SELECT @status=status_cadastro,@liberado=identificadores_liberados_em FROM membros WITH (UPDLOCK,HOLDLOCK) WHERE codigo=@id;
        IF @status IS NULL THROW 50001,N'Membro nao encontrado.',1;
        IF @status=N'APROVADO' OR (@aprovada=0 AND @status=N'RECUSADO')
            THROW 50001,N'O cadastro ja foi analisado.',1;
        IF @liberado IS NOT NULL THROW 50001,N'Os identificadores foram liberados. E necessario fazer um novo cadastro.',1;
        UPDATE membros SET status_cadastro=CASE WHEN @aprovada=1 THEN N'APROVADO' ELSE N'RECUSADO' END,
            ativo=@aprovada,administrador=0,cadastro_decidido_em=@agora,cadastro_decidido_por=@responsavel WHERE codigo=@id;
        IF @propria=1 COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @propria=1 AND XACT_STATE()<>0 ROLLBACK TRANSACTION;
        ELSE IF @propria=0 AND XACT_STATE()=1 ROLLBACK TRANSACTION decidir_cadastro;
        THROW;
    END CATCH
END;
GO

CREATE OR ALTER PROCEDURE dbo.usp_liberar_identificadores
    @id INT, @senha_hash NVARCHAR(255), @agora DATETIME2
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    DECLARE @iniciar_transacao INT;
    IF @@TRANCOUNT=0 AND (2 & @@OPTIONS)=2
        SELECT TOP (1) @iniciar_transacao=codigo FROM membros;
    DECLARE @propria BIT=CASE WHEN @@TRANCOUNT=0 THEN 1 ELSE 0 END;
    IF @propria=1 BEGIN TRANSACTION; ELSE SAVE TRANSACTION liberar_identificadores;
    BEGIN TRY
        DECLARE @responsavel INT=TRY_CONVERT(INT,SESSION_CONTEXT(N'membro_responsavel')), @resultado_lock INT;
        EXEC @resultado_lock=sys.sp_getapplock @Resource=N'AgendaBanda:administradores',
            @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=5000;
        IF @resultado_lock<0 THROW 50004,N'Administracao em uso. Aguarde e tente novamente.',1;
        IF NOT EXISTS (SELECT 1 FROM membros WHERE codigo=@responsavel AND administrador=1 AND ativo=1 AND status_cadastro=N'APROVADO')
            THROW 50003,N'Apenas administradores ativos podem executar esta operacao.',1;
        DECLARE @status NVARCHAR(10),@liberado DATETIME2;
        SELECT @status=status_cadastro,@liberado=identificadores_liberados_em FROM membros WITH (UPDLOCK,HOLDLOCK) WHERE codigo=@id;
        IF @status IS NULL THROW 50001,N'Membro nao encontrado.',1;
        IF @status<>N'RECUSADO' THROW 50001,N'Somente cadastros recusados podem ter identificadores liberados.',1;
        IF @liberado IS NULL
        BEGIN
            IF @agora IS NULL OR @senha_hash IS NULL OR LEN(@senha_hash)<>60 OR LEFT(@senha_hash,4) NOT IN (N'$2a$',N'$2b$',N'$2y$')
                THROW 50001,N'Informe um hash de senha valido e a data da operacao.',1;
            UPDATE membros SET cpf=NULL,email=NULL,login=NULL,senha=@senha_hash,
                identificadores_liberados_em=@agora,identificadores_liberados_por=@responsavel WHERE codigo=@id;
        END;
        DELETE FROM sessoes WHERE codigo_membro=@id;
        IF @propria=1 COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @propria=1 AND XACT_STATE()<>0 ROLLBACK TRANSACTION;
        ELSE IF @propria=0 AND XACT_STATE()=1 ROLLBACK TRANSACTION liberar_identificadores;
        THROW;
    END CATCH
END;
GO

CREATE OR ALTER PROCEDURE dbo.usp_limpar_historicos
    @limite_eventos DATETIME2, @limite_operacoes DATETIME2, @lote INT=1000
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    DECLARE @iniciar_transacao INT;
    IF @@TRANCOUNT=0 AND (2 & @@OPTIONS)=2
        SELECT TOP (1) @iniciar_transacao=codigo FROM membros;
    DECLARE @propria BIT=CASE WHEN @@TRANCOUNT=0 THEN 1 ELSE 0 END;
    IF @propria=1 BEGIN TRANSACTION; ELSE SAVE TRANSACTION limpar_historicos;
    BEGIN TRY
        IF @lote IS NULL OR @lote<1 OR @lote>10000 THROW 50001,N'Tamanho de lote invalido.',1;
        IF @limite_eventos IS NULL OR @limite_operacoes IS NULL THROW 50001,N'Informe os limites de retencao.',1;
        ;WITH vencidos AS (SELECT TOP (@lote) * FROM historico_eventos WHERE data_hora<@limite_eventos ORDER BY data_hora,codigo_historico)
        DELETE FROM vencidos;
        ;WITH vencidos AS (SELECT TOP (@lote) * FROM historico_operacoes WHERE data_hora<@limite_operacoes ORDER BY data_hora,codigo_operacao)
        DELETE FROM vencidos;
        IF @propria=1 COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @propria=1 AND XACT_STATE()<>0 ROLLBACK TRANSACTION;
        ELSE IF @propria=0 AND XACT_STATE()=1 ROLLBACK TRANSACTION limpar_historicos;
        THROW;
    END CATCH
END;
GO

CREATE OR ALTER PROCEDURE dbo.usp_limpar_sessoes
    @agora DATETIME2, @lote INT=1000
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    DECLARE @iniciar_transacao INT;
    IF @@TRANCOUNT=0 AND (2 & @@OPTIONS)=2
        SELECT TOP (1) @iniciar_transacao=codigo FROM membros;
    DECLARE @propria BIT=CASE WHEN @@TRANCOUNT=0 THEN 1 ELSE 0 END;
    IF @propria=1 BEGIN TRANSACTION; ELSE SAVE TRANSACTION limpar_sessoes;
    BEGIN TRY
        IF @lote IS NULL OR @lote<1 OR @lote>10000 THROW 50001,N'Tamanho de lote invalido.',1;
        IF @agora IS NULL THROW 50001,N'Informe a data da operacao.',1;
        ;WITH vencidas AS (SELECT TOP (@lote) * FROM sessoes WHERE expira_em<=@agora ORDER BY expira_em,codigo_sessao)
        DELETE FROM vencidas;
        DECLARE @removidas INT=@@ROWCOUNT;
        IF @propria=1 COMMIT TRANSACTION;
        SELECT @removidas AS removidas;
    END TRY
    BEGIN CATCH
        IF @propria=1 AND XACT_STATE()<>0 ROLLBACK TRANSACTION;
        ELSE IF @propria=0 AND XACT_STATE()=1 ROLLBACK TRANSACTION limpar_sessoes;
        THROW;
    END CATCH
END;
GO

CREATE OR ALTER PROCEDURE dbo.usp_expirar_recuperacoes
    @membro INT, @agora DATETIME2
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    DECLARE @iniciar_transacao INT;
    IF @@TRANCOUNT=0 AND (2 & @@OPTIONS)=2
        SELECT TOP (1) @iniciar_transacao=codigo FROM membros;
    DECLARE @propria BIT=CASE WHEN @@TRANCOUNT=0 THEN 1 ELSE 0 END;
    IF @propria=1 BEGIN TRANSACTION; ELSE SAVE TRANSACTION expirar_recuperacoes;
    BEGIN TRY
        IF @agora IS NULL THROW 50001,N'Informe a data da operacao.',1;
        DECLARE @codigo INT;
        SELECT @codigo=codigo FROM membros WITH (UPDLOCK,HOLDLOCK) WHERE codigo=@membro;
        UPDATE redefinicoes_senha SET status=N'EXPIRADA',encerrado_em=@agora,motivo_encerramento=N'Prazo de recuperacao encerrado.'
            WHERE codigo_membro=@codigo AND status IN (N'PENDENTE',N'APROVADA') AND expira_em<=@agora;
        DECLARE @alteradas INT=@@ROWCOUNT;
        IF @propria=1 COMMIT TRANSACTION;
        SELECT @alteradas AS alteradas;
    END TRY
    BEGIN CATCH
        IF @propria=1 AND XACT_STATE()<>0 ROLLBACK TRANSACTION;
        ELSE IF @propria=0 AND XACT_STATE()=1 ROLLBACK TRANSACTION expirar_recuperacoes;
        THROW;
    END CATCH
END;
GO

CREATE OR ALTER PROCEDURE dbo.usp_cancelar_recuperacoes
    @membro INT, @agora DATETIME2, @motivo NVARCHAR(200), @solicitacao INT=NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    DECLARE @iniciar_transacao INT;
    IF @@TRANCOUNT=0 AND (2 & @@OPTIONS)=2
        SELECT TOP (1) @iniciar_transacao=codigo FROM membros;
    DECLARE @propria BIT=CASE WHEN @@TRANCOUNT=0 THEN 1 ELSE 0 END;
    IF @propria=1 BEGIN TRANSACTION; ELSE SAVE TRANSACTION cancelar_recuperacoes;
    BEGIN TRY
        IF @agora IS NULL OR NULLIF(LTRIM(RTRIM(@motivo)),N'') IS NULL THROW 50001,N'Informe a data e o motivo do encerramento.',1;
        DECLARE @responsavel INT=TRY_CONVERT(INT,SESSION_CONTEXT(N'membro_responsavel')),@codigo INT,@resultado_lock INT;
        -- O proprio membro pode estar sendo desativado na mesma edicao em que troca a senha.
        IF @responsavel<>@membro
        BEGIN
            EXEC @resultado_lock=sys.sp_getapplock @Resource=N'AgendaBanda:administradores',
                @LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=5000;
            IF @resultado_lock<0 THROW 50004,N'Administracao em uso. Aguarde e tente novamente.',1;
        END;
        IF @responsavel IS NULL OR (@responsavel<>@membro AND NOT EXISTS (
            SELECT 1 FROM membros WHERE codigo=@responsavel AND administrador=1 AND ativo=1 AND status_cadastro=N'APROVADO'))
            THROW 50003,N'Operacao nao autorizada.',1;
        SELECT @codigo=codigo FROM membros WITH (UPDLOCK,HOLDLOCK) WHERE codigo=@membro;
        IF @codigo IS NULL THROW 50001,N'Membro nao encontrado.',1;
        UPDATE redefinicoes_senha SET
            status=CASE WHEN @solicitacao IS NOT NULL AND expira_em<=@agora THEN N'EXPIRADA' ELSE N'CANCELADA' END,
            encerrado_em=@agora,
            motivo_encerramento=CASE WHEN @solicitacao IS NOT NULL AND expira_em<=@agora THEN N'Prazo de recuperacao encerrado.' ELSE @motivo END
            WHERE codigo_membro=@membro AND status IN (N'PENDENTE',N'APROVADA')
                AND (@solicitacao IS NULL OR codigo_redefinicao=@solicitacao);
        DECLARE @alteradas INT=@@ROWCOUNT;
        IF @propria=1 COMMIT TRANSACTION;
        SELECT @alteradas AS alteradas;
    END TRY
    BEGIN CATCH
        IF @propria=1 AND XACT_STATE()<>0 ROLLBACK TRANSACTION;
        ELSE IF @propria=0 AND XACT_STATE()=1 ROLLBACK TRANSACTION cancelar_recuperacoes;
        THROW;
    END CATCH
END;
GO

CREATE OR ALTER PROCEDURE dbo.usp_excluir_local
    @id INT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    DECLARE @iniciar_transacao INT;
    IF @@TRANCOUNT=0 AND (2 & @@OPTIONS)=2
        SELECT TOP (1) @iniciar_transacao=codigo FROM membros;
    DECLARE @propria BIT=CASE WHEN @@TRANCOUNT=0 THEN 1 ELSE 0 END;
    IF @propria=1 BEGIN TRANSACTION; ELSE SAVE TRANSACTION excluir_local;
    BEGIN TRY
        DECLARE @responsavel INT=TRY_CONVERT(INT,SESSION_CONTEXT(N'membro_responsavel'));
        IF NOT EXISTS (SELECT 1 FROM membros WHERE codigo=@responsavel AND administrador=1 AND ativo=1 AND status_cadastro=N'APROVADO')
            THROW 50003,N'Apenas administradores ativos podem excluir locais.',1;
        DECLARE @recurso NVARCHAR(255)=CONCAT(N'AgendaBanda:local:',@id),@resultado_lock INT,@codigo INT;
        EXEC @resultado_lock=sys.sp_getapplock @Resource=@recurso,@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=5000;
        IF @resultado_lock<0 THROW 50004,N'Recurso de agenda em uso. Aguarde e tente novamente.',1;
        SELECT @codigo=codigo_local FROM locais WITH (UPDLOCK,HOLDLOCK) WHERE codigo_local=@id;
        IF @codigo IS NULL THROW 50001,N'Local nao encontrado.',1;
        IF EXISTS (SELECT 1 FROM agenda WHERE codigo_local=@id)
            THROW 50001,N'O local esta vinculado a eventos e nao pode ser excluido.',1;
        DELETE FROM locais WHERE codigo_local=@id;
        IF @propria=1 COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @propria=1 AND XACT_STATE()<>0 ROLLBACK TRANSACTION;
        ELSE IF @propria=0 AND XACT_STATE()=1 ROLLBACK TRANSACTION excluir_local;
        THROW;
    END CATCH
END;
GO

-- Eventos de autenticacao sao explicitos: limpeza de sessao vencida nao representa logout.
CREATE OR ALTER PROCEDURE dbo.usp_registrar_autenticacao
    @operacao NVARCHAR(20), @membro INT=NULL, @motivo NVARCHAR(30), @recurso NVARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    IF @operacao IS NULL OR @operacao NOT IN (N'LOGIN',N'LOGOUT',N'LOGIN_FALHOU',N'ACESSO_NEGADO',N'LIMITE_EXCEDIDO')
        THROW 50001,N'Operacao de autenticacao invalida.',1;
    IF @motivo IS NULL OR @motivo NOT IN (N'SUCESSO',N'CREDENCIAIS_INVALIDAS',N'SEM_SESSAO',N'SEM_PERMISSAO',N'CSRF_INVALIDO',N'LIMITE_EXCEDIDO',N'CODIGO_INVALIDO')
        THROW 50001,N'Motivo de autenticacao invalido.',1;
    IF @recurso IS NULL OR @recurso NOT IN (N'LOGIN',N'LOGOUT',N'API',N'CADASTRO',N'RECUPERACAO')
        THROW 50001,N'Recurso de autenticacao invalido.',1;
    IF @membro IS NOT NULL AND @membro<1 THROW 50001,N'Membro invalido.',1;
    IF @operacao IN (N'LOGIN',N'LOGOUT') AND (@membro IS NULL OR @motivo<>N'SUCESSO')
        THROW 50001,N'Autenticacao bem sucedida exige membro identificado.',1;
    IF (@operacao=N'LOGIN_FALHOU' AND @motivo<>N'CREDENCIAIS_INVALIDAS')
        OR (@operacao=N'LIMITE_EXCEDIDO' AND @motivo<>N'LIMITE_EXCEDIDO')
        OR (@operacao=N'ACESSO_NEGADO' AND @motivo NOT IN (N'SEM_SESSAO',N'SEM_PERMISSAO',N'CSRF_INVALIDO',N'CODIGO_INVALIDO'))
        THROW 50001,N'Motivo incompativel com a operacao de autenticacao.',1;
    IF (@operacao IN (N'LOGIN',N'LOGIN_FALHOU') AND @recurso<>N'LOGIN')
        OR (@operacao=N'LOGOUT' AND @recurso<>N'LOGOUT')
        THROW 50001,N'Recurso incompativel com a operacao de autenticacao.',1;
    DECLARE @ator INT=NULL;
    -- Login recusado nunca atribui a tentativa ao titular da conta.
    IF @operacao<>N'LOGIN_FALHOU'
        SELECT @ator=codigo FROM membros WHERE codigo=@membro;
    IF @operacao IN (N'LOGIN',N'LOGOUT') AND @ator IS NULL
        THROW 50001,N'Membro nao encontrado.',1;
    DECLARE @dados NVARCHAR(MAX)=(SELECT @motivo AS motivo,@recurso AS recurso FOR JSON PATH,WITHOUT_ARRAY_WRAPPER);
    INSERT INTO historico_operacoes(membro_responsavel,entidade_afetada,codigo_registro_afetado,tipo_operacao,data_hora,dados_anteriores,dados_novos)
        VALUES (@ator,N'AUTENTICACAO',COALESCE(CONVERT(NVARCHAR(100),@membro),N'NAO_IDENTIFICADO'),@operacao,
            CONVERT(DATETIME2,SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time'),NULL,@dados);
END;
GO
