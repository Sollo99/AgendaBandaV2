USE AgendaBanda;
GO

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
GO

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
GO

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
GO

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
GO

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
GO

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
GO

INSERT INTO tipos_evento (nome, descricao, categoria_detalhe, ativo) VALUES
(N'ENSAIO', N'Encontro para prática e preparação musical.', N'ENSAIO', 1),
(N'SHOW', N'Apresentação pública da banda.', N'SHOW', 1),
(N'REUNIAO', N'Encontro de organização e planejamento.', N'REUNIAO', 1);
GO

INSERT INTO agenda (titulo, descricao, dt_hora_in, dt_hora_fim, codigo_local, cod_membro_criador) VALUES
(N'Ensaio 1', N'Ensaio geral da banda numero 1.', '2026-06-01T19:00:00', '2026-06-01T21:00:00', 1, 1),
(N'Ensaio 2', N'Ensaio geral da banda numero 2.', '2026-06-02T19:00:00', '2026-06-02T21:00:00', 2, 2),
(N'Ensaio 3', N'Ensaio geral da banda numero 3.', '2026-06-03T19:00:00', '2026-06-03T21:00:00', 3, 3),
(N'Ensaio 4', N'Ensaio geral da banda numero 4.', '2026-06-04T19:00:00', '2026-06-04T21:00:00', 4, 4),
(N'Ensaio 5', N'Ensaio geral da banda numero 5.', '2026-06-05T19:00:00', '2026-06-05T21:00:00', 5, 5),
(N'Ensaio 6', N'Ensaio geral da banda numero 6.', '2026-06-06T19:00:00', '2026-06-06T21:00:00', 6, 6),
(N'Ensaio 7', N'Ensaio geral da banda numero 7.', '2026-06-07T19:00:00', '2026-06-07T21:00:00', 7, 7),
(N'Ensaio 8', N'Ensaio geral da banda numero 8.', '2026-06-08T19:00:00', '2026-06-08T21:00:00', 8, 8),
(N'Ensaio 9', N'Ensaio geral da banda numero 9.', '2026-06-09T19:00:00', '2026-06-09T21:00:00', 9, 9),
(N'Ensaio 10', N'Ensaio geral da banda numero 10.', '2026-06-10T19:00:00', '2026-06-10T21:00:00', 10, 10),
(N'Show 1', N'Apresentacao da banda numero 1.', '2026-07-01T20:00:00', '2026-07-01T22:30:00', 1, 1),
(N'Show 2', N'Apresentacao da banda numero 2.', '2026-07-02T20:00:00', '2026-07-02T22:30:00', 2, 2),
(N'Show 3', N'Apresentacao da banda numero 3.', '2026-07-03T20:00:00', '2026-07-03T22:30:00', 3, 3),
(N'Show 4', N'Apresentacao da banda numero 4.', '2026-07-04T20:00:00', '2026-07-04T22:30:00', 4, 4),
(N'Show 5', N'Apresentacao da banda numero 5.', '2026-07-05T20:00:00', '2026-07-05T22:30:00', 5, 5),
(N'Show 6', N'Apresentacao da banda numero 6.', '2026-07-06T20:00:00', '2026-07-06T22:30:00', 6, 6),
(N'Show 7', N'Apresentacao da banda numero 7.', '2026-07-07T20:00:00', '2026-07-07T22:30:00', 7, 7),
(N'Show 8', N'Apresentacao da banda numero 8.', '2026-07-08T20:00:00', '2026-07-08T22:30:00', 8, 8),
(N'Show 9', N'Apresentacao da banda numero 9.', '2026-07-09T20:00:00', '2026-07-09T22:30:00', 9, 9),
(N'Show 10', N'Apresentacao da banda numero 10.', '2026-07-10T20:00:00', '2026-07-10T22:30:00', 10, 10),
(N'Reuniao 1', N'Reuniao de planejamento numero 1.', '2026-08-01T18:30:00', '2026-08-01T19:30:00', 1, 1),
(N'Reuniao 2', N'Reuniao de planejamento numero 2.', '2026-08-02T18:30:00', '2026-08-02T19:30:00', 2, 2),
(N'Reuniao 3', N'Reuniao de planejamento numero 3.', '2026-08-03T18:30:00', '2026-08-03T19:30:00', 3, 3),
(N'Reuniao 4', N'Reuniao de planejamento numero 4.', '2026-08-04T18:30:00', '2026-08-04T19:30:00', 4, 4),
(N'Reuniao 5', N'Reuniao de planejamento numero 5.', '2026-08-05T18:30:00', '2026-08-05T19:30:00', 5, 5),
(N'Reuniao 6', N'Reuniao de planejamento numero 6.', '2026-08-06T18:30:00', '2026-08-06T19:30:00', 6, 6),
(N'Reuniao 7', N'Reuniao de planejamento numero 7.', '2026-08-07T18:30:00', '2026-08-07T19:30:00', 7, 7),
(N'Reuniao 8', N'Reuniao de planejamento numero 8.', '2026-08-08T18:30:00', '2026-08-08T19:30:00', 8, 8),
(N'Reuniao 9', N'Reuniao de planejamento numero 9.', '2026-08-09T18:30:00', '2026-08-09T19:30:00', 9, 9),
(N'Reuniao 10', N'Reuniao de planejamento numero 10.', '2026-08-10T18:30:00', '2026-08-10T19:30:00', 10, 10);
GO

INSERT INTO ensaios (codigo_agenda, repertorio, observacao) VALUES
(1, N'Musica 1A, Musica 1B, Musica 1C', N'Ajustar dinamica do ensaio 1.'),
(2, N'Musica 2A, Musica 2B, Musica 2C', N'Ajustar dinamica do ensaio 2.'),
(3, N'Musica 3A, Musica 3B, Musica 3C', N'Ajustar dinamica do ensaio 3.'),
(4, N'Musica 4A, Musica 4B, Musica 4C', N'Ajustar dinamica do ensaio 4.'),
(5, N'Musica 5A, Musica 5B, Musica 5C', N'Ajustar dinamica do ensaio 5.'),
(6, N'Musica 6A, Musica 6B, Musica 6C', N'Ajustar dinamica do ensaio 6.'),
(7, N'Musica 7A, Musica 7B, Musica 7C', N'Ajustar dinamica do ensaio 7.'),
(8, N'Musica 8A, Musica 8B, Musica 8C', N'Ajustar dinamica do ensaio 8.'),
(9, N'Musica 9A, Musica 9B, Musica 9C', N'Ajustar dinamica do ensaio 9.'),
(10, N'Musica 10A, Musica 10B, Musica 10C', N'Ajustar dinamica do ensaio 10.');
GO

INSERT INTO shows (codigo_agenda, codigo_cidade) VALUES
(11, 1),
(12, 2),
(13, 3),
(14, 4),
(15, 5),
(16, 6),
(17, 7),
(18, 8),
(19, 9),
(20, 10);
GO

INSERT INTO reunioes (codigo_agenda, pauta) VALUES
(21, N'Organizacao de repertorio, horarios, transporte e divulgacao do evento 1.'),
(22, N'Organizacao de repertorio, horarios, transporte e divulgacao do evento 2.'),
(23, N'Organizacao de repertorio, horarios, transporte e divulgacao do evento 3.'),
(24, N'Organizacao de repertorio, horarios, transporte e divulgacao do evento 4.'),
(25, N'Organizacao de repertorio, horarios, transporte e divulgacao do evento 5.'),
(26, N'Organizacao de repertorio, horarios, transporte e divulgacao do evento 6.'),
(27, N'Organizacao de repertorio, horarios, transporte e divulgacao do evento 7.'),
(28, N'Organizacao de repertorio, horarios, transporte e divulgacao do evento 8.'),
(29, N'Organizacao de repertorio, horarios, transporte e divulgacao do evento 9.'),
(30, N'Organizacao de repertorio, horarios, transporte e divulgacao do evento 10.');
GO



UPDATE agenda SET codigo_tipo = 2 WHERE codigo_agenda BETWEEN 11 AND 20;
UPDATE agenda SET codigo_tipo = 3 WHERE codigo_agenda BETWEEN 21 AND 30;
GO

INSERT INTO participacao_evento (codigo_agenda, cod_musico, status_confirmacao, presente, data_resposta) VALUES
(1, 1, N'CONFIRMADO', 1, SYSDATETIME()), (2, 2, N'CONFIRMADO', 1, SYSDATETIME()),
(3, 3, N'CONFIRMADO', 1, SYSDATETIME()), (4, 4, N'CONFIRMADO', 1, SYSDATETIME()),
(5, 5, N'CONFIRMADO', 1, SYSDATETIME()), (6, 6, N'CONFIRMADO', 1, SYSDATETIME()),
(7, 7, N'CONFIRMADO', 1, SYSDATETIME()), (8, 8, N'CONFIRMADO', 1, SYSDATETIME()),
(9, 9, N'CONFIRMADO', 1, SYSDATETIME()), (10, 10, N'CONFIRMADO', 1, SYSDATETIME()),
(11, 1, N'PENDENTE', NULL, NULL), (12, 2, N'PENDENTE', NULL, NULL),
(13, 3, N'PENDENTE', NULL, NULL), (14, 4, N'PENDENTE', NULL, NULL),
(15, 5, N'PENDENTE', NULL, NULL), (16, 6, N'PENDENTE', NULL, NULL),
(17, 7, N'PENDENTE', NULL, NULL), (18, 8, N'PENDENTE', NULL, NULL),
(19, 9, N'PENDENTE', NULL, NULL), (20, 10, N'PENDENTE', NULL, NULL),
(21, 1, N'PENDENTE', NULL, NULL), (22, 2, N'PENDENTE', NULL, NULL),
(23, 3, N'PENDENTE', NULL, NULL), (24, 4, N'PENDENTE', NULL, NULL),
(25, 5, N'PENDENTE', NULL, NULL), (26, 6, N'PENDENTE', NULL, NULL),
(27, 7, N'PENDENTE', NULL, NULL), (28, 8, N'PENDENTE', NULL, NULL),
(29, 9, N'PENDENTE', NULL, NULL), (30, 10, N'PENDENTE', NULL, NULL);
GO
