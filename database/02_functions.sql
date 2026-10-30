USE AgendaBanda;
GO

-- Funcoes utilizadas pelas consultas da aplicacao.

-- Escalar: percentual somente entre presencas/ausencias efetivamente lancadas.
CREATE OR ALTER FUNCTION dbo.fn_taxa_presenca_musico
(@musico INT, @inicio DATETIME2, @fim DATETIME2)
RETURNS DECIMAL(5,2)
AS
BEGIN
    DECLARE @taxa DECIMAL(5,2);
    SELECT @taxa = CAST(100.0 * SUM(CASE WHEN p.presente = 1 THEN 1 ELSE 0 END)
                        / NULLIF(COUNT(*), 0) AS DECIMAL(5,2))
    FROM participacao_evento p JOIN agenda a ON a.codigo_agenda = p.codigo_agenda
    WHERE p.cod_musico = @musico AND a.status = N'REALIZADO' AND p.presente IS NOT NULL
      AND a.dt_hora_in >= @inicio AND a.dt_hora_in < @fim;
    -- NULL significa ausencia de lancamentos, e nao taxa de zero.
    RETURN @taxa;
END;
GO

-- TVF inline: cada evento sobreposto e seus musicos; preserva evento sem musico.
CREATE OR ALTER FUNCTION dbo.fn_conflitos_agendamento
(@inicio DATETIME2, @fim DATETIME2, @ignorar_agenda INT)
RETURNS TABLE
AS RETURN (
    SELECT a.codigo_agenda, a.titulo, a.codigo_local, p.cod_musico
    FROM agenda a LEFT JOIN participacao_evento p ON p.codigo_agenda = a.codigo_agenda
    WHERE a.status <> N'CANCELADO'
      AND (@ignorar_agenda IS NULL OR a.codigo_agenda <> @ignorar_agenda)
      AND a.dt_hora_in < @fim AND a.dt_hora_fim > @inicio
);
GO

-- TVF com variavel-tabela: inicializa todos os tipos e preenche seus indicadores.
CREATE OR ALTER FUNCTION dbo.fn_resumo_periodo (@inicio DATETIME2, @fim DATETIME2)
RETURNS @resultado TABLE (
    codigo_tipo INT, tipo_nome NVARCHAR(50), total INT, agendados INT,
    realizados INT, cancelados INT, minutos_programados BIGINT
)
AS
BEGIN
    INSERT INTO @resultado
        SELECT codigo_tipo, nome, 0, 0, 0, 0, 0 FROM tipos_evento;
    UPDATE r SET total = x.total, agendados = x.agendados, realizados = x.realizados,
        cancelados = x.cancelados, minutos_programados = x.minutos
    FROM @resultado r JOIN (
        SELECT codigo_tipo, COUNT(*) AS total,
            SUM(CASE WHEN status = N'AGENDADO' THEN 1 ELSE 0 END) AS agendados,
            SUM(CASE WHEN status = N'REALIZADO' THEN 1 ELSE 0 END) AS realizados,
            SUM(CASE WHEN status = N'CANCELADO' THEN 1 ELSE 0 END) AS cancelados,
            SUM(CASE WHEN status <> N'CANCELADO' THEN DATEDIFF_BIG(MINUTE, dt_hora_in, dt_hora_fim) ELSE 0 END) AS minutos
        FROM agenda WHERE dt_hora_in >= @inicio AND dt_hora_in < @fim GROUP BY codigo_tipo
    ) x ON x.codigo_tipo = r.codigo_tipo;
    RETURN;
END;
GO
