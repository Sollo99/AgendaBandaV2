# Agenda da Banda

Aplicação web para organizar os compromissos de uma banda, reunir músicos em ensaios, shows e reuniões e acompanhar confirmações e presenças. O sistema centraliza a agenda, os locais dos eventos e a administração dos membros, com controle de acesso e persistência em SQL Server.

Desenvolvido como Mínimo Produto Viável (MPV) para o Projeto de Extensão Comunitária do 4º semestre.

**Repositório:** [Sollo99/AgendaBandaV2](https://github.com/Sollo99/AgendaBandaV2)

## Sumário

- [Funcionalidades e perfis](#funcionalidades-e-perfis)
- [Tecnologias e requisitos](#tecnologias-e-requisitos)
- [Instalação e execução](#instalação-e-execução)
- [Acesso e uso](#acesso-e-uso)
- [Roteiro de avaliação](#roteiro-de-avaliação)
- [Regras de negócio](#regras-de-negócio)
- [Banco de dados e arquitetura](#banco-de-dados-e-arquitetura)
- [Dicionário de dados](#dicionário-de-dados)
- [Configuração e segurança](#configuração-e-segurança)
- [API](#api)
- [Solução de problemas](#solução-de-problemas)
- [Equipe e licença](#equipe-e-licença)

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

## Tecnologias e requisitos

| Componente | Tecnologia | Requisito para execução |
| --- | --- | --- |
| Backend | Java, Spring Boot, Spring Data JPA e JDBC | JDK 17 e Maven 3.9. |
| Frontend | React, TypeScript, Vite e Chakra UI | Node.js 22 ou superior e npm. |
| Banco de dados | Microsoft SQL Server | SQL Server 2016 SP1 ou superior; a edição Express pode ser utilizada. |
| Administração do banco | Scripts T-SQL | SQL Server Management Studio ou outro cliente compatível com separadores `GO`. |

O banco precisa de **nível de compatibilidade 130 ou superior**. Java, Maven, Node.js e npm devem estar disponíveis no terminal.

## Instalação e execução

### Obter o projeto

Clone o repositório e entre na pasta criada:

```powershell
git clone https://github.com/Sollo99/AgendaBandaV2.git AgendaBanda
cd AgendaBanda
```

Se o projeto já foi baixado, abra o terminal na pasta que contém `backend`, `database` e `web`.

### Preparar o SQL Server

1. Inicie a instância do SQL Server e conecte-se a ela pelo SQL Server Management Studio.
2. Para a conexão usada pela aplicação, habilite a autenticação do SQL Server junto à autenticação do Windows e o protocolo TCP/IP. Os valores padrão do projeto usam `localhost:1433`; se a instância utilizar outro endereço ou porta, ajuste a configuração do backend.
3. Conecte-se com uma conta que tenha permissão para criar o banco e o login da aplicação.
4. Execute os arquivos de [database](database) inteiros, uma única vez e na ordem abaixo.

| Ordem | Script | Finalidade |
| --- | --- | --- |
| 1 | [00_create_login.sql](database/00_create_login.sql) | Cria o banco `AgendaBanda`, o login `agenda_user` e suas permissões. |
| 2 | [01_create_schema.sql](database/01_create_schema.sql) | Cria tabelas, relacionamentos, restrições e índices. |
| 3 | [02_insert_data.sql](database/02_insert_data.sql) | Insere as contas e os dados de demonstração. |
| 4 | [03_functions.sql](database/03_functions.sql) | Cria as funções de presença, conflitos e resumo por período. |
| 5 | [04_views.sql](database/04_views.sql) | Cria as consultas de agenda, presenças e ocupação dos locais. |
| 6 | [05_procedures.sql](database/05_procedures.sql) | Cria as operações transacionais de eventos e participações. |
| 7 | [06_triggers.sql](database/06_triggers.sql) | Cria os registros automáticos de auditoria. |

**Use uma base dedicada ao projeto:** o script `01_create_schema.sql` recria os objetos e remove os dados anteriores. O backend utiliza a estrutura preparada pelos scripts; sua inicialização não cria as tabelas.

Ao alterar autenticação ou TCP/IP na configuração do SQL Server, reinicie o serviço da instância.

### Iniciar o backend

Em um terminal na raiz do projeto:

```powershell
cd backend
Copy-Item .env.example .env
```

Abra `backend/.env` e confira os dados da conexão. A configuração padrão corresponde ao login criado pelo primeiro script:

```dotenv
DB_HOST=localhost
DB_PORT=1433
DB_NAME=AgendaBanda
DB_USER=agenda_user
DB_PASSWORD=agenda123
SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Para uma instância nomeada ou uma conexão personalizada, informe a URL JDBC completa em `DB_URL`:

```dotenv
DB_URL=jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=AgendaBanda;encrypt=true;trustServerCertificate=true
DB_USER=agenda_user
DB_PASSWORD=agenda123
```

Em seguida, dentro de `backend`:

```powershell
mvn spring-boot:run
```

Mantenha o terminal aberto. A API estará disponível em `http://localhost:8080/api`.

### Iniciar o frontend

Abra **outro terminal** na raiz do projeto:

```powershell
cd web
npm ci --include=optional
npm run dev
```

Acesse **[http://localhost:5173](http://localhost:5173)**.

O frontend encaminha as chamadas de `/api` para o backend em `localhost:8080`. A configuração de referência está em [web/vite.config.ts](web/vite.config.ts). Use os endereços e as portas indicados para manter a conexão e os cookies consistentes.

Para gerar os arquivos estáticos da interface:

```powershell
npm run build
```

A saída será gravada em `web/dist`. As dependências devem ser instaladas no sistema em que o projeto será executado, usando o `package-lock.json`; `node_modules` não faz parte dos fontes.

## Acesso e uso

Após executar os scripts de preparação, estas contas estão disponíveis:

| Login | Senha | Acesso |
| --- | --- | --- |
| `admin` | `admin` | Administração e perfil de músico. |
| `usuario` | `usuario` | Músico sem permissão administrativa. |
| `teste` | `teste` | Músico sem permissão administrativa. |

Essas são **credenciais da aplicação**. O login `agenda_user` é exclusivo da conexão com o banco.

Na tela inicial, entre com uma das contas. A navegação apresenta **Agenda** e **Meu perfil**; administradores também acessam **Usuários**, **Locais**, **Cidades**, **Tipos de evento**, **Relatórios** e as solicitações de recuperação de senha.

### Cadastro de uma conta

1. Selecione **Criar conta**.
2. Informe os dados pessoais, as credenciais e o instrumento.
3. Entre como administrador e analise o pedido na tela **Usuários**.
4. Após a aprovação, o membro poderá entrar com seu login e senha.

O cadastro público cria um músico sem permissão administrativa. Contas pendentes ou recusadas não acessam a agenda.

### Recuperação de senha

1. O membro seleciona **Esqueci minha senha** e informa o login ou e-mail.
2. O administrador recebe a solicitação e confirma a identidade do membro presencialmente ou por um contato previamente conhecido.
3. Ao aprovar, o sistema exibe um código uma única vez ao administrador, para entrega ao membro identificado.
4. O membro seleciona **Já tenho o código do administrador**, informa o código completo e define a nova senha.

O pedido pendente tem validade padrão de 24 horas. Depois da aprovação, o código vale por duas horas e pode ser usado uma única vez. O administrador pode cancelar pedidos abertos. O fluxo utiliza entrega manual do código, sem envio automático de e-mail.

## Roteiro de avaliação

1. **Acesse como administrador.** Conheça o calendário e abra os detalhes de um evento.
2. **Crie um compromisso.** Escolha uma data, um local, o tipo de evento e os músicos participantes. Preencha repertório, cidade ou pauta conforme a categoria.
3. **Confira a persistência.** Atualize a página e abra novamente o evento cadastrado.
4. **Acesse como músico.** Use uma das contas vinculadas ao evento e confirme ou recuse a participação antes de seu início.
5. **Edite o evento como administrador.** Altere o título ou outro campo e confira a preservação da resposta do músico mantido.
6. **Explore os cadastros.** Cadastre uma cidade e um local; consulte também a administração de usuários e tipos de evento.
7. **Consulte Relatórios.** Selecione o período desejado e observe o resumo dos eventos, as presenças, a utilização dos locais e o histórico das alterações feitas pela aplicação.

O registro de presença fica disponível ao administrador a partir do início de um evento não cancelado. Para avaliar essa função, selecione um compromisso que já tenha começado.

## Regras de negócio

### Eventos e participações

- Os horários da agenda seguem o fuso de São Paulo (`America/Sao_Paulo`).
- Eventos possuem os estados `AGENDADO`, `REALIZADO` e `CANCELADO`.
- O horário final deve ser posterior ao inicial. Um evento só pode ser marcado como realizado após o término.
- Eventos não cancelados reservam o local e os músicos vinculados. Sobreposições são rejeitadas; horários consecutivos são permitidos.
- Cada músico tem uma única participação por evento, independentemente de ser ensaio, show ou reunião.
- Ao editar um evento, participantes mantidos conservam confirmação, presença, observação e data de resposta. Novos participantes começam como `PENDENTE`.
- O músico pode responder somente pela própria participação, antes do início de um evento agendado.
- O administrador registra presença ou ausência após o início. Presença positiva exige confirmação.
- Eventos realizados não podem ser reabertos ou excluídos. Registros de presença ou ausência também impedem a exclusão do evento e a remoção do participante correspondente.
- Um evento com presença registrada não pode ser cancelado ou reagendado para o futuro.

### Membros e cadastros

- Cada membro possui um único perfil operacional; a permissão administrativa é concedida separadamente.
- A desativação de um membro preserva seus vínculos. O último administrador ativo não pode ser desativado nem perder sua permissão.
- Músicos inativos não ficam disponíveis para novas inclusões em eventos; suas participações existentes são preservadas.
- Locais vinculados a eventos não podem ser excluídos.
- Cidades são únicas pela combinação de nome e UF, sem distinção entre maiúsculas e minúsculas.
- CPF, e-mail, login, senhas e limites de tamanho são validados. Senhas novas exigem ao menos seis caracteres e no máximo 72 bytes em UTF-8.
- Um administrador pode liberar CPF, e-mail e login de um cadastro recusado para um novo pedido, preservando o registro da decisão.

## Banco de dados e arquitetura

```text
AgendaBanda/
├── backend/
│   ├── src/main/java/br/com/fatec/agendabanda/
│   │   ├── config/        Configuração da aplicação
│   │   ├── controller/    API REST
│   │   ├── dto/           Entradas e respostas da API
│   │   ├── model/         Entidades persistentes
│   │   ├── repository/    Acesso a dados por JPA e JDBC
│   │   ├── security/      Sessões e proteção das requisições
│   │   ├── service/       Regras e operações da aplicação
│   │   └── validation/    Validações dos dados de entrada
│   └── src/main/resources/
├── database/             Scripts SQL Server
├── web/                  Interface React
└── README.md
```

O frontend consome a API REST. No backend, os serviços coordenam as operações e as transações. Os cadastros usam Spring Data JPA; a agenda, as participações e os relatórios também utilizam JDBC para executar os objetos SQL. Os scripts estão em `database` e possuem cópias em `backend/src/main/resources/db/scripts`.

### Modelo de dados

| Entidades | Responsabilidade |
| --- | --- |
| `membros`, `musicos` e `produtores` | Identificação, acesso e perfil operacional. |
| `cidades` e `locais` | Cadastro dos municípios e dos espaços dos eventos. |
| `tipos_evento` e `agenda` | Classificação, período, situação, local e criador de cada evento. |
| `ensaios`, `shows` e `reunioes` | Detalhes específicos da categoria do evento. |
| `participacao_evento` | Vínculo único entre músico e evento, com confirmação e presença. |
| `redefinicoes_senha` e `sessoes` | Solicitações de recuperação e sessões autenticadas. |
| `historico_eventos` e `historico_operacoes` | Auditoria de eventos, participações e campos administrativos dos membros. |

A definição completa dos campos, índices e restrições está em [01_create_schema.sql](database/01_create_schema.sql). As entidades Java estão em [model](backend/src/main/java/br/com/fatec/agendabanda/model).

<details>
<summary>Diagrama de entidade-relacionamento</summary>

O diagrama apresenta os relacionamentos e os principais atributos persistidos.

```mermaid
erDiagram
    MEMBROS ||--o| MUSICOS : "perfil musical"
    MEMBROS ||--o| PRODUTORES : "perfil de producao"
    MEMBROS ||--o{ AGENDA : cria
    CIDADES ||--o{ LOCAIS : possui
    CIDADES ||--o{ SHOWS : recebe
    LOCAIS ||--o{ AGENDA : sedia
    TIPOS_EVENTO ||--o{ AGENDA : classifica
    AGENDA ||--o| ENSAIOS : detalha
    AGENDA ||--o| SHOWS : detalha
    AGENDA ||--o| REUNIOES : detalha
    AGENDA ||--o{ PARTICIPACAO_EVENTO : possui
    MUSICOS ||--o{ PARTICIPACAO_EVENTO : participa
    MEMBROS ||--o{ REDEFINICOES_SENHA : solicita
    MEMBROS o|--o{ REDEFINICOES_SENHA : decide
    MEMBROS ||--o{ SESSOES : autentica
    MEMBROS o|--o{ HISTORICO_EVENTOS : responsavel
    MEMBROS o|--o{ HISTORICO_OPERACOES : responsavel
    MEMBROS o|--o{ MEMBROS : "analisa ou libera cadastro"

    MEMBROS {
        int codigo PK
        string nome
        string cpf UK
        string email UK
        string login UK
        string senha
        string perfil
        boolean administrador
        boolean ativo
        string status_cadastro
    }
    MUSICOS {
        int codigo PK, FK
        string perfil FK
        string instrumento
        string biografia
    }
    PRODUTORES {
        int codigo PK, FK
        string perfil FK
        string especialidade
    }
    CIDADES {
        int codigo_cidade PK
        string nome
        string uf
    }
    LOCAIS {
        int codigo_local PK
        string nome
        string endereco
        string complemento
        int codigo_cidade FK
    }
    TIPOS_EVENTO {
        int codigo_tipo PK
        string nome UK
        string descricao
        string categoria_detalhe
        boolean ativo
    }
    AGENDA {
        int codigo_agenda PK
        string titulo
        string descricao
        datetime dt_hora_in
        datetime dt_hora_fim
        int codigo_local FK
        int cod_membro_criador FK
        int codigo_tipo FK
        string status
    }
    ENSAIOS {
        int codigo_agenda PK, FK
        string repertorio
        string observacao
    }
    SHOWS {
        int codigo_agenda PK, FK
        int codigo_cidade FK
    }
    REUNIOES {
        int codigo_agenda PK, FK
        string pauta
    }
    PARTICIPACAO_EVENTO {
        int codigo_agenda PK, FK
        int cod_musico PK, FK
        string status_confirmacao
        boolean presente
        datetime data_resposta
        string observacao
    }
    REDEFINICOES_SENHA {
        int codigo_redefinicao PK
        int codigo_membro FK
        int codigo_administrador FK
        string status
        string chave_hash
        datetime solicitado_em
        datetime expira_em
        datetime encerrado_em
    }
    SESSOES {
        int codigo_sessao PK
        int codigo_membro FK
        string token_hash UK
        datetime criado_em
        datetime expira_em
    }
    HISTORICO_EVENTOS {
        bigint codigo_historico PK
        int codigo_agenda
        int membro_responsavel FK
        string tipo_operacao
        datetime data_hora
        string dados_anteriores
        string dados_novos
    }
    HISTORICO_OPERACOES {
        bigint codigo_operacao PK
        int membro_responsavel FK
        string entidade_afetada
        string codigo_registro_afetado
        string tipo_operacao
        datetime data_hora
        string dados_anteriores
        string dados_novos
    }
```

Os perfis de músico e produtor são exclusivos, protegidos por chaves estrangeiras compostas com o perfil do membro. A chave composta da participação impede duplicação do mesmo músico no evento.

O código do evento em `historico_eventos` é mantido sem chave estrangeira para a agenda, preservando o histórico quando um evento elegível é excluído. Os responsáveis pelos históricos são opcionais.

</details>

### Dicionário de dados

Os nomes, tipos, valores padrão, chaves e restrições abaixo correspondem ao [schema SQL Server](database/01_create_schema.sql). O dicionário abrange as 15 tabelas e seus 90 campos. As regras descritas como da aplicação ou das procedures complementam as restrições do schema.

**Convenções:**

- **PK:** chave primária. Quando indicada em mais de um campo da mesma tabela, forma uma chave composta.
- **FK:** chave estrangeira. Os campos de uma FK composta são validados em conjunto.
- **UK:** unicidade. Restrições compostas e índices únicos filtrados estão detalhados na respectiva tabela.
- **Nulo:** indica se o schema permite `NULL`; permissões condicionais, como a liberação dos identificadores de um cadastro recusado, também devem respeitar os `CHECKs`.
- **Padrão / geração:** `IDENTITY(1,1)` gera o identificador no banco. `Sem DEFAULT` indica ausência de uma cláusula `DEFAULT`; o valor é informado na gravação, ou pode ficar nulo quando permitido.
- **Tipos:** `BIT` representa 0 ou 1; `NVARCHAR` armazena texto Unicode; `CHAR` possui comprimento fixo. `DATETIME2` não armazena fuso; a aplicação utiliza o horário de São Paulo e o padrão SQL `SYSDATETIME()` utiliza o relógio do servidor de banco.
- **Exclusão:** as FKs sem `ON DELETE CASCADE` impedem excluir um registro pai enquanto houver referências. As cascatas declaradas estão indicadas em cada relacionamento.

<details>
<summary><strong>membros</strong>: Dados de identificação, acesso, perfil e aprovação das contas.</summary>

| Campo | Tipo SQL Server | Nulo | Chave | Padrão / geração | Descrição |
| --- | --- | --- | --- | --- | --- |
| `codigo` | `INT` | Não | PK; UK composta | `IDENTITY(1,1)` | Identificador do membro. |
| `nome` | `NVARCHAR(100)` | Não | Nenhuma | Sem DEFAULT | Nome completo do membro. |
| `cpf` | `NVARCHAR(14)` | Sim | UK filtrada | Sem DEFAULT | CPF normalizado com 11 dígitos; a coluna admite até 14 posições. |
| `email` | `NVARCHAR(120)` | Sim | UK filtrada | Sem DEFAULT | Endereço de e-mail do membro. |
| `login` | `NVARCHAR(50)` | Sim | UK filtrada | Sem DEFAULT | Identificador utilizado para entrar na aplicação. |
| `senha` | `NVARCHAR(255)` | Não | Nenhuma | Sem DEFAULT | Hash BCrypt da senha. |
| `administrador` | `BIT` | Não | Nenhuma | `0` | Permissão administrativa: 1 concede acesso administrativo; 0 mantém o acesso comum. |
| `ativo` | `BIT` | Não | Nenhuma | `1` | Indica se a conta está habilitada para acesso. |
| `perfil` | `NVARCHAR(10)` | Não | UK composta | `N'MEMBRO'` | Perfil operacional do membro. |
| `status_cadastro` | `NVARCHAR(10)` | Não | Nenhuma | `N'APROVADO'` | Situação da análise do cadastro. |
| `cadastro_decidido_em` | `DATETIME2` | Sim | Nenhuma | Sem DEFAULT | Data e hora da decisão administrativa sobre o cadastro. |
| `cadastro_decidido_por` | `INT` | Sim | FK | Sem DEFAULT | Membro responsável pela decisão do cadastro. |
| `identificadores_liberados_em` | `DATETIME2` | Sim | Nenhuma | Sem DEFAULT | Data e hora da liberação de CPF, e-mail e login de um cadastro recusado. |
| `identificadores_liberados_por` | `INT` | Sim | FK | Sem DEFAULT | Administrador responsável pela liberação dos identificadores. |
| `criado_em` | `DATETIME2` | Não | Nenhuma | `SYSDATETIME()` | Data e hora de criação do cadastro. |

**Relacionamentos:**

- `fk_membros_cadastro_administrador`: `(cadastro_decidido_por)` referencia `membros(codigo)`. Sem exclusão em cascata.
- `fk_membros_liberacao_administrador`: `(identificadores_liberados_por)` referencia `membros(codigo)`. Sem exclusão em cascata.

**Restrições do banco:**

- `ck_membros_perfil`: `perfil` aceita `MEMBRO`, `MUSICO` ou `PRODUTOR`.
- `ck_membros_cadastro`: `status_cadastro` aceita `PENDENTE`, `APROVADO` ou `RECUSADO`.
- `ck_membros_cadastro_acesso`: um cadastro não aprovado deve ter `ativo = 0` e `administrador = 0`.
- `ck_membros_identificadores`: CPF, e-mail e login ficam preenchidos juntos, sem dados de liberação; ou ficam todos nulos em cadastro `RECUSADO`, com data e responsável pela liberação preenchidos.
- `ck_membros_cpf`: quando preenchido, o CPF deve ter 11 dígitos numéricos. A validação dos dígitos verificadores e do formato do e-mail é feita pela aplicação.
- `uq_membros_codigo_perfil`: unicidade composta de `(codigo, perfil)`, utilizada pelos vínculos dos perfis de músico e produtor.

**Índices adicionais:**

- `ux_membros_cpf`: índice único em `cpf`, filtrado por `cpf IS NOT NULL`.
- `ux_membros_email`: índice único em `email`, filtrado por `email IS NOT NULL`.
- `ux_membros_login`: índice único em `login`, filtrado por `login IS NOT NULL`.

**Regras da aplicação e das procedures:** O cadastro público define `PENDENTE`, `ativo = 0` e `administrador = 0`, substituindo os padrões do schema. A aplicação protege o último administrador ativo e utiliza desativação para preservar os vínculos.

</details>

<details>
<summary><strong>musicos</strong>: Informações específicas dos membros com perfil musical.</summary>

| Campo | Tipo SQL Server | Nulo | Chave | Padrão / geração | Descrição |
| --- | --- | --- | --- | --- | --- |
| `codigo` | `INT` | Não | PK; FK composta | Sem DEFAULT | Código do membro que possui o perfil musical; também identifica o músico. |
| `perfil` | `NVARCHAR(10)` | Não | FK composta | `N'MUSICO'` | Discriminador do perfil musical, usado na referência composta ao membro. |
| `instrumento` | `NVARCHAR(80)` | Não | Nenhuma | Sem DEFAULT | Instrumento ou função musical. |
| `biografia` | `NVARCHAR(500)` | Sim | Nenhuma | Sem DEFAULT | Apresentação ou informações complementares do músico. |

**Relacionamentos:**

- `fk_musicos_membros`: `(codigo, perfil)` referencia `membros(codigo, perfil)`. Sem exclusão em cascata.

**Restrições do banco:**

- `ck_musicos_perfil`: o discriminador deve ser `MUSICO`.
- O código é compartilhado com `membros`; a referência composta `(codigo, perfil)` exige correspondência com o perfil do membro.

</details>

<details>
<summary><strong>produtores</strong>: Informações específicas dos membros com perfil de produção.</summary>

| Campo | Tipo SQL Server | Nulo | Chave | Padrão / geração | Descrição |
| --- | --- | --- | --- | --- | --- |
| `codigo` | `INT` | Não | PK; FK composta | Sem DEFAULT | Código do membro que possui o perfil de produtor; também identifica o produtor. |
| `perfil` | `NVARCHAR(10)` | Não | FK composta | `N'PRODUTOR'` | Discriminador do perfil de produtor, usado na referência composta ao membro. |
| `especialidade` | `NVARCHAR(100)` | Não | Nenhuma | Sem DEFAULT | Área de atuação na produção. |

**Relacionamentos:**

- `fk_produtores_membros`: `(codigo, perfil)` referencia `membros(codigo, perfil)`. Sem exclusão em cascata.

**Restrições do banco:**

- `ck_produtores_perfil`: o discriminador deve ser `PRODUTOR`.
- O código é compartilhado com `membros`; a referência composta `(codigo, perfil)` exige correspondência com o perfil do membro.

</details>

<details>
<summary><strong>cidades</strong>: Municípios utilizados pelos locais e pelos shows.</summary>

| Campo | Tipo SQL Server | Nulo | Chave | Padrão / geração | Descrição |
| --- | --- | --- | --- | --- | --- |
| `codigo_cidade` | `INT` | Não | PK | `IDENTITY(1,1)` | Identificador da cidade. |
| `nome` | `NVARCHAR(100)` | Não | UK composta | Sem DEFAULT | Nome do município. |
| `uf` | `CHAR(2)` | Não | UK composta | Sem DEFAULT | Sigla da unidade federativa. |

**Restrições do banco:**

- `uq_cidades_nome_uf`: unicidade composta de `(nome, uf)`.
- `nome` e `uf` usam a collation `Latin1_General_100_CI_AS`, com comparação sem distinção de maiúsculas e minúsculas.
- A validação da sigla de UF brasileira é feita pela aplicação.

</details>

<details>
<summary><strong>locais</strong>: Espaços disponíveis para os compromissos da banda.</summary>

| Campo | Tipo SQL Server | Nulo | Chave | Padrão / geração | Descrição |
| --- | --- | --- | --- | --- | --- |
| `codigo_local` | `INT` | Não | PK | `IDENTITY(1,1)` | Identificador do local. |
| `nome` | `NVARCHAR(120)` | Não | Nenhuma | Sem DEFAULT | Nome do espaço ou estabelecimento. |
| `endereco` | `NVARCHAR(180)` | Não | Nenhuma | Sem DEFAULT | Endereço do local. |
| `complemento` | `NVARCHAR(120)` | Sim | Nenhuma | Sem DEFAULT | Informação adicional do endereço. |
| `codigo_cidade` | `INT` | Não | FK | Sem DEFAULT | Cidade à qual o local pertence. |

**Relacionamentos:**

- `fk_locais_cidades`: `(codigo_cidade)` referencia `cidades(codigo_cidade)`. Sem exclusão em cascata.

**Restrições do banco:**

- O vínculo com uma cidade é obrigatório.

**Regras da aplicação e das procedures:** A exclusão verifica a existência de eventos vinculados e adquire o lock lógico do local antes de efetuar a operação.

</details>

<details>
<summary><strong>tipos_evento</strong>: Classificação dos eventos e definição da categoria de detalhe.</summary>

| Campo | Tipo SQL Server | Nulo | Chave | Padrão / geração | Descrição |
| --- | --- | --- | --- | --- | --- |
| `codigo_tipo` | `INT` | Não | PK | `IDENTITY(1,1)` | Identificador do tipo de evento. |
| `nome` | `NVARCHAR(50)` | Não | UK | Sem DEFAULT | Nome apresentado para o tipo de evento. |
| `descricao` | `NVARCHAR(300)` | Sim | Nenhuma | Sem DEFAULT | Descrição do tipo. |
| `categoria_detalhe` | `NVARCHAR(15)` | Não | Nenhuma | `'EVENTO'` | Categoria estável que determina o detalhe específico do evento. |
| `ativo` | `BIT` | Não | Nenhuma | `1` | Indica se o tipo pode ser selecionado para novos eventos. |

**Restrições do banco:**

- O nome do tipo é único.
- `ck_tipos_evento_categoria`: `categoria_detalhe` aceita `EVENTO`, `ENSAIO`, `SHOW` ou `REUNIAO`.

**Regras da aplicação e das procedures:** As procedures de eventos selecionam tipos ativos e usam a categoria estável para gravar o detalhe de ensaio, show ou reunião.

</details>

<details>
<summary><strong>agenda</strong>: Dados comuns a todos os eventos.</summary>

| Campo | Tipo SQL Server | Nulo | Chave | Padrão / geração | Descrição |
| --- | --- | --- | --- | --- | --- |
| `codigo_agenda` | `INT` | Não | PK | `IDENTITY(1,1)` | Identificador do evento. |
| `titulo` | `NVARCHAR(120)` | Não | Nenhuma | Sem DEFAULT | Título do compromisso. |
| `descricao` | `NVARCHAR(800)` | Sim | Nenhuma | Sem DEFAULT | Descrição geral do evento. |
| `dt_hora_in` | `DATETIME2` | Não | Nenhuma | Sem DEFAULT | Data e hora de início. |
| `dt_hora_fim` | `DATETIME2` | Não | Nenhuma | Sem DEFAULT | Data e hora de término. |
| `codigo_local` | `INT` | Não | FK | Sem DEFAULT | Local reservado para o evento. |
| `cod_membro_criador` | `INT` | Não | FK | Sem DEFAULT | Membro que criou o evento. |
| `codigo_tipo` | `INT` | Não | FK | `1` | Tipo que classifica o evento. |
| `status` | `NVARCHAR(20)` | Não | Nenhuma | `N'AGENDADO'` | Situação do evento. |
| `criado_em` | `DATETIME2` | Não | Nenhuma | `SYSDATETIME()` | Data e hora de criação do evento. |
| `atualizado_em` | `DATETIME2` | Sim | Nenhuma | Sem DEFAULT | Data e hora da atualização do evento. |

**Relacionamentos:**

- `fk_agenda_local`: `(codigo_local)` referencia `locais(codigo_local)`. Sem exclusão em cascata.
- `fk_agenda_membro_criador`: `(cod_membro_criador)` referencia `membros(codigo)`. Sem exclusão em cascata.
- `fk_agenda_tipo`: `(codigo_tipo)` referencia `tipos_evento(codigo_tipo)`. Sem exclusão em cascata.

**Restrições do banco:**

- `ck_agenda_periodo`: `dt_hora_fim` deve ser maior que `dt_hora_in`.
- `ck_agenda_status`: aceita `AGENDADO`, `CANCELADO` ou `REALIZADO`.
- O valor padrão `1` de `codigo_tipo` exige que esse identificador exista em `tipos_evento`.

**Índices adicionais:**

- `ix_agenda_periodo`: `(dt_hora_in, dt_hora_fim) INCLUDE (codigo_local, status)`.

**Regras da aplicação e das procedures:** As procedures verificam permissões, atividade dos participantes e conflitos por local ou músico. `REALIZADO` exige término já ocorrido; presença registrada impede cancelamento ou reagendamento para o futuro. A exclusão protege eventos realizados ou com presença/ausência.

</details>

<details>
<summary><strong>ensaios</strong>: Detalhes dos eventos classificados como ensaio.</summary>

| Campo | Tipo SQL Server | Nulo | Chave | Padrão / geração | Descrição |
| --- | --- | --- | --- | --- | --- |
| `codigo_agenda` | `INT` | Não | PK; FK | Sem DEFAULT | Evento ao qual o detalhe do ensaio pertence; reutiliza o código da agenda. |
| `repertorio` | `NVARCHAR(800)` | Não | Nenhuma | Sem DEFAULT | Repertório previsto para o ensaio. |
| `observacao` | `NVARCHAR(500)` | Sim | Nenhuma | Sem DEFAULT | Observação específica do ensaio. |

**Relacionamentos:**

- `fk_ensaios_agenda`: `(codigo_agenda)` referencia `agenda(codigo_agenda)`. Exclusão do registro pai com `ON DELETE CASCADE`.

**Restrições do banco:**

- A chave primária compartilhada permite no máximo um detalhe de ensaio para cada código de evento.
- `repertorio` é obrigatório e não possui `DEFAULT` no schema.

**Regras da aplicação e das procedures:** A procedure usa `Repertorio nao informado` quando o repertório recebido está vazio ou ausente; esse preenchimento não é um `DEFAULT` SQL.

</details>

<details>
<summary><strong>shows</strong>: Detalhes dos eventos classificados como show.</summary>

| Campo | Tipo SQL Server | Nulo | Chave | Padrão / geração | Descrição |
| --- | --- | --- | --- | --- | --- |
| `codigo_agenda` | `INT` | Não | PK; FK | Sem DEFAULT | Evento ao qual o detalhe do show pertence; reutiliza o código da agenda. |
| `codigo_cidade` | `INT` | Não | FK | Sem DEFAULT | Cidade associada ao show. |

**Relacionamentos:**

- `fk_shows_agenda`: `(codigo_agenda)` referencia `agenda(codigo_agenda)`. Exclusão do registro pai com `ON DELETE CASCADE`.
- `fk_shows_cidade`: `(codigo_cidade)` referencia `cidades(codigo_cidade)`. Sem exclusão em cascata.

**Restrições do banco:**

- A chave primária compartilhada permite no máximo um detalhe de show para cada código de evento.
- A cidade do show é um vínculo próprio; o schema não exige igualdade com a cidade do local.

</details>

<details>
<summary><strong>reunioes</strong>: Detalhes dos eventos classificados como reunião.</summary>

| Campo | Tipo SQL Server | Nulo | Chave | Padrão / geração | Descrição |
| --- | --- | --- | --- | --- | --- |
| `codigo_agenda` | `INT` | Não | PK; FK | Sem DEFAULT | Evento ao qual o detalhe da reunião pertence; reutiliza o código da agenda. |
| `pauta` | `NVARCHAR(800)` | Não | Nenhuma | Sem DEFAULT | Assuntos previstos para a reunião. |

**Relacionamentos:**

- `fk_reunioes_agenda`: `(codigo_agenda)` referencia `agenda(codigo_agenda)`. Exclusão do registro pai com `ON DELETE CASCADE`.

**Restrições do banco:**

- A chave primária compartilhada permite no máximo um detalhe de reunião para cada código de evento.
- `pauta` é obrigatória e não possui `DEFAULT` no schema.

**Regras da aplicação e das procedures:** A procedure usa `Pauta nao informada` quando a pauta recebida está vazia ou ausente; esse preenchimento não é um `DEFAULT` SQL.

</details>

<details>
<summary><strong>participacao_evento</strong>: Relacionamento entre músicos e eventos, com confirmação e presença.</summary>

| Campo | Tipo SQL Server | Nulo | Chave | Padrão / geração | Descrição |
| --- | --- | --- | --- | --- | --- |
| `codigo_agenda` | `INT` | Não | PK; FK | Sem DEFAULT | Evento do qual o músico participa. |
| `cod_musico` | `INT` | Não | PK; FK | Sem DEFAULT | Músico vinculado ao evento. |
| `status_confirmacao` | `NVARCHAR(20)` | Não | Nenhuma | `N'PENDENTE'` | Resposta do participante ao convite. |
| `presente` | `BIT` | Sim | Nenhuma | Sem DEFAULT | Registro de presença: NULL significa não lançado, 0 significa ausência e 1 significa presença. |
| `data_resposta` | `DATETIME2` | Sim | Nenhuma | Sem DEFAULT | Data e hora do lançamento ou da alteração da confirmação. |
| `observacao` | `NVARCHAR(500)` | Sim | Nenhuma | Sem DEFAULT | Observação da participação; seu texto não é copiado pelo trigger de auditoria. |

**Relacionamentos:**

- `fk_participacao_evento_agenda`: `(codigo_agenda)` referencia `agenda(codigo_agenda)`. Exclusão do registro pai com `ON DELETE CASCADE`.
- `fk_participacao_evento_musico`: `(cod_musico)` referencia `musicos(codigo)`. Sem exclusão em cascata.

**Restrições do banco:**

- A chave primária composta `(codigo_agenda, cod_musico)` permite uma única participação do músico em cada evento.
- `ck_participacao_status`: aceita `PENDENTE`, `CONFIRMADO` ou `RECUSADO`.
- `ck_participacao_presenca_confirmada`: `presente = 1` exige `status_confirmacao = 'CONFIRMADO'`. Ausência e presença não lançada não exigem confirmação.

**Índices adicionais:**

- `ix_participacao_musico`: `(cod_musico, codigo_agenda)`.

**Regras da aplicação e das procedures:** A confirmação própria só é alterada antes do início de evento agendado. A presença administrativa exige evento iniciado e não cancelado. A edição da agenda preserva os dados dos músicos mantidos; novos vínculos começam pendentes.

</details>

<details>
<summary><strong>redefinicoes_senha</strong>: Controle da solicitação, aprovação, validade e conclusão da recuperação de senha.</summary>

| Campo | Tipo SQL Server | Nulo | Chave | Padrão / geração | Descrição |
| --- | --- | --- | --- | --- | --- |
| `codigo_redefinicao` | `INT` | Não | PK | `IDENTITY(1,1)` | Identificador da solicitação de recuperação. |
| `codigo_membro` | `INT` | Não | FK | Sem DEFAULT | Membro cuja senha será redefinida. |
| `status` | `NVARCHAR(15)` | Não | Nenhuma | `'PENDENTE'` | Estado do pedido de recuperação. |
| `chave_hash` | `CHAR(64)` | Não | Nenhuma | Sem DEFAULT | Hash SHA-256 hexadecimal da credencial de recuperação; não armazena o código em texto aberto. |
| `solicitado_em` | `DATETIME2` | Não | Nenhuma | `SYSDATETIME()` | Data e hora da abertura do pedido. |
| `expira_em` | `DATETIME2` | Não | Nenhuma | Sem DEFAULT | Prazo de validade do pedido ou da autorização aprovada. |
| `codigo_administrador` | `INT` | Sim | FK | Sem DEFAULT | Administrador associado à decisão de aprovar ou recusar. |
| `decidido_em` | `DATETIME2` | Sim | Nenhuma | Sem DEFAULT | Data e hora da decisão administrativa. |
| `concluido_em` | `DATETIME2` | Sim | Nenhuma | Sem DEFAULT | Data e hora em que a nova senha foi definida. |
| `encerrado_em` | `DATETIME2` | Sim | Nenhuma | Sem DEFAULT | Data e hora de encerramento do pedido. |
| `motivo_encerramento` | `NVARCHAR(200)` | Sim | Nenhuma | Sem DEFAULT | Motivo registrado para o encerramento. |

**Relacionamentos:**

- `fk_redefinicoes_senha_membro`: `(codigo_membro)` referencia `membros(codigo)`. Exclusão do registro pai com `ON DELETE CASCADE`.
- `fk_redefinicoes_senha_administrador`: `(codigo_administrador)` referencia `membros(codigo)`. Sem exclusão em cascata.

**Restrições do banco:**

- `ck_redefinicoes_senha_status`: aceita `PENDENTE`, `APROVADA`, `RECUSADA`, `CONCLUIDA`, `EXPIRADA` ou `CANCELADA`.
- `ck_redefinicoes_senha_prazo`: `expira_em` deve ser posterior a `solicitado_em`.
- `ck_redefinicoes_senha_encerramento`: pedidos `PENDENTE` ou `APROVADA` têm `encerrado_em` nulo; os demais estados exigem essa data preenchida.

**Índices adicionais:**

- `ux_redefinicoes_senha_aberta`: índice único em `codigo_membro`, filtrado por `status IN ('PENDENTE', 'APROVADA')`; garante uma única solicitação aberta por membro.
- `ix_redefinicoes_senha_status`: `(status, solicitado_em)`.
- `ix_redefinicoes_senha_expiracao`: `(expira_em) INCLUDE (codigo_membro, status)`.

**Regras da aplicação e das procedures:** A aprovação após verificação manual de identidade gera uma credencial nova no servidor e substitui seu hash. Prazos padrão: 24 horas para o pedido pendente e duas horas após a aprovação. Cancelamento, consumo e expiração encerram o pedido e liberam uma nova solicitação.

</details>

<details>
<summary><strong>sessoes</strong>: Sessões autenticadas persistidas no banco.</summary>

| Campo | Tipo SQL Server | Nulo | Chave | Padrão / geração | Descrição |
| --- | --- | --- | --- | --- | --- |
| `codigo_sessao` | `INT` | Não | PK | `IDENTITY(1,1)` | Identificador da sessão persistida. |
| `token_hash` | `CHAR(64)` | Não | UK | Sem DEFAULT | Hash SHA-256 hexadecimal do token de autenticação. |
| `codigo_membro` | `INT` | Não | FK | Sem DEFAULT | Membro autenticado pela sessão. |
| `criado_em` | `DATETIME2` | Não | Nenhuma | `SYSDATETIME()` | Data e hora de criação da sessão. |
| `expira_em` | `DATETIME2` | Não | Nenhuma | Sem DEFAULT | Data e hora a partir da qual a sessão deixa de autorizar acesso. |

**Relacionamentos:**

- `fk_sessoes_membro`: `(codigo_membro)` referencia `membros(codigo)`. Exclusão do registro pai com `ON DELETE CASCADE`.

**Restrições do banco:**

- `token_hash` é único.
- O schema exige `expira_em`, sem definir valor padrão nem um `CHECK` de comparação com `criado_em`.

**Índices adicionais:**

- `ix_sessoes_token_expiracao`: `(token_hash, expira_em)`.
- `ix_sessoes_expiracao`: `(expira_em)`.

**Regras da aplicação e das procedures:** O serviço define duração de oito horas, verifica a validade em cada autenticação e remove sessões vencidas periodicamente. Alterar a senha revoga as sessões do membro.

</details>

<details>
<summary><strong>historico_eventos</strong>: Auditoria das operações de negócio dos eventos.</summary>

| Campo | Tipo SQL Server | Nulo | Chave | Padrão / geração | Descrição |
| --- | --- | --- | --- | --- | --- |
| `codigo_historico` | `BIGINT` | Não | PK | `IDENTITY(1,1)` | Identificador do registro de auditoria do evento. |
| `codigo_agenda` | `INT` | Não | Nenhuma | Sem DEFAULT | Código original do evento, preservado sem FK para a agenda. |
| `membro_responsavel` | `INT` | Sim | FK | Sem DEFAULT | Membro identificado como responsável pela operação, quando disponível. |
| `tipo_operacao` | `NVARCHAR(20)` | Não | Nenhuma | Sem DEFAULT | Operação de negócio registrada sobre o evento. |
| `data_hora` | `DATETIME2` | Não | Nenhuma | Sem DEFAULT | Data e hora da operação registrada pelo trigger. |
| `dados_anteriores` | `NVARCHAR(MAX)` | Sim | Nenhuma | Sem DEFAULT | Representação JSON dos dados anteriores; pode ser nula na criação. |
| `dados_novos` | `NVARCHAR(MAX)` | Sim | Nenhuma | Sem DEFAULT | Representação JSON dos dados novos; pode ser nula na exclusão. |

**Relacionamentos:**

- `fk_historico_eventos_responsavel`: `(membro_responsavel)` referencia `membros(codigo)`. Sem exclusão em cascata.

**Restrições do banco:**

- `ck_historico_eventos_operacao`: aceita `CRIACAO`, `ALTERACAO`, `REAGENDAMENTO`, `CANCELAMENTO` ou `EXCLUSAO`.
- `codigo_agenda` é um identificador histórico sem FK, permitindo conservar o registro após a exclusão do evento.
- As colunas de dados armazenam texto JSON produzido pela aplicação e pelos triggers; o schema não declara `CHECK ISJSON`.

**Índices adicionais:**

- `ix_historico_eventos_data`: `(data_hora DESC, codigo_historico DESC)`.
- `ix_historico_eventos_agenda`: `(codigo_agenda, data_hora)`.

**Regras da aplicação e das procedures:** O trigger preenche a operação e a data; procedures e exclusão pelo repositório complementam os detalhes do evento. O responsável vem do contexto da sessão. A retenção padrão é de 365 dias.

</details>

<details>
<summary><strong>historico_operacoes</strong>: Auditoria das alterações selecionadas de membros e participações.</summary>

| Campo | Tipo SQL Server | Nulo | Chave | Padrão / geração | Descrição |
| --- | --- | --- | --- | --- | --- |
| `codigo_operacao` | `BIGINT` | Não | PK | `IDENTITY(1,1)` | Identificador do registro de auditoria geral. |
| `membro_responsavel` | `INT` | Sim | FK | Sem DEFAULT | Membro identificado como responsável pela operação, quando disponível. |
| `entidade_afetada` | `NVARCHAR(50)` | Não | Nenhuma | Sem DEFAULT | Identificação da entidade; os triggers atuais usam MEMBRO e PARTICIPACAO_EVENTO. |
| `codigo_registro_afetado` | `NVARCHAR(100)` | Não | Nenhuma | Sem DEFAULT | Identificador textual do registro, incluindo a combinação agenda:musico nas participações. |
| `tipo_operacao` | `NVARCHAR(10)` | Não | Nenhuma | Sem DEFAULT | Operação de persistência registrada. |
| `data_hora` | `DATETIME2` | Não | Nenhuma | Sem DEFAULT | Data e hora da operação registrada pelo trigger. |
| `dados_anteriores` | `NVARCHAR(MAX)` | Sim | Nenhuma | Sem DEFAULT | Representação JSON dos campos auditados antes da operação. |
| `dados_novos` | `NVARCHAR(MAX)` | Sim | Nenhuma | Sem DEFAULT | Representação JSON dos campos auditados depois da operação. |

**Relacionamentos:**

- `fk_historico_operacoes_responsavel`: `(membro_responsavel)` referencia `membros(codigo)`. Sem exclusão em cascata.

**Restrições do banco:**

- `ck_historico_operacoes_operacao`: aceita `INSERT`, `UPDATE` ou `DELETE`.
- `entidade_afetada` e `codigo_registro_afetado` não possuem FK nem lista de valores imposta por `CHECK`.
- As colunas de dados armazenam texto JSON produzido pelos triggers; o schema não declara `CHECK ISJSON`.

**Índices adicionais:**

- `ix_historico_operacoes_data`: `(data_hora DESC, codigo_operacao DESC)`.

**Regras da aplicação e das procedures:** Os triggers registram membros e participações. O histórico de membros omite CPF, e-mail, login e credenciais; o de participações omite o texto da observação. O responsável vem do contexto da sessão. A retenção padrão é de 180 dias.

</details>

### Recursos SQL utilizados pela aplicação

| Recurso | Objeto | Uso |
| --- | --- | --- |
| Procedure | `usp_criar_evento` | Valida e grava o evento, os detalhes e os participantes em transação. |
| Procedure | `usp_atualizar_evento` | Atualiza o evento e sincroniza participantes preservando os registros mantidos. |
| Procedure | `usp_atualizar_participacao` | Aplica as permissões e as regras de confirmação e presença. |
| View | `vw_agenda_detalhada` | Reúne os dados e as contagens exibidos na agenda. |
| View | `vw_presenca_musicos` | Fornece os dados do relatório de participação e presença. |
| View | `vw_ocupacao_locais` | Relaciona locais, eventos e duração para o relatório de utilização. |
| Função escalar | `fn_taxa_presenca_musico` | Calcula o percentual entre presenças e ausências lançadas em eventos realizados. |
| Função com retorno de tabela inline | `fn_conflitos_agendamento` | Consulta eventos sobrepostos para validação de conflitos. |
| Função com variável-tabela | `fn_resumo_periodo` | Retorna os indicadores de eventos agrupados por tipo no período. |
| Trigger | `trg_agenda_historico` | Registra criação, alteração, reagendamento, cancelamento e exclusão de eventos. |
| Trigger | `trg_participacao_historico` | Registra operações nas participações sem copiar o texto livre da observação. |
| Trigger | `trg_membros_historico` | Registra alterações dos campos administrativos selecionados dos membros. |

`AgendaSqlRepository` chama as três procedures, consulta a view de agenda e usa a função de conflitos. `RelatorioSqlRepository` consulta as views de presença e ocupação e as funções de taxa e resumo. Os triggers são disparados pelas gravações realizadas pela aplicação.

As operações de agenda utilizam locks transacionais por evento, local e músico, com aquisição em ordem determinística. As procedures verificam os conflitos antes da gravação. A política de locks também é aplicada na exclusão de locais.

Nos relatórios, o período considera a data de início do evento. A taxa de presença fica sem valor quando não existem lançamentos elegíveis; eventos cancelados não compõem esse cálculo.

## Configuração e segurança

O arquivo [backend/.env.example](backend/.env.example) reúne as opções de conexão, acesso e retenção. As configurações mais relevantes são:

| Configuração | Uso |
| --- | --- |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` e `DB_PASSWORD` | Conexão com o SQL Server. |
| `DB_URL` | URL JDBC completa para configuração personalizada da conexão. |
| `SERVER_PORT` | Porta do backend; padrão `8080`. |
| `CORS_ALLOWED_ORIGINS` | Origens autorizadas a acessar a API; padrão `http://localhost:5173`. |
| `AUTH_COOKIE_SECURE` | Use `true` ao publicar em HTTPS; o valor `false` atende ao desenvolvimento local em HTTP. |
| `FORWARD_HEADERS_STRATEGY` e `PROXY_IPS_REGEX` | Configuração dos cabeçalhos encaminhados por um proxy confiável. |
| `AUTH_RECUPERACAO_PENDENTE_HORAS` e `AUTH_RECUPERACAO_APROVADA_HORAS` | Validade do pedido e do código de recuperação. |
| `AUDITORIA_EVENTOS_DIAS` e `AUDITORIA_OPERACOES_DIAS` | Retenção dos históricos; padrões de 365 e 180 dias. |

- Senhas são armazenadas como hash BCrypt. Tokens de sessão e códigos de recuperação também têm somente seus hashes persistidos.
- A sessão tem duração de oito horas e utiliza cookie `HttpOnly` com `SameSite=Strict`. Operações de escrita exigem token CSRF, preparado pelo frontend.
- O login tem limitação por IP e por falhas da combinação conta/IP. Cadastro e recuperação possuem limites próprios; excesso retorna HTTP 429.
- Alterar a senha encerra as sessões do membro e invalida autorizações de recuperação abertas.
- Relatórios e históricos exigem permissão administrativa. O histórico de membros omite CPF, e-mail, login e credenciais; campos livres de eventos podem conter informações inseridas pelos usuários.
- A limpeza dos históricos é feita em lotes, conforme os prazos configurados, sem arquivamento automático.

Em uma publicação, altere as credenciais de demonstração, utilize HTTPS e configure a origem real do frontend. Para acesso direto ao backend, mantenha `FORWARD_HEADERS_STRATEGY=NONE`. Se houver proxy reverso, configure `NATIVE` e restrinja `PROXY_IPS_REGEX` aos IPs confiáveis do proxy, que deve controlar os cabeçalhos encaminhados.

## API

A base local é `http://localhost:8080/api`. As rotas privadas exigem a sessão por cookie. Para chamadas de escrita fora da interface, obtenha o token em `GET /api/auth/csrf`, preserve os cookies e envie o valor em `X-CSRF-Token`.

| Área | Rotas principais |
| --- | --- |
| Autenticação | `GET /auth/csrf`; `POST /auth/login`; `POST /auth/logout`. |
| Cadastro e recuperação | `POST /auth/cadastro`; `POST /auth/recuperar-senha`; `POST /auth/redefinir-senha`. |
| Solicitações de senha | `GET /redefinicoes-senha/abertas`; `PATCH /redefinicoes-senha/{id}`; `POST /redefinicoes-senha/{id}/cancelar`. |
| Agenda | `GET/POST /agenda`; `GET/PUT/DELETE /agenda/{id}`. |
| Participações | `GET /agenda/{id}/participacoes`; `PATCH /agenda/{id}/participacoes/minha`; `PATCH /agenda/{id}/participacoes/{musicoId}`. |
| Perfil | `GET/PUT /membros/me`. |
| Membros | `GET/POST /membros`; `PUT /membros/{id}`; `PATCH /membros/{id}/status`. |
| Aprovação de cadastros | `PATCH /membros/{id}/cadastro`; `POST /membros/{id}/cadastro/liberar-identificadores`. |
| Seleção de músicos | `GET /membros/musicos`. |
| Cidades e locais | `GET/POST /cidades` e `/locais`; `PUT/DELETE /cidades/{id}` e `/locais/{id}`. |
| Tipos de evento | `GET/POST /tipos-evento`; `GET /tipos-evento/admin`; `PUT/DELETE /tipos-evento/{id}`. |
| Relatórios | `GET /relatorios?inicio=AAAA-MM-DD&fim=AAAA-MM-DD`; `GET /relatorios/historicos`. |

As permissões de cada operação são conferidas pelo backend; estar autenticado não concede acesso administrativo.

## Solução de problemas

| Situação | O que conferir |
| --- | --- |
| `mvn`, `java`, `node` ou `npm` não reconhecido | Instalação da ferramenta e configuração do `PATH`; reabra o terminal após instalar. |
| Falha de conexão com o banco | Serviço da instância, TCP/IP, porta, endereço e valores do `backend/.env`. |
| Login `agenda_user` recusado | Modo de autenticação do SQL Server, execução do script `00` na instância correta e senha configurada. |
| Tabela, função, view ou procedure inexistente | Execução completa dos sete scripts, na ordem indicada, e conexão ao banco `AgendaBanda`. |
| Pacote nativo do Rollup ausente | Execute `npm ci --include=optional` dentro de `web` no próprio sistema de destino. |
| Interface sem acesso à API | Backend em execução, porta `8080` e configuração do proxy em `web/vite.config.ts`. |
| Requisição recusada por origem ou CSRF | Acesso por `http://localhost:5173`, origem autorizada no backend e configuração de cookies compatível com HTTP local ou HTTPS publicado. |
| Porta já ocupada | Libere as portas `8080` e `5173` ou ajuste em conjunto o backend, o proxy e a origem autorizada. |

## Equipe e licença

**Equipe: System of a dev**

- Eduardo R. R. N.: qualidade e documentação.
- Lorena A. T.: gerenciamento do projeto.
- Paulo S. S.: desenvolvimento.
- Roberto B. F.: documentação e qualidade.

Licença GNU GPL v3. Consulte o arquivo [LICENSE](LICENSE).
