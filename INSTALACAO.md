# Instalação e execução

[Voltar ao README](README.md)

## Sumário

- [Tecnologias e requisitos](#tecnologias-e-requisitos)
- [Obter o projeto](#obter-o-projeto)
- [Preparar o SQL Server](#preparar-o-sql-server)
- [Iniciar o backend](#iniciar-o-backend)
- [Iniciar o frontend](#iniciar-o-frontend)
- [Solução de problemas](#solução-de-problemas)

## Tecnologias e requisitos

| Componente | Tecnologia | Requisito para execução |
| --- | --- | --- |
| Backend | Java, Spring Boot, Spring Data JPA e JDBC | JDK 17 e Maven 3.9. |
| Frontend | React, TypeScript, Vite e Chakra UI | Node.js 22 ou superior e npm. |
| Banco de dados | Microsoft SQL Server | SQL Server 2016 SP1 ou superior; a edição Express pode ser utilizada. |
| Administração do banco | Scripts T-SQL | SQL Server Management Studio ou outro cliente compatível com separadores `GO`. |

O banco precisa de **nível de compatibilidade 130 ou superior**. Java, Maven, Node.js e npm devem estar disponíveis no terminal.

## Obter o projeto

Clone o repositório e entre na pasta criada:

```powershell
git clone https://github.com/Sollo99/AgendaBandaV2.git AgendaBanda
cd AgendaBanda
```

Se o projeto já foi baixado, abra o terminal na pasta que contém `backend`, `database` e `web`.

## Preparar o SQL Server

1. Inicie a instância do SQL Server e conecte-se a ela pelo SQL Server Management Studio.
2. Para a conexão usada pela aplicação, habilite a autenticação do SQL Server junto à autenticação do Windows e o protocolo TCP/IP. Os valores padrão do projeto usam `localhost:1433`; se a instância utilizar outro endereço ou porta, ajuste a configuração do backend.
3. Conecte-se com uma conta que tenha permissão para criar o banco e o login da aplicação.
4. Execute os arquivos de [database](database) inteiros, uma única vez e na ordem numérica abaixo.

| Ordem | Script | Finalidade |
| --- | --- | --- |
| 1 | [00_create_login.sql](database/00_create_login.sql) | Cria o banco `AgendaBanda`, o login `agenda_user` e suas permissões. |
| 2 | [01_create_schema.sql](database/01_create_schema.sql) | Cria tabelas, relacionamentos, restrições e índices. |
| 3 | [02_functions.sql](database/02_functions.sql) | Cria as funções de presença, conflitos e resumo por período. |
| 4 | [03_views.sql](database/03_views.sql) | Cria as consultas de agenda, presenças, ocupação dos locais, histórico unificado e sessões autorizadas. |
| 5 | [04_procedures.sql](database/04_procedures.sql) | Cria as operações transacionais de eventos, participações, cadastros, acesso, recuperação e manutenção. |
| 6 | [05_triggers.sql](database/05_triggers.sql) | Cria os registros automáticos de auditoria. |
| 7 | [06_insert_data.sql](database/06_insert_data.sql) | Insere as contas e os dados de demonstração, gerando seus históricos pelas triggers. |

**Use uma base dedicada ao projeto:** o script `01_create_schema.sql` recria os objetos e remove os dados anteriores. O backend utiliza a estrutura preparada pelos scripts; sua inicialização não cria as tabelas.

A carga de demonstração é executada por último, numa única transação, com as triggers habilitadas. Seus registros de histórico ficam sem responsável autenticado. Login, logout e recuperação de senha geram seus próprios históricos conforme essas ações forem realizadas na aplicação.

Ao alterar autenticação ou TCP/IP na configuração do SQL Server, reinicie o serviço da instância.

## Iniciar o backend

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

## Iniciar o frontend

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
