# API

[Voltar ao README](README.md)

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

Em `PUT /membros/me`, os dados comuns são `nome`, `cpf`, `email` e `login`. Músicos podem atualizar `instrumento` e `biografia`; produtores podem atualizar `especialidade`, com até 100 caracteres e sem valor em branco. Omitir a especialidade preserva o valor existente. Campos de outro perfil não alteram seus subtipos nem concedem permissões. A troca de senha utiliza `senhaAtual` e `novaSenha`.

O administrador registra ausência com `{"presente":false}` em `PATCH /agenda/{id}/participacoes/{musicoId}`, preservando confirmação e data de resposta. Para registrar presença, envie `{"presente":true}` se o músico já estiver confirmado, ou `{"presente":true,"statusConfirmacao":"CONFIRMADO"}` para confirmar e marcar presença. As regras de horário e cancelamento continuam aplicadas pelo backend.

As listagens `GET /agenda` e `GET /membros` aceitam `page` (a partir de zero) e `size` (de 1 a 100). Os padrões são 50 eventos e 20 membros por página. A agenda também aceita `inicio` e `fim` em `AAAA-MM-DD`, incluindo os dois dias, para filtrar a data de início do evento. Exemplo: `GET /agenda?page=0&size=50&inicio=2026-11-01&fim=2026-11-30`.

A resposta paginada contém `content`, `page`, `size`, `totalElements` e `totalPages`. O calendário consulta somente o período visível e oferece navegação entre páginas quando necessário.

Datas e horas nas respostas usam ISO 8601 com offset, como `2026-11-01T19:00:00-03:00`. Na criação e edição, a API aceita valores com offset ou valores sem offset interpretados como horário de São Paulo. Uma lista de participantes aceita até 500 códigos por requisição.

Erros da API seguem o formato `{"erro":"Mensagem","codigo":"CODIGO"}`, com HTTP 400 para dados inválidos, 401 para autenticação, 403 para permissão, 404 para recurso inexistente, 409 para conflito, 429 para excesso de tentativas e 500 para falhas internas. Detalhes internos permanecem nos logs do servidor.
