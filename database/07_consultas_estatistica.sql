USE AgendaBanda;
GO

-- Opcional, depois da instalacao 00 a 06. Execute o arquivo inteiro no SSMS.
-- Apenas consulta os dados da aplicacao; a tabela #base existe somente nesta sessao.
-- Cada resultado pode ser copiado COM CABECALHOS para uma aba do Excel.
-- Roteiro e formulas: ../ESTATISTICA.md.
SET NOCOUNT ON;
DECLARE @inicio DATE='20260101', @fim DATE='20261231';
DECLARE @somente_simulados BIT=1;

IF @fim<@inicio OR @fim='99991231'
    THROW 50001,N'Informe um periodo valido com fim anterior a 31/12/9999.',1;

DROP TABLE IF EXISTS #base_estatistica;

-- Agregar as participacoes ANTES do JOIN preserva uma linha por evento.
-- A duracao e a programada no calendario; nao existe medicao da duracao efetiva.
SELECT a.codigo_agenda,
    CASE WHEN a.titulo LIKE N'SIM-2026-%' THEN N'SIMULADO' ELSE N'OUTRO' END AS origem,
    a.titulo,CONVERT(CHAR(10),a.dt_hora_in,23) AS data_evento,
    CONVERT(CHAR(7),a.dt_hora_in,126) AS mes,
    t.nome AS tipo,a.status,l.nome AS local,c.nome AS cidade,
    DATEDIFF(MINUTE,a.dt_hora_in,a.dt_hora_fim) AS duracao_minutos,
    COALESCE(p.participantes_previstos,0) AS participantes_previstos,
    COALESCE(p.confirmados,0) AS confirmados,
    COALESCE(p.recusados,0) AS recusados,
    COALESCE(p.pendentes,0) AS pendentes,
    -- Presencas/ausencias/sem registro so se aplicam a eventos realizados.
    CASE WHEN a.status=N'REALIZADO' THEN COALESCE(p.presencas,0) END AS presencas,
    CASE WHEN a.status=N'REALIZADO' THEN COALESCE(p.ausencias,0) END AS ausencias,
    CASE WHEN a.status=N'REALIZADO' THEN COALESCE(p.sem_registro,0) END AS sem_registro,
    CASE WHEN a.status=N'REALIZADO' THEN
        CAST(100.0*p.presencas/NULLIF(p.presencas+p.ausencias,0) AS DECIMAL(5,2))
    END AS taxa_presenca_percentual
INTO #base_estatistica
FROM agenda a
JOIN tipos_evento t ON t.codigo_tipo=a.codigo_tipo
JOIN locais l ON l.codigo_local=a.codigo_local
JOIN cidades c ON c.codigo_cidade=l.codigo_cidade
LEFT JOIN (
    SELECT codigo_agenda,COUNT(*) AS participantes_previstos,
        SUM(CASE WHEN status_confirmacao=N'CONFIRMADO' THEN 1 ELSE 0 END) AS confirmados,
        SUM(CASE WHEN status_confirmacao=N'RECUSADO' THEN 1 ELSE 0 END) AS recusados,
        SUM(CASE WHEN status_confirmacao=N'PENDENTE' THEN 1 ELSE 0 END) AS pendentes,
        SUM(CASE WHEN presente=1 THEN 1 ELSE 0 END) AS presencas,
        SUM(CASE WHEN presente=0 THEN 1 ELSE 0 END) AS ausencias,
        SUM(CASE WHEN presente IS NULL THEN 1 ELSE 0 END) AS sem_registro
    FROM participacao_evento GROUP BY codigo_agenda
) p ON p.codigo_agenda=a.codigo_agenda
WHERE a.dt_hora_in>=@inicio AND a.dt_hora_in<DATEADD(DAY,1,@fim)
    AND (@somente_simulados=0 OR a.titulo LIKE N'SIM-2026-%');

-- 1. Aba BaseGeral: calendario completo, incluindo agenda e cancelamentos.
SELECT * FROM #base_estatistica ORDER BY data_evento,codigo_agenda;

-- 2. Aba Eventos: apenas realizados, para medidas de posicao e dispersao.
-- A duracao continua sendo a PROGRAMADA, mesmo quando o evento foi realizado.
SELECT * FROM #base_estatistica WHERE status=N'REALIZADO'
ORDER BY data_evento,codigo_agenda;

-- 3. Aba Mensal: grafico de linhas. Inclui meses sem eventos no periodo.
;WITH meses AS (
    SELECT DATEFROMPARTS(YEAR(@inicio),MONTH(@inicio),1) AS inicio_mes
    UNION ALL
    SELECT DATEADD(MONTH,1,inicio_mes) FROM meses
    WHERE inicio_mes<DATEFROMPARTS(YEAR(@fim),MONTH(@fim),1)
)
SELECT CONVERT(CHAR(7),m.inicio_mes,126) AS mes,COUNT(b.codigo_agenda) AS total_eventos,
    SUM(CASE WHEN b.status=N'REALIZADO' THEN 1 ELSE 0 END) AS realizados,
    SUM(CASE WHEN b.status=N'AGENDADO' THEN 1 ELSE 0 END) AS agendados,
    SUM(CASE WHEN b.status=N'CANCELADO' THEN 1 ELSE 0 END) AS cancelados
FROM meses m LEFT JOIN #base_estatistica b ON b.mes=CONVERT(CHAR(7),m.inicio_mes,126)
GROUP BY m.inicio_mes ORDER BY m.inicio_mes
OPTION (MAXRECURSION 0);

-- 4. Aba Tipos: frequencias absoluta e relativa, para barras e setores.
-- O denominador considera somente os eventos realizados do periodo.
SELECT tipo,COUNT(*) AS frequencia_absoluta,
    CAST(100.0*COUNT(*)/SUM(COUNT(*)) OVER () AS DECIMAL(6,2)) AS frequencia_percentual
FROM #base_estatistica WHERE status=N'REALIZADO'
GROUP BY tipo ORDER BY tipo;

-- 5. Aba Frequencias: histograma em intervalos de 30 minutos [inferior,superior).
-- Intervalos vazios tambem aparecem; um valor no limite superior vai para a proxima faixa.
DECLARE @maior_duracao INT=COALESCE((SELECT MAX(duracao_minutos)
    FROM #base_estatistica WHERE status=N'REALIZADO'),0);
;WITH faixas AS (
    SELECT 0 AS limite_inferior
    UNION ALL
    SELECT limite_inferior+30 FROM faixas WHERE limite_inferior+30<=@maior_duracao
), frequencias AS (
    SELECT f.limite_inferior,f.limite_inferior+30 AS limite_superior,
        COUNT(b.codigo_agenda) AS frequencia_absoluta
    FROM faixas f LEFT JOIN #base_estatistica b ON b.status=N'REALIZADO'
        AND b.duracao_minutos>=f.limite_inferior AND b.duracao_minutos<f.limite_inferior+30
    GROUP BY f.limite_inferior
)
SELECT limite_inferior,limite_superior,
    CONCAT(limite_inferior,N' a menos de ',limite_superior) AS faixa_minutos,
    frequencia_absoluta,
    SUM(frequencia_absoluta) OVER (ORDER BY limite_inferior) AS frequencia_acumulada,
    CAST(100.0*frequencia_absoluta/NULLIF(SUM(frequencia_absoluta) OVER (),0)
        AS DECIMAL(6,2)) AS frequencia_percentual
FROM frequencias ORDER BY limite_inferior
OPTION (MAXRECURSION 0);

-- 6. Aba Regressao: X=musicos previstos, Y=duracao programada em minutos.
-- Somente ENSAIOS realizados, para nao misturar duracoes de categorias diferentes.
-- Relacao simulada com variacao adicional: nao e evidencia de causalidade real.
SELECT codigo_agenda,titulo,participantes_previstos AS x_participantes,
    duracao_minutos AS y_duracao_minutos
FROM #base_estatistica WHERE status=N'REALIZADO' AND tipo=N'ENSAIO'
ORDER BY codigo_agenda;

DROP TABLE #base_estatistica;
GO
