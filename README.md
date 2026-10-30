# AgendaBanda

Aplicação web para organizar ensaios, shows e reuniões de uma banda. Reúne calendário, músicos participantes, confirmações, presenças e administração de contas e locais, com persistência em SQL Server.

Desenvolvida como Mínimo Produto Viável (MPV) para o Projeto de Extensão Comunitária do 4º semestre.

**Repositório:** [Sollo99/AgendaBandaV2](https://github.com/Sollo99/AgendaBandaV2)

## Sumário

- [Comece aqui](#comece-aqui)
- [Funcionalidades e perfis](#funcionalidades-e-perfis)
- [Documentação](#documentação)
- [DER (Diagrama Entidade-Relacionamento)](BANCO_DE_DADOS.md#der)
- [Dicionário de dados](DICIONARIO_DE_DADOS.md)
- [Recursos SQL e auditoria](RECURSOS_SQL.md)
- [Equipe e licença](#equipe-e-licença)

## Comece aqui

O projeto utiliza **Java 17 e Maven 3.9**, **React com TypeScript**, **Node.js 22 ou superior** e **SQL Server 2016 SP1 ou superior**, com compatibilidade 130 ou superior.

1. Siga o [guia de instalação](INSTALACAO.md) para preparar as ferramentas e executar os sete scripts SQL na ordem indicada.
2. Inicie o backend e o frontend conforme o guia. A interface local fica em [http://localhost:5173](http://localhost:5173).
3. Entre com uma [conta de demonstração](GUIA_DE_USO.md#contas-de-demonstração) e siga o [roteiro de avaliação](GUIA_DE_USO.md#roteiro-de-avaliação).

Use uma base dedicada: o script de estrutura remove os dados anteriores ao recriar as tabelas.

## Funcionalidades e perfis

| Área | Recursos |
| --- | --- |
| Agenda | Calendário, criação de eventos, detalhes, edição, cancelamento e exclusão conforme as permissões. |
| Eventos | Ensaios com repertório e observação, shows com cidade e reuniões com pauta. |
| Participações | Seleção de músicos em qualquer categoria de evento, confirmação de participação e registro administrativo de presença ou ausência. |
| Cadastros | Membros, cidades, locais e tipos de evento, com validação dos dados e dos relacionamentos. |
| Contas | Cadastro sujeito à aprovação, edição do próprio perfil, ativação e desativação de membros e recuperação de senha mediada por administrador. |
| Relatórios | Resumo por tipo de evento, presença dos músicos, utilização de locais e consulta de históricos, com filtro de período. |
| Integridade | Prevenção de conflitos de horário, preservação das respostas ao editar eventos e proteção de registros vinculados. |

Cada conta possui um perfil operacional: **membro**, **músico** ou **produtor**. A permissão de **administrador** é independente desse perfil.

- **Membro autenticado:** consulta a agenda, administra o próprio perfil e cria eventos. Pode editar ou excluir os eventos que criou, respeitando as regras do sistema.
- **Músico:** também responde às próprias participações em ensaios, shows e reuniões.
- **Produtor:** possui cadastro de especialidade e utiliza os recursos comuns aos membros.
- **Administrador:** gerencia usuários e cadastros, analisa solicitações, administra eventos, registra presenças e acessa relatórios e históricos.

## Documentação

| Documento | Conteúdo |
| --- | --- |
| [Instalação e execução](INSTALACAO.md) | Ferramentas, preparação do SQL Server, configuração, comandos de execução e solução de problemas. |
| [Guia de uso](GUIA_DE_USO.md) | Contas de demonstração, cadastro, recuperação de senha e roteiro de avaliação. |
| [Regras de negócio](REGRAS_DE_NEGOCIO.md) | Permissões, conflitos de horário, participações, presenças e preservação dos vínculos. |
| [Banco de dados e DER](BANCO_DE_DADOS.md) | Arquitetura, responsabilidades das entidades e diagrama das 15 tabelas. |
| [Dicionário de dados](DICIONARIO_DE_DADOS.md) | Tipos, limites, valores padrão, chaves, índices e restrições dos 90 campos. |
| [Recursos SQL e auditoria](RECURSOS_SQL.md) | As 13 procedures, cinco views, três funções e nove triggers utilizadas pela aplicação. |
| [Configuração e segurança](SEGURANCA.md) | Sessões, cookies, CSRF, aprovação de contas, limites de tentativas e retenção de históricos. |
| [API](API.md) | Rotas, permissões, paginação, formatos de data e respostas de erro. |

Os fontes estão em [backend](backend) e [web](web). Os scripts oficiais para SQL Server estão em [database](database).

## Equipe e licença

**Equipe: System of a dev**

- Eduardo R. R. N.: qualidade e documentação.
- Lorena A. T.: gerenciamento do projeto.
- Paulo S. S.: desenvolvimento.
- Roberto B. F.: documentação e qualidade.

Licença GNU GPL v3. Consulte o arquivo [LICENSE](LICENSE).
