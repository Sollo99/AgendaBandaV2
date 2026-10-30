# Dicionário de dados

[Voltar ao README](README.md)

## Tabelas

- [membros](#membros)
- [musicos](#musicos)
- [produtores](#produtores)
- [cidades](#cidades)
- [locais](#locais)
- [tipos_evento](#tipos_evento)
- [agenda](#agenda)
- [ensaios](#ensaios)
- [shows](#shows)
- [reunioes](#reunioes)
- [participacao_evento](#participacao_evento)
- [redefinicoes_senha](#redefinicoes_senha)
- [sessoes](#sessoes)
- [historico_eventos](#historico_eventos)
- [historico_operacoes](#historico_operacoes)

Os nomes, tipos, valores padrão, chaves e restrições abaixo correspondem ao [schema SQL Server](database/01_create_schema.sql). O dicionário abrange as 15 tabelas e seus 90 campos. As regras descritas como da aplicação ou das procedures complementam as restrições do schema.

**Convenções:**

- **PK:** chave primária. Quando indicada em mais de um campo da mesma tabela, forma uma chave composta.
- **FK:** chave estrangeira. Os campos de uma FK composta são validados em conjunto.
- **UK:** unicidade. Restrições compostas e índices únicos filtrados estão detalhados na respectiva tabela.
- **Nulo:** indica se o schema permite `NULL`; permissões condicionais, como a liberação dos identificadores de um cadastro recusado, também devem respeitar os `CHECKs`.
- **Padrão / geração:** `IDENTITY(1,1)` gera o identificador no banco. `Sem DEFAULT` indica ausência de uma cláusula `DEFAULT`; o valor é informado na gravação, ou pode ficar nulo quando permitido.
- **Tipos:** `BIT` representa 0 ou 1; `NVARCHAR` armazena texto Unicode; `CHAR` possui comprimento fixo. `DATETIME2` não armazena fuso; a aplicação utiliza o horário de São Paulo e o padrão SQL `SYSDATETIME()` utiliza o relógio do servidor de banco.
- **Exclusão:** as FKs sem `ON DELETE CASCADE` impedem excluir um registro pai enquanto houver referências. As cascatas declaradas estão indicadas em cada relacionamento.

<a id="membros"></a>

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
- `ix_membros_nome_codigo`: `(nome, codigo)`, utilizado na listagem paginada de usuários.

**Regras da aplicação e das procedures:** O cadastro público define `PENDENTE`, `ativo = 0` e `administrador = 0`, substituindo os padrões do schema. A procedure `usp_alterar_acesso_membro` protege o último administrador ativo nas edições administrativas e mudanças de status. `usp_decidir_cadastro` registra aprovação ou recusa; `usp_liberar_identificadores` libera os dados de cadastros recusados, preservando a decisão e revogando as sessões. A aplicação utiliza desativação para preservar os vínculos.

</details>

<a id="musicos"></a>

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

<a id="produtores"></a>

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

<a id="cidades"></a>

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

<a id="locais"></a>

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

**Regras da aplicação e das procedures:** `usp_excluir_local` exige administrador ativo, adquire o lock lógico do local e impede a exclusão quando existem eventos vinculados.

</details>

<a id="tipos_evento"></a>

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

<a id="agenda"></a>

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

- `ix_agenda_periodo`: `(dt_hora_in, codigo_agenda) INCLUDE (dt_hora_fim, codigo_local, status)`, utilizado nos filtros de data e na ordenação da agenda.
- `ix_agenda_local_periodo`: `(codigo_local, dt_hora_in) INCLUDE (dt_hora_fim, status)`, utilizado nas consultas por local.

**Regras da aplicação e das procedures:** As procedures verificam permissões, atividade dos participantes e conflitos por local ou músico. `REALIZADO` exige término já ocorrido; presença registrada impede cancelamento ou reagendamento para o futuro. A exclusão protege eventos realizados ou com presença/ausência.

</details>

<a id="ensaios"></a>

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

<a id="shows"></a>

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

<a id="reunioes"></a>

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

<a id="participacao_evento"></a>

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

<a id="redefinicoes_senha"></a>

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

**Regras da aplicação e das procedures:** A aprovação após verificação manual de identidade gera uma credencial nova no servidor e substitui seu hash. Prazos padrão: 24 horas para o pedido pendente e duas horas após a aprovação. `usp_expirar_recuperacoes` encerra pedidos vencidos; `usp_cancelar_recuperacoes` cancela autorizações abertas, inclusive por troca de senha. O cancelamento administrativo de um pedido já vencido o registra como expirado. A criação, aprovação e conclusão continuam coordenadas pelo Java, com validação da chave e transações. Cancelamento, consumo e expiração liberam uma nova solicitação.

</details>

<a id="sessoes"></a>

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

**Regras da aplicação e das procedures:** O serviço define duração de oito horas. A autenticação consulta `vw_sessoes_membros` e exige prazo ainda válido, membro ativo e cadastro aprovado. `usp_limpar_sessoes` remove periodicamente até 1000 sessões vencidas por execução. Alterar a senha revoga as sessões do membro.

</details>

<a id="historico_eventos"></a>

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

**Regras da aplicação e das procedures:** O trigger preenche a operação e a data; as procedures de criação, edição e exclusão complementam os detalhes do evento. O responsável vem do contexto da sessão. A retenção padrão é de 365 dias.

</details>

<a id="historico_operacoes"></a>

<details>
<summary><strong>historico_operacoes</strong>: Auditoria de cadastros, participações, recuperações e autenticação.</summary>

| Campo | Tipo SQL Server | Nulo | Chave | Padrão / geração | Descrição |
| --- | --- | --- | --- | --- | --- |
| `codigo_operacao` | `BIGINT` | Não | PK | `IDENTITY(1,1)` | Identificador do registro de auditoria geral. |
| `membro_responsavel` | `INT` | Sim | FK | Sem DEFAULT | Membro identificado como responsável pela operação, quando disponível. |
| `entidade_afetada` | `NVARCHAR(50)` | Não | Nenhuma | Sem DEFAULT | Identificação da entidade ou do fluxo de autenticação auditado. |
| `codigo_registro_afetado` | `NVARCHAR(100)` | Não | Nenhuma | Sem DEFAULT | Código do registro, combinação agenda:musico ou NAO_IDENTIFICADO para autenticação sem membro conhecido. |
| `tipo_operacao` | `NVARCHAR(20)` | Não | Nenhuma | Sem DEFAULT | Operação de persistência ou evento de autenticação registrado. |
| `data_hora` | `DATETIME2` | Não | Nenhuma | Sem DEFAULT | Data e hora da operação registrada pela trigger ou procedure. |
| `dados_anteriores` | `NVARCHAR(MAX)` | Sim | Nenhuma | Sem DEFAULT | Representação JSON dos campos auditados antes da operação. |
| `dados_novos` | `NVARCHAR(MAX)` | Sim | Nenhuma | Sem DEFAULT | Representação JSON dos campos auditados depois da operação. |

**Relacionamentos:**

- `fk_historico_operacoes_responsavel`: `(membro_responsavel)` referencia `membros(codigo)`. Sem exclusão em cascata.

**Restrições do banco:**

- `ck_historico_operacoes_operacao`: aceita `INSERT`, `UPDATE`, `DELETE`, `LOGIN`, `LOGOUT`, `LOGIN_FALHOU`, `ACESSO_NEGADO` e `LIMITE_EXCEDIDO`.
- `entidade_afetada` e `codigo_registro_afetado` não possuem FK nem lista de valores imposta por `CHECK`.
- As colunas de dados armazenam texto JSON produzido pelas triggers e pela procedure de autenticação; o schema não declara `CHECK ISJSON`.

**Índices adicionais:**

- `ix_historico_operacoes_data`: `(data_hora DESC, codigo_operacao DESC)`.

**Regras da aplicação e das procedures:** As triggers registram membros, participações, recuperações, cidades, locais, tipos de evento e perfis de músico/produtor. O histórico de membros omite valores de CPF, e-mail, login e credenciais; registra marcadores de alteração desses campos. Biografia, hash de recuperação e motivo livre de encerramento também são representados por marcadores, sem cópia do conteúdo. O histórico de participações omite o texto da observação. Os eventos de autenticação são gravados por procedure. O responsável vem do contexto da sessão ou da identidade verificada pelo serviço; operações anônimas e automáticas podem não ter responsável. A retenção padrão é de 180 dias.

</details>
