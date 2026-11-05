USE AgendaBanda;
GO

-- Dados inteiramente simulados para demonstracao e para a etapa de Estatistica.
-- Executar uma unica vez, depois dos scripts 00 a 05, em uma base recem-preparada.
-- A carga nao apaga nem substitui registros de uma instalacao existente.
SET NOCOUNT ON;
-- A carga depende da auditoria instalada: todos os exemplos geram historicos pelas triggers.
SET XACT_ABORT ON;
IF (SELECT COUNT(*) FROM sys.triggers WHERE is_disabled=0 AND object_id IN (
    OBJECT_ID(N'dbo.trg_agenda_historico',N'TR'),
    OBJECT_ID(N'dbo.trg_participacao_historico',N'TR'),
    OBJECT_ID(N'dbo.trg_membros_historico',N'TR'),
    OBJECT_ID(N'dbo.trg_cidades_historico',N'TR'),
    OBJECT_ID(N'dbo.trg_locais_historico',N'TR'),
    OBJECT_ID(N'dbo.trg_tipos_evento_historico',N'TR'),
    OBJECT_ID(N'dbo.trg_musicos_historico',N'TR'),
    OBJECT_ID(N'dbo.trg_produtores_historico',N'TR'),
    OBJECT_ID(N'dbo.trg_recuperacoes_historico',N'TR')
))<>9
    THROW 50001,N'Execute os scripts 00 a 05 antes da carga inicial; as triggers de auditoria devem estar habilitadas.',1;

IF EXISTS (SELECT 1 FROM membros) OR EXISTS (SELECT 1 FROM cidades)
    OR EXISTS (SELECT 1 FROM locais) OR EXISTS (SELECT 1 FROM tipos_evento)
    OR EXISTS (SELECT 1 FROM agenda)
    THROW 50001,N'A carga inicial exige uma base vazia. Os dados existentes foram preservados; nao execute novamente o script 06.',1;

-- Carga de instalacao nao representa uma acao de um usuario autenticado.
EXEC sys.sp_set_session_context @key=N'membro_responsavel',@value=NULL;

BEGIN TRY
    BEGIN TRANSACTION;

INSERT INTO membros (nome, cpf, email, login, senha, administrador, ativo, perfil) VALUES
(N'Administrador', N'90000000175', N'admin@banda.local', N'admin', '$2y$05$3FRvl9fdRnF5fEOqIBW2VerMK840C3RA6xwC2CPcO5agcuRYHtXqm', 1, 1, N'MUSICO'),
(N'Usuario Teste', N'90000000256', N'usuario@banda.local', N'usuario', '$2y$05$3oagEcFOHAlQPEa9TM3fM.LVLxw1bbB7GbcAuRfhuN8ySQnqheWzi', 0, 1, N'MUSICO'),
(N'Conta Teste', N'90000000337', N'teste@banda.local', N'teste', '$2y$05$jhAVJlRJeb71xVRK7EqRmuV9X1J3.vr1zfZHgK2hIYUN.4liQN52G', 0, 1, N'MUSICO'),
(N'Ana Silva', N'90000000418', N'ana@banda.local', N'ana', '$2y$05$sQQgwrIFOTxi0MduVmg8KuRRGry63ppKHocOXQ0iB5cUldTIK5V3S', 0, 1, N'MUSICO'),
(N'Bruno Costa', N'90000000507', N'bruno@banda.local', N'bruno', '$2y$05$sQQgwrIFOTxi0MduVmg8KuRRGry63ppKHocOXQ0iB5cUldTIK5V3S', 0, 1, N'MUSICO'),
(N'Carla Souza', N'90000000680', N'carla@banda.local', N'carla', '$2y$05$sQQgwrIFOTxi0MduVmg8KuRRGry63ppKHocOXQ0iB5cUldTIK5V3S', 0, 1, N'MUSICO'),
(N'Diego Lima', N'90000000760', N'diego@banda.local', N'diego', '$2y$05$sQQgwrIFOTxi0MduVmg8KuRRGry63ppKHocOXQ0iB5cUldTIK5V3S', 0, 1, N'MUSICO'),
(N'Elisa Rocha', N'90000000841', N'elisa@banda.local', N'elisa', '$2y$05$sQQgwrIFOTxi0MduVmg8KuRRGry63ppKHocOXQ0iB5cUldTIK5V3S', 0, 1, N'MUSICO'),
(N'Felipe Martins', N'90000000922', N'felipe@banda.local', N'felipe', '$2y$05$sQQgwrIFOTxi0MduVmg8KuRRGry63ppKHocOXQ0iB5cUldTIK5V3S', 0, 1, N'MUSICO'),
(N'Gabriela Alves', N'90000001066', N'gabriela@banda.local', N'gabriela', '$2y$05$sQQgwrIFOTxi0MduVmg8KuRRGry63ppKHocOXQ0iB5cUldTIK5V3S', 0, 1, N'MUSICO');

INSERT INTO musicos (codigo, instrumento, biografia) VALUES
(1, N'Direcao musical', N'Responsavel pela organizacao musical da banda.'),
(2, N'Vocal', N'Vocalista principal da banda.'),
(3, N'Guitarra', N'Responsavel por solos e riffs.'),
(4, N'Baixo', N'Responsavel pela base grave.'),
(5, N'Bateria', N'Responsavel pela conducao ritmica.'),
(6, N'Teclado', N'Responsavel por timbres e harmonia.'),
(7, N'Violao', N'Acompanha ensaios acusticos.'),
(8, N'Percussao', N'Apoio ritmico em apresentacoes.'),
(9, N'Saxofone', N'Participa de arranjos especiais.'),
(10, N'Backing vocal', N'Apoio vocal nas musicas.');

INSERT INTO membros (nome, cpf, email, login, senha, administrador, ativo, perfil) VALUES
(N'Produtor demonstracao 1', N'90000001147', N'produtor1@banda.local', N'produtor1', '$2y$05$sQQgwrIFOTxi0MduVmg8KuRRGry63ppKHocOXQ0iB5cUldTIK5V3S', 0, 1, N'PRODUTOR'),
(N'Produtor demonstracao 2', N'90000001228', N'produtor2@banda.local', N'produtor2', '$2y$05$sQQgwrIFOTxi0MduVmg8KuRRGry63ppKHocOXQ0iB5cUldTIK5V3S', 0, 1, N'PRODUTOR'),
(N'Produtor demonstracao 3', N'90000001309', N'produtor3@banda.local', N'produtor3', '$2y$05$sQQgwrIFOTxi0MduVmg8KuRRGry63ppKHocOXQ0iB5cUldTIK5V3S', 0, 1, N'PRODUTOR'),
(N'Produtor demonstracao 4', N'90000001490', N'produtor4@banda.local', N'produtor4', '$2y$05$sQQgwrIFOTxi0MduVmg8KuRRGry63ppKHocOXQ0iB5cUldTIK5V3S', 0, 1, N'PRODUTOR'),
(N'Produtor demonstracao 5', N'90000001570', N'produtor5@banda.local', N'produtor5', '$2y$05$sQQgwrIFOTxi0MduVmg8KuRRGry63ppKHocOXQ0iB5cUldTIK5V3S', 0, 1, N'PRODUTOR'),
(N'Produtor demonstracao 6', N'90000001651', N'produtor6@banda.local', N'produtor6', '$2y$05$sQQgwrIFOTxi0MduVmg8KuRRGry63ppKHocOXQ0iB5cUldTIK5V3S', 0, 1, N'PRODUTOR'),
(N'Produtor demonstracao 7', N'90000001732', N'produtor7@banda.local', N'produtor7', '$2y$05$sQQgwrIFOTxi0MduVmg8KuRRGry63ppKHocOXQ0iB5cUldTIK5V3S', 0, 1, N'PRODUTOR'),
(N'Produtor demonstracao 8', N'90000001813', N'produtor8@banda.local', N'produtor8', '$2y$05$sQQgwrIFOTxi0MduVmg8KuRRGry63ppKHocOXQ0iB5cUldTIK5V3S', 0, 1, N'PRODUTOR'),
(N'Produtor demonstracao 9', N'90000001902', N'produtor9@banda.local', N'produtor9', '$2y$05$sQQgwrIFOTxi0MduVmg8KuRRGry63ppKHocOXQ0iB5cUldTIK5V3S', 0, 1, N'PRODUTOR'),
(N'Produtor demonstracao 10', N'90000002038', N'produtor10@banda.local', N'produtor10', '$2y$05$sQQgwrIFOTxi0MduVmg8KuRRGry63ppKHocOXQ0iB5cUldTIK5V3S', 0, 1, N'PRODUTOR');

INSERT INTO produtores (codigo, especialidade) VALUES
(11, N'Administracao geral'),
(12, N'Agenda'),
(13, N'Contratos'),
(14, N'Producao executiva'),
(15, N'Producao musical'),
(16, N'Comunicacao'),
(17, N'Logistica'),
(18, N'Som ao vivo'),
(19, N'Iluminacao'),
(20, N'Redes sociais');

INSERT INTO cidades (nome, uf) VALUES
(N'Sao Paulo', 'SP'),
(N'Guarulhos', 'SP'),
(N'Osasco', 'SP'),
(N'Campinas', 'SP'),
(N'Santos', 'SP'),
(N'Sorocaba', 'SP'),
(N'Jundiai', 'SP'),
(N'Sao Bernardo do Campo', 'SP'),
(N'Santo Andre', 'SP'),
(N'Mogi das Cruzes', 'SP');

INSERT INTO locais (nome, endereco, complemento, codigo_cidade) VALUES
(N'Estudio Central', N'Rua das Bandas, 100', N'Sala 2', 1),
(N'Casa de Shows Aurora', N'Av. Principal, 500', N'Palco interno', 2),
(N'Centro Cultural Norte', N'Rua Cultura, 88', N'Auditorio', 3),
(N'Garagem do Bruno', N'Rua Harmonia, 45', N'Fundos', 1),
(N'Teatro Municipal', N'Praca Central, 10', N'Entrada lateral', 4),
(N'Espaco Jovem', N'Av. Juventude, 300', N'Sala multiuso', 5),
(N'Estudio Mix', N'Rua do Som, 77', N'Sala A', 6),
(N'Galpao Musical', N'Rua Industrial, 900', N'Portao 3', 7),
(N'Sede da Banda', N'Rua dos Artistas, 12', N'1o andar', 8),
(N'Praca de Eventos', N'Av. Eventos, 1000', N'Palco externo', 9);

INSERT INTO tipos_evento (nome, descricao, categoria_detalhe, ativo) VALUES
(N'ENSAIO', N'Encontro para prática e preparação musical.', N'ENSAIO', 1),
(N'SHOW', N'Apresentação pública da banda.', N'SHOW', 1),
(N'REUNIAO', N'Encontro de organização e planejamento.', N'REUNIAO', 1);

-- Cenario fixo de 2026: 120 eventos, com volumes mensais diferentes e sem sobreposicao.
-- Janeiro a outubro: realizados, exceto cancelamentos. Novembro/dezembro: agenda futura
-- no cenario de referencia (01/11/2026). As datas nao mudam conforme o dia da instalacao.
-- A duracao PROGRAMADA depende de participantes previstos, tipo e variacao adicional.
-- Essa relacao foi construida para o exercicio; nao comprova causalidade no mundo real.
DECLARE @eventos TABLE (
    sequencia INT PRIMARY KEY,
    titulo NVARCHAR(120) NOT NULL,
    codigo_tipo INT NOT NULL,
    inicio DATETIME2 NOT NULL,
    duracao_minutos INT NOT NULL,
    participantes INT NOT NULL,
    codigo_local INT NOT NULL,
    criador INT NOT NULL,
    status NVARCHAR(20) NOT NULL
);

;WITH meses AS (
    SELECT mes, quantidade FROM (VALUES
        (1,6),(2,7),(3,8),(4,9),(5,10),(6,11),
        (7,12),(8,13),(9,14),(10,11),(11,10),(12,9)
    ) v(mes,quantidade)
), numeros AS (
    SELECT numero FROM (VALUES (1),(2),(3),(4),(5),(6),(7),
        (8),(9),(10),(11),(12),(13),(14)) v(numero)
), sequencias AS (
    SELECT m.mes, n.numero,
        CONVERT(INT,ROW_NUMBER() OVER (ORDER BY m.mes,n.numero)) AS sequencia
    FROM meses m JOIN numeros n ON n.numero<=m.quantidade
)
INSERT INTO @eventos
SELECT s.sequencia,
    CONCAT(N'SIM-2026-',RIGHT(CONCAT(N'000',s.sequencia),3),N' - ',
        CASE t.codigo_tipo WHEN 1 THEN N'Ensaio' WHEN 2 THEN N'Show' ELSE N'Reuniao' END),
    t.codigo_tipo,
    DATEADD(HOUR,CASE t.codigo_tipo WHEN 2 THEN 19 ELSE 18 END,
        CONVERT(DATETIME2,DATEFROMPARTS(2026,s.mes,s.numero*2))),
    CASE t.codigo_tipo WHEN 1 THEN 45+p.quantidade*15
        WHEN 2 THEN 75+p.quantidade*15 ELSE 30+p.quantidade*10 END
        + ((s.sequencia*11+s.mes*3)%5-2)*15,
    p.quantidade, 1+(s.sequencia*3+s.mes)%10, 1+(s.sequencia-1)%20,
    CASE WHEN s.sequencia%13=0 THEN N'CANCELADO'
        WHEN s.mes<=10 THEN N'REALIZADO' ELSE N'AGENDADO' END
FROM sequencias s
CROSS APPLY (SELECT CASE WHEN s.sequencia%10<6 THEN 1
    WHEN s.sequencia%10<9 THEN 2 ELSE 3 END AS codigo_tipo) t
CROSS APPLY (SELECT 2+(s.sequencia*7+s.mes)%
    (CASE WHEN t.codigo_tipo=3 THEN 5 ELSE 9 END) AS quantidade) p;

DECLARE @inseridos TABLE (codigo_agenda INT NOT NULL, titulo NVARCHAR(120) NOT NULL);
INSERT INTO agenda (titulo,descricao,dt_hora_in,dt_hora_fim,codigo_local,
    cod_membro_criador,codigo_tipo,status,criado_em)
OUTPUT inserted.codigo_agenda,inserted.titulo INTO @inseridos
SELECT titulo,N'Dado simulado para demonstracao e analise estatistica; nao representa atividade real.',
    inicio,DATEADD(MINUTE,duracao_minutos,inicio),codigo_local,criador,codigo_tipo,status,
    DATEADD(DAY,-7,inicio)
FROM @eventos;

INSERT INTO ensaios (codigo_agenda,repertorio,observacao)
SELECT i.codigo_agenda,N'Repertorio demonstrativo: abertura, balada e encerramento.',
    N'Ensaio simulado com duracao e formacao variadas.'
FROM @eventos e JOIN @inseridos i ON i.titulo=e.titulo WHERE e.codigo_tipo=1;

INSERT INTO shows (codigo_agenda,codigo_cidade)
SELECT i.codigo_agenda,l.codigo_cidade
FROM @eventos e JOIN @inseridos i ON i.titulo=e.titulo
JOIN locais l ON l.codigo_local=e.codigo_local WHERE e.codigo_tipo=2;

INSERT INTO reunioes (codigo_agenda,pauta)
SELECT i.codigo_agenda,N'Planejamento simulado de repertorio, agenda, transporte e divulgacao.'
FROM @eventos e JOIN @inseridos i ON i.titulo=e.titulo WHERE e.codigo_tipo=3;

-- De 2 a 10 musicos por evento (reunioes: de 2 a 6), alternando os convidados.
-- NULL em presente significa sem lancamento, inclusive em eventos nao realizados.
INSERT INTO participacao_evento (codigo_agenda,cod_musico,status_confirmacao,presente,data_resposta)
SELECT i.codigo_agenda,1+(n.numero+e.sequencia-2)%10,c.status_confirmacao,
    CASE WHEN e.status<>N'REALIZADO' OR v.valor IN (0,3) THEN NULL
        WHEN v.valor BETWEEN 1 AND 6 THEN 0 ELSE 1 END,
    CASE WHEN c.status_confirmacao=N'PENDENTE' THEN NULL
        ELSE DATEADD(HOUR,-(12+(e.sequencia+n.numero)%5*6),e.inicio) END
FROM @eventos e JOIN @inseridos i ON i.titulo=e.titulo
JOIN (VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10)) n(numero)
    ON n.numero<=e.participantes
CROSS APPLY (SELECT (e.sequencia*7+n.numero*3+MONTH(e.inicio))%20 AS valor) v
CROSS APPLY (SELECT CASE WHEN v.valor=0 OR (e.status<>N'REALIZADO' AND v.valor>=12)
    THEN N'PENDENTE' WHEN v.valor IN (1,2) THEN N'RECUSADO'
    ELSE N'CONFIRMADO' END AS status_confirmacao) c;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
