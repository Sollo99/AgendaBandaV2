USE AgendaBanda;
GO

-- Views utilizadas pela agenda, autenticacao e relatorios.

CREATE OR ALTER VIEW dbo.vw_agenda_detalhada AS
SELECT a.codigo_agenda, a.titulo, a.descricao, a.dt_hora_in, a.dt_hora_fim, a.status,
    l.codigo_local, l.nome AS local_nome, l.endereco, l.complemento,
    c.codigo_cidade, c.nome AS cidade_nome, c.uf,
    m.codigo AS criador_codigo, m.nome AS criador_nome,
    t.codigo_tipo, t.nome AS tipo_nome, t.descricao AS tipo_descricao, t.categoria_detalhe, t.ativo AS tipo_ativo,
    e.repertorio, e.observacao AS ensaio_observacao, r.pauta,
    cs.codigo_cidade AS show_cidade_codigo, cs.nome AS show_cidade_nome, cs.uf AS show_uf,
    COALESCE(p.participantes, 0) AS total_participantes,
    COALESCE(p.confirmados, 0) AS total_confirmados
FROM agenda a
JOIN locais l ON l.codigo_local = a.codigo_local
JOIN cidades c ON c.codigo_cidade = l.codigo_cidade
JOIN membros m ON m.codigo = a.cod_membro_criador
JOIN tipos_evento t ON t.codigo_tipo = a.codigo_tipo
LEFT JOIN ensaios e ON e.codigo_agenda = a.codigo_agenda
LEFT JOIN reunioes r ON r.codigo_agenda = a.codigo_agenda
LEFT JOIN shows s ON s.codigo_agenda = a.codigo_agenda
LEFT JOIN cidades cs ON cs.codigo_cidade = s.codigo_cidade
LEFT JOIN (
    SELECT codigo_agenda, COUNT(*) AS participantes,
        SUM(CASE WHEN status_confirmacao = N'CONFIRMADO' THEN 1 ELSE 0 END) AS confirmados
    FROM participacao_evento GROUP BY codigo_agenda
) p ON p.codigo_agenda = a.codigo_agenda;
GO

-- Uma linha por musico/evento: permite filtrar o periodo antes de agregar no Java/SQL.
CREATE OR ALTER VIEW dbo.vw_presenca_musicos AS
SELECT mu.codigo AS codigo_musico, m.nome AS musico_nome, mu.instrumento,
    a.codigo_agenda, a.titulo, a.dt_hora_in, a.status AS evento_status,
    t.nome AS tipo_nome, l.nome AS local_nome,
    p.status_confirmacao, p.presente,
    CASE WHEN a.status = N'REALIZADO' THEN 1 ELSE 0 END AS realizado,
    CASE WHEN a.status = N'REALIZADO' AND p.presente = 1 THEN 1 ELSE 0 END AS presenca,
    CASE WHEN a.status = N'REALIZADO' AND p.presente = 0 THEN 1 ELSE 0 END AS ausencia,
    CASE WHEN a.status = N'REALIZADO' AND p.presente IS NULL THEN 1 ELSE 0 END AS sem_registro
FROM participacao_evento p
JOIN musicos mu ON mu.codigo = p.cod_musico
JOIN membros m ON m.codigo = mu.codigo
JOIN agenda a ON a.codigo_agenda = p.codigo_agenda
JOIN tipos_evento t ON t.codigo_tipo = a.codigo_tipo
JOIN locais l ON l.codigo_local = a.codigo_local;
GO

-- Uma linha por local/evento, incluindo locais sem agenda.
CREATE OR ALTER VIEW dbo.vw_ocupacao_locais AS
SELECT l.codigo_local, l.nome AS local_nome, c.nome AS cidade_nome, c.uf,
    a.codigo_agenda, a.dt_hora_in, a.status,
    t.nome AS tipo_nome,
    CASE WHEN a.status = N'AGENDADO' THEN 1 ELSE 0 END AS agendado,
    CASE WHEN a.status = N'REALIZADO' THEN 1 ELSE 0 END AS realizado,
    CASE WHEN a.status = N'CANCELADO' THEN 1 ELSE 0 END AS cancelado,
    CASE WHEN a.status <> N'CANCELADO' THEN DATEDIFF_BIG(MINUTE, a.dt_hora_in, a.dt_hora_fim) ELSE 0 END AS minutos_programados
FROM locais l JOIN cidades c ON c.codigo_cidade = l.codigo_cidade
LEFT JOIN agenda a ON a.codigo_local = l.codigo_local
LEFT JOIN tipos_evento t ON t.codigo_tipo = a.codigo_tipo;
GO

CREATE OR ALTER VIEW dbo.vw_historico_unificado AS
SELECT h.codigo, h.entidade, h.registro, h.operacao, h.data_hora, h.membro_responsavel,
    h.dados_anteriores, h.dados_novos, m.nome AS responsavel
FROM (
    SELECT codigo_historico AS codigo, N'AGENDA' AS entidade,
        CONVERT(NVARCHAR(100), codigo_agenda) AS registro, tipo_operacao AS operacao,
        data_hora, membro_responsavel, dados_anteriores, dados_novos
    FROM historico_eventos
    UNION ALL
    SELECT codigo_operacao, entidade_afetada, codigo_registro_afetado, tipo_operacao,
        data_hora, membro_responsavel, dados_anteriores, dados_novos
    FROM historico_operacoes
) h LEFT JOIN membros m ON m.codigo = h.membro_responsavel;
GO

-- O prazo e aplicado na consulta parametrizada; desativacao e recusa retiram o acesso imediatamente.
CREATE OR ALTER VIEW dbo.vw_sessoes_membros AS
SELECT s.codigo_sessao,s.token_hash,s.criado_em,s.expira_em,
    m.codigo,m.nome,m.login,m.administrador
FROM sessoes s JOIN membros m ON m.codigo=s.codigo_membro
WHERE m.ativo=1 AND m.status_cadastro=N'APROVADO';
GO
