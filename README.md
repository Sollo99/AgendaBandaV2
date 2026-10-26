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
* Um único perfil operacional por membro: membro, músico ou produtor
* Proteção do último administrador ativo

## Agenda

* Cadastro de eventos
* Visualização de eventos
* Detalhes completos do evento
* Edição e exclusão por administrador ou criador do evento
* Edição preserva confirmações, presenças e respostas dos participantes mantidos
* Bloqueio de horários sobrepostos para o mesmo local ou músico

## Eventos específicos

* Ensaio com repertório, observação e músicos vinculados
* Show com cidade vinculada
* Reunião com pauta

## Locais

* Cadastro de locais
* Edição e exclusão de locais, com bloqueio quando houver eventos vinculados
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

Os scripts criam o banco AgendaBanda e inserem dados de demonstração: 20 membros, 10 músicos, 10 produtores, 10 cidades, 10 locais, 3 tipos e 30 eventos com suas participações. Sessões e solicitações de senha começam vazias. Os CPFs de demonstração são exemplos com dígitos verificadores válidos.

`ParticipacaoEvento` é o único relacionamento entre músicos e eventos, incluindo ensaios, shows e reuniões. Cada membro possui um único perfil operacional em `membros.perfil`; as tabelas `musicos` e `produtores` usam chaves estrangeiras compostas com esse discriminador para impedir perfis simultâneos. A permissão de administrador é independente do perfil operacional.

---

# Como Executar o Projeto

## 1. Banco de dados

Abra o SQL Server Management Studio e execute:

```bash
database/00_create_login.sql
database/01_create_schema.sql
database/02_insert_data.sql
```

Esses scripts criam o banco AgendaBanda, o login agenda_user e os dados de demonstração.

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

O cadastro público cria um membro com perfil de músico, sem permissão administrativa e com cadastro `PENDENTE`, sem acesso à agenda. Na tela Usuários, um administrador aprova ou recusa o cadastro. A aprovação libera a conta; a recusa mantém o acesso bloqueado e pode ser reconsiderada pelo administrador. Cadastro pendente/recusado é diferente de conta aprovada e desativada: ativar a conta ou editar sua flag não substitui a aprovação. A resposta pública é sempre HTTP 202 com a mesma mensagem para novos cadastros e CPF/e-mail/login já existentes, sem retornar os dados do membro nem alterar contas existentes. Erros de formato continuam sendo informados.

A mensagem pública explica que CPF/e-mail/login já utilizados impedem outro cadastro e orienta entrar, recuperar a senha ou contatar um administrador. Para um cadastro recusado, o administrador pode usar **Liberar identificadores**: CPF, e-mail e login são removidos daquele pedido e ficam disponíveis para novo cadastro. O registro recusado mantém nome, código, decisão original e responsável/data da liberação; ele permanece inativo e não pode mais ser aprovado ou editado. Antes da liberação, o administrador ainda pode reconsiderar a recusa. Contas aprovadas ou pendentes não permitem essa liberação.

Músicos inativos não aparecem na seleção de novos participantes; os vínculos antigos são mantidos e podem ser removidos ao editar eventos ainda não realizados e sem presença/ausência registrada para o participante. A edição administrativa atualiza o perfil existente. A troca de músico para outro perfil é bloqueada se houver participações vinculadas.

Membros são desativados e reativados pelo administrador. Não existe endpoint de exclusão física de membros: isso preserva vínculos e evita perder registros de eventos e decisões de recuperação de senha. O último administrador ativo não pode ser desativado nem perder a permissão administrativa. Essa regra vale tanto para a edição completa quanto para a ação de ativar/desativar.

As entradas da API e os campos da interface respeitam os tamanhos das colunas SQL. CPF aceita formato com ou sem pontuação, tem os dígitos verificadores validados e é armazenado somente com números. Login aceita letras, números, ponto, hífen e sublinhado; UF deve ser brasileira. Senhas novas, inclusive no cadastro administrativo, exigem pelo menos 6 caracteres e no máximo 72 bytes em UTF-8 por compatibilidade com BCrypt. Na edição administrativa, deixar a senha vazia mantém a atual.

Para recuperar uma senha, o membro informa o login ou e-mail na tela inicial. O pedido permanece `PENDENTE` por até 24 horas. Após a aprovação, há um novo prazo de 2 horas para definir a senha. O backend verifica o prazo ao consultar, aprovar e concluir o pedido; também há uma verificação periódica a cada minuto. Pedidos vencidos viram `EXPIRADA` e deixam de impedir novas solicitações. Antes de criar um novo pedido, o anterior vencido é encerrado na mesma transação. Os prazos usam o horário de São Paulo e são configuráveis em `backend/.env.example`.

O administrador vê pedidos pendentes e aprovados em Recuperações de senha e pode cancelar qualquer pedido aberto. Cancelamento e expiração preservam data e motivo do encerramento. Alterar a senha pelo perfil ou pela edição administrativa cancela pedidos abertos e revoga todas as sessões do membro. A conclusão por recuperação também revoga as sessões e consome a autorização. É necessário entrar novamente após trocar a senha.

Depois da aprovação, o membro pode informar tanto o login quanto o e-mail para definir a nova senha. A referência da recuperação fica em cookie HttpOnly, sem usar o localStorage. A tela também exibe um código para continuar em outro navegador. A validade é conferida no servidor; possuir o cookie ou código de um pedido vencido/cancelado não permite redefinir a senha.

Pedidos públicos retornam a mesma mensagem e um código opaco para contas existentes, inexistentes, inativas ou com outro pedido aberto. Receber um código não confirma a existência da conta nem a aprovação. Somente uma chave de pedido aprovado e dentro do prazo libera a redefinição. O SQL permite uma única solicitação aberta (`PENDENTE` ou `APROVADA`) por membro; pedidos concorrentes são serializados.

Há limites independentes por IP e por alvo: 20 requisições de login em 15 minutos, 5 de cadastro em 60 minutos e 10 de recuperação/conclusão em 15 minutos. Mudar de IP não zera o contador do alvo: login usa o código da conta quando existente; recuperação por login/e-mail e conclusão usam o mesmo código do membro; cadastro contabiliza CPF, e-mail e login separadamente. Os identificadores são normalizados e os contadores guardam hashes. Há também tetos globais por fluxo, respectivamente 200, 50 e 100 requisições nas mesmas janelas, limitando tentativas com vários IPs e alvos.

Todas as tentativas que chegam a cada contador contam, inclusive as bem-sucedidas. Os valores são configuráveis no `.env`; excesso retorna HTTP 429 com `Retry-After`. O controle fica em memória por instância do backend e reinicia com o processo; várias instâncias exigem um armazenamento compartilhado ou controle equivalente no gateway. Um teto global atingido também afeta usuários legítimos até a janela vencer. O login retorna a mesma mensagem e HTTP 401 para credenciais incorretas, conta inexistente, desativada ou ainda sem aprovação; a comparação BCrypt ocorre antes da decisão, usando hash fictício para login inexistente.

A sessão dura 8 horas. Ao abrir o frontend, o token é validado em `/api/membros/me` antes de mostrar telas privadas. Um HTTP 401 em chamada autenticada limpa a sessão e volta ao login com aviso único, inclusive com várias chamadas simultâneas. HTTP 403 e falha de rede não encerram a sessão; uma falha na verificação inicial oferece nova tentativa. Erro no login público não dispara aviso de sessão expirada, e uma resposta antiga não encerra um login mais recente.

Uma tarefa remove as sessões com `expira_em <= agora` a cada minuto, configurável por `AUTH_SESSAO_VARREDURA_MS`. A sessão vencida já é recusada na autenticação mesmo antes da limpeza. O formulário de login inicia vazio; as contas de demonstração continuam listadas nesta documentação.

Ao editar eventos, os participantes mantidos preservam confirmação, presença, observação e data de resposta. Uma lista de músicos omitida mantém os vínculos; uma lista vazia remove os participantes somente quando não houver impedimentos de histórico; somente os novos entram como `PENDENTE`. Eventos `REALIZADO` não podem ser excluídos, reabertos ou perder participantes. Eventos com presença ou ausência registrada também não podem ser excluídos, e o participante com esse registro não pode ser removido. As respostas da agenda enviam apenas código e nome do criador e código, nome e instrumento dos músicos, sem CPF, e-mail ou credenciais.

O músico só altera sua confirmação quando o evento está `AGENDADO` e seu início ainda está no futuro. No instante inicial e depois, ou se cancelado/realizado, a resposta é bloqueada. O administrador continua podendo corrigir participações e registrar presença em eventos realizados. As operações de participação bloqueiam primeiro a linha do evento para não disputar com cancelamento/exclusão sem serialização.

O administrador só registra presença ou ausência a partir do início de um evento não cancelado. `presente=true` exige `statusConfirmacao=CONFIRMADO`, inclusive em mudanças posteriores; ausência pode ser registrada sem confirmação. Para uma participação pendente ou recusada, o botão **Confirmar e marcar presente** envia as duas alterações na mesma ação. A regra de confirmação também é protegida por CHECK no banco. Eventos com presença/ausência registrada não podem ser cancelados ou movidos para o futuro. `REALIZADO` exige que o fim do evento já tenha ocorrido, tanto na criação quanto na edição; o formulário e o backend verificam essa condição pelo término informado.

O schema exige cidade única por `(nome, uf)`, com collation `Latin1_General_100_CI_AS` nesses campos para não distinguir maiúsculas/minúsculas. A consulta prévia da aplicação melhora a mensagem de duplicidade; a restrição única protege inserções e atualizações concorrentes. `categoria_detalhe` também possui CHECK com os valores `EVENTO`, `ENSAIO`, `SHOW` e `REUNIAO`.

Ao editar a própria conta, a interface aplica imediatamente o perfil devolvido pelo backend. Se o administrador remover sua própria permissão, a navegação administrativa é retirada e a tela volta à agenda, sem tentar recarregar a lista de membros. Desativar a própria conta encerra a sessão. A proteção do último administrador continua sendo aplicada pelo backend.

Eventos não cancelados reservam local e todos os músicos vinculados, independentemente da confirmação individual. Há conflito quando `início existente < fim solicitado` e `fim existente > início solicitado`; horários consecutivos são permitidos. Eventos cancelados não reservam horário, e sua reativação passa pela mesma validação. A agenda usa horários locais da banda. No SQL Server, as escritas da agenda e a exclusão de locais obtêm um bloqueio transacional na tabela de locais antes da validação: isso prioriza consistência para o volume de uma banda, serializando essas operações entre instâncias do backend. Escritas SQL feitas fora da aplicação precisam seguir a mesma regra de agendamento.

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

Para compilar a interface, use `npm run build` dentro de `web`.

---

# Contas de demonstração

```bash
admin / admin
usuario / usuario
teste / teste
```

O seed contém 30 eventos entre outubro e dezembro de 2026: 10 ensaios realizados em outubro, com presença, 10 shows agendados em novembro e 10 reuniões agendadas em dezembro. Em 26/10/2026 há exemplos passados e futuros.

---

# Endpoints principais

```bash
POST /api/auth/login
POST /api/auth/logout
POST /api/auth/cadastro
POST /api/auth/recuperar-senha
POST /api/auth/redefinir-senha
GET /api/redefinicoes-senha/abertas
PATCH /api/redefinicoes-senha/{id}
POST /api/redefinicoes-senha/{id}/cancelar
GET /api/agenda
POST /api/agenda
PUT /api/agenda/{id}
DELETE /api/agenda/{id}
GET /api/membros
POST /api/membros
PUT /api/membros/{id}
PATCH /api/membros/{id}/status
PATCH /api/membros/{id}/cadastro
POST /api/membros/{id}/cadastro/liberar-identificadores
GET /api/locais
POST /api/locais
PUT /api/locais/{id}
DELETE /api/locais/{id}
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

Projeto desenvolvido para o Projeto de Extensão Comunitária do 4º semestre.

---

# Futuras Melhorias

* Relatórios de eventos
* Notificações automáticas
