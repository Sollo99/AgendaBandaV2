# Agenda da Banda

## Sobre o Projeto

O **Agenda da Banda** é um MPV desenvolvido para auxiliar uma banda no controle de compromissos como ensaios, shows e reuniões.

O sistema possui login, agenda, cadastro de eventos, gerenciamento de usuários e locais. A proposta é centralizar as informações para evitar conflitos de horário, esquecimentos e perda de detalhes importantes.

---

# Tecnologias Utilizadas

* Java 17
* Spring Boot
* SQL Server
* BCrypt para senhas
* React
* TypeScript
* Vite
* Chakra UI 3
* Maven
* Git e GitHub

---

# Estrutura do Projeto

```bash
agenda-banda-mpv/
 ├── backend/
 │   ├── src/main/java/br/com/fatec/agendabanda/
 │   │   ├── config/
 │   │   ├── controller/
 │   │   ├── dto/
 │   │   ├── exception/
 │   │   ├── model/
 │   │   ├── repository/
 │   │   ├── security/
 │   │   └── service/
 │   └── src/main/resources/db/scripts/
 ├── database/
 └── web/
     └── src/
```

---

# Funcionalidades

## Login

* Login com usuário e senha
* Validação das credenciais no backend
* Controle simples de sessão por token
* Logout

## Usuários

* Cadastro de membros
* Edição de membros
* Ativação e desativação de usuários
* Controle de administrador por flag
* Tabelas específicas para músicos e produtores

## Agenda

* Cadastro de eventos
* Visualização de eventos
* Detalhes completos do evento
* Edição e exclusão por administrador ou criador do evento

## Eventos específicos

* Ensaio com repertório, observação e músicos vinculados
* Show com cidade vinculada
* Reunião com pauta

## Locais

* Cadastro de locais
* Associação de eventos com local

---

# Arquitetura

O projeto utiliza uma arquitetura em camadas no backend:

* `model` → Entidades do sistema
* `repository` → Acesso ao banco com Spring Data JPA
* `service` → Regras de negócio
* `controller` → Endpoints REST
* `security` → Controle simples de sessão
* `dto` → Objetos de entrada e saída da API

O frontend consome a API REST do backend e apresenta as telas do sistema em português.

---

# Banco de Dados

Os scripts estão em dois lugares:

```bash
database/
backend/src/main/resources/db/scripts/
```

Ordem de execução no SQL Server Management Studio:

```bash
00_create_login.sql
01_create_schema.sql
02_insert_data.sql
```

O script cria o banco AgendaBanda e insere dados de teste, com pelo menos 10 registros em cada tabela.

---

# Como Executar o Projeto

## 1. Banco de dados

Abra o SQL Server Management Studio e execute:

```bash
database/00_create_login.sql
database/01_create_schema.sql
database/02_insert_data.sql
```

Esses scripts criam o banco AgendaBanda, o login agenda_user e os dados de teste.

Os scripts `01_create_schema.sql` e `02_insert_data.sql` criam toda a estrutura atual, incluindo tipos de evento, participações, redefinições de senha e sessões.

---

## 2. Backend

Entre na pasta do backend:

```bash
cd backend
```

Se o SQL Server local estiver na porta padrão 1433, nenhuma configuração extra é necessária. O backend já usa:

```bash
DB_HOST=localhost
DB_PORT=1433
DB_NAME=AgendaBanda
DB_USER=agenda_user
DB_PASSWORD=agenda123
```

Se precisar alterar host, porta, senha ou usar uma instância nomeada, crie um arquivo `.env` na pasta backend seguindo:

```bash
.env.example
```

Exemplo para instância nomeada:

```bash
DB_URL=jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=AgendaBanda;encrypt=true;trustServerCertificate=true
DB_USER=agenda_user
DB_PASSWORD=agenda123
```

Para recuperar uma senha, o membro informa o login ou e-mail na tela inicial. A solicitação fica pendente até um administrador aprovar ou recusar pelo aviso exibido no sistema. Depois da aprovação, no mesmo navegador, o membro informa o mesmo login ou e-mail e é direcionado para cadastrar a nova senha.

Execute:

```bash
mvn spring-boot:run
```

Backend padrão:

```bash
http://localhost:8080
```

---

## 3. Frontend

Entre na pasta web:

```bash
cd web
```

Instale as dependências:

```bash
npm install
```

Execute:

```bash
npm run dev
```

Frontend padrão:

```bash
http://localhost:5173
```

---

# Logins de Teste

```bash
admin / admin
usuario / usuario
teste / teste
```

---

# Endpoints principais

```bash
POST /api/auth/login
POST /api/auth/logout
POST /api/auth/cadastro
POST /api/auth/recuperar-senha
POST /api/auth/redefinir-senha
GET /api/redefinicoes-senha/pendentes
PATCH /api/redefinicoes-senha/{id}
GET /api/agenda
POST /api/agenda
PUT /api/agenda/{id}
DELETE /api/agenda/{id}
GET /api/membros
POST /api/membros
PUT /api/membros/{id}
PATCH /api/membros/{id}/status
GET /api/locais
GET /api/cidades
GET /api/tipos-evento
GET /api/tipos-evento/admin
POST /api/tipos-evento
PUT /api/tipos-evento/{id}
DELETE /api/tipos-evento/{id}
POST /api/cidades
PUT /api/cidades/{id}
DELETE /api/cidades/{id}
GET /api/agenda/{id}/participacoes
PATCH /api/agenda/{id}/participacoes/minha
PATCH /api/agenda/{id}/participacoes/{musicoId}
GET /api/membros/me
PUT /api/membros/me
```

---

# Construtores (Porque construimos o projeto)

**System of a dev**
* Eduardo R. R. N. - Tester/Documentacao
* Lorena A. T. - Project Manager
* Paulo S. S. = Main Developer
* Roberto B. F. - Documentacao/Tester

Projeto desenvolvido para o Projeto de Extensão Comunitária do 3º semestre.

---

# Futuras Melhorias

* Calendário mensal visual
* Relatórios de eventos
* Notificações automáticas
* Deploy em ambiente definitivo
* Envio de mensagens via Whatsapp
