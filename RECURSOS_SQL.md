# Recursos SQL e auditoria

[Voltar ao README](README.md)

Consulte a [ordem de instalação](INSTALACAO.md#preparar-o-sql-server), o [DER](BANCO_DE_DADOS.md#der) e o [dicionário de dados](DICIONARIO_DE_DADOS.md).

## Recursos utilizados pela aplicação

| Recurso | Objeto | Uso |
| --- | --- | --- |
| Procedure | `usp_criar_evento` | Valida e grava o evento, os detalhes e os participantes em transação. |
| Procedure | `usp_atualizar_evento` | Atualiza o evento e sincroniza participantes preservando os registros mantidos. |
| Procedure | `usp_atualizar_participacao` | Aplica as permissões e as regras de confirmação e presença. |
| Procedure | `usp_excluir_evento` | Verifica permissões, bloqueia recursos e exclui eventos elegíveis, preservando os detalhes no histórico. |
| Procedure | `usp_alterar_acesso_membro` | Altera ativação e permissão administrativa, exige cadastro aprovado para conceder acesso e protege o último administrador ativo. |
| Procedure | `usp_decidir_cadastro` | Aprova ou recusa cadastros, registra o administrador responsável e impede reativação de identificadores já liberados. |
| Procedure | `usp_liberar_identificadores` | Libera CPF, e-mail e login de cadastro recusado, preserva a recusa, substitui o hash da senha pelo BCrypt de um segredo aleatório descartado e revoga sessões. |
| Procedure | `usp_excluir_local` | Autoriza e bloqueia o local antes de verificar os vínculos e excluir. |
| Procedure | `usp_expirar_recuperacoes` | Encerra pedidos vencidos, com lock do membro antes da alteração da solicitação. |
| Procedure | `usp_cancelar_recuperacoes` | Cancela pedidos abertos por membro ou solicitação, respeitando autorização e prazo. |
| Procedure | `usp_limpar_historicos` | Aplica os prazos de retenção em lotes de até 1000 registros por tabela na chamada da aplicação. |
| Procedure | `usp_limpar_sessoes` | Remove sessões vencidas em lotes de até 1000 registros na chamada da aplicação. |
| Procedure | `usp_registrar_autenticacao` | Registra login, logout, credenciais inválidas, recusas de acesso e limites excedidos com metadados controlados. |
| View | `vw_agenda_detalhada` | Reúne os dados e as contagens exibidos na agenda. |
| View | `vw_presenca_musicos` | Fornece os dados do relatório de participação e presença. |
| View | `vw_ocupacao_locais` | Relaciona locais, eventos e duração para o relatório de utilização. |
| View | `vw_historico_unificado` | Unifica históricos de eventos e operações, incluindo o nome do responsável quando disponível. |
| View | `vw_sessoes_membros` | Reúne sessões e membros ativos com cadastro aprovado; a consulta Java aplica o hash e a validade temporal. |
| Função escalar | `fn_taxa_presenca_musico` | Calcula o percentual entre presenças e ausências lançadas em eventos realizados. |
| Função com retorno de tabela inline | `fn_conflitos_agendamento` | Consulta eventos sobrepostos para validação de conflitos. |
| Função com variável-tabela | `fn_resumo_periodo` | Retorna os indicadores de eventos agrupados por tipo no período. |
| Trigger | `trg_agenda_historico` | Registra criação, alteração, reagendamento, cancelamento e exclusão de eventos. |
| Trigger | `trg_participacao_historico` | Registra operações nas participações sem copiar o texto livre da observação. |
| Trigger | `trg_membros_historico` | Registra alterações administrativas e marcadores de mudança de senha, CPF, e-mail e login, sem copiar os valores protegidos. |
| Trigger | `trg_recuperacoes_historico` | Registra abertura, aprovação, recusa, conclusão, cancelamento e expiração, sem copiar hash ou motivo livre. |
| Trigger | `trg_cidades_historico` | Registra inclusão, alteração e exclusão de cidades. |
| Trigger | `trg_locais_historico` | Registra inclusão, alteração e exclusão de locais, preservando os dados após a remoção. |
| Trigger | `trg_tipos_evento_historico` | Registra mudanças nos tipos de evento, inclusive categoria e ativação. |
| Trigger | `trg_musicos_historico` | Registra o perfil musical e instrumento; mudanças de biografia são sinalizadas sem copiar o texto. |
| Trigger | `trg_produtores_historico` | Registra inclusão, alteração e exclusão dos perfis de produtor e sua especialidade. |

São utilizadas 13 procedures, cinco views, três funções e nove triggers. `AgendaSqlRepository` chama as quatro procedures de eventos e participações, consulta a view de agenda e usa a função de conflitos. `MembroSqlRepository` chama as procedures de acesso, decisão cadastral e liberação de identificadores. `LocalSqlRepository`, `RecuperacaoSqlRepository`, `ManutencaoSqlRepository` e `AuditoriaSqlRepository` executam as rotinas correspondentes. `SessaoSqlRepository` consulta a view de sessões em uma leitura; `RelatorioSqlRepository` consulta as views de presença, ocupação e histórico unificado e as funções de taxa e resumo. As triggers são disparadas pelas gravações realizadas pela aplicação e tratam operações com múltiplas linhas.

As operações de agenda utilizam locks transacionais por evento, local e músico, com aquisição em ordem determinística. As procedures verificam os conflitos antes da gravação. A política de locks também é aplicada na exclusão de locais.

A alteração de acesso adquire o lock transacional `AgendaBanda:administradores`, compartilhado com as decisões administrativas de cadastro e recuperação. A verificação do último administrador e a atualização ocorrem na mesma transação. Se outra parte da edição do membro falhar, a mudança de acesso também é desfeita. As permissões e os locks dessas operações dependem do uso das procedures pelos caminhos de escrita da aplicação.

Os agendadores Java chamam as procedures de expiração e limpeza; a instalação não depende de um agendador externo do banco. O instante da operação e os limites de retenção são calculados com o relógio configurado em São Paulo e enviados como parâmetros. As operações JDBC e JPA compartilham a transação; entidades de recuperação alteradas diretamente por SQL são recarregadas antes de continuar o fluxo Java.

Nos relatórios, o período considera a data de início do evento. A taxa de presença fica sem valor quando não existem lançamentos elegíveis; eventos cancelados não compõem esse cálculo.

## Auditoria

Administradores consultam os últimos 100 registros do período na tela **Relatórios**, em **Histórico de operações**. A view `vw_historico_unificado` reúne os históricos de eventos, dados cadastrais e autenticação.

As triggers registram os dados anteriores e novos dos campos de negócio selecionados. Para campos protegidos, usam indicadores como `senha_alterada`, `cpf_alterado`, `email_alterado`, `login_alterado`, `biografia_alterada` e `credencial_alterada`. As alterações são registradas na mesma transação dos dados e desfeitas se a operação sofrer rollback. Os históricos preservam os códigos dos registros excluídos.

Login e logout bem-sucedidos são gravados na mesma transação da criação ou revogação da sessão. Tentativas com credenciais inválidas, recusas de acesso e limites excedidos usam uma transação separada depois de encerrada a tentativa original, preservando o evento mesmo quando ela sofre rollback. Se o banco estiver indisponível para esse registro de recusa, a falha é registrada no log do servidor e a resposta original de acesso negado é mantida.

A auditoria de autenticação guarda apenas códigos internos e categorias fixas de operação, motivo e recurso. Não recebe senha, token, hash, identificador informado, IP, caminho livre ou corpo da requisição. Uma tentativa de login recusada não é atribuída ao titular da conta; seu responsável fica vazio. A consulta inicial anônima ao próprio perfil, sem cookie, não é tratada como tentativa de login.

O histórico de eventos tem retenção padrão de 365 dias; o de operações, incluindo autenticação e recuperações, de 180 dias. A limpeza segue as configurações de retenção e não gera novos registros de auditoria.
