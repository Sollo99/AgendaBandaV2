# Backup e restauração

[Voltar ao README](README.md)

## Recursos disponíveis ao administrador

Na aba **Backups**, o administrador pode gerar uma cópia, consultar o histórico, restaurar uma cópia concluída e configurar os backups automáticos.

- **Fazer backup ao iniciar o backend:** vem ligado por padrão e pode ser desligado na tela.
- **Dias e horário:** permite uma execução diária nos dias selecionados, sempre no horário de São Paulo.
- **Histórico:** apresenta operações manuais, agendadas, de inicialização e as cópias de proteção anteriores às restaurações.
- **Download:** fica disponível quando o backend tem acesso à pasta em que o SQL Server grava os arquivos.
- **Restauração:** exige a senha atual do administrador e a confirmação do nome do banco.

O agendamento funciona enquanto o backend está ligado. Um horário vencido é atendido uma vez na próxima verificação disponível; a aplicação não tenta recriar todas as execuções perdidas. Após uma tentativa agendada, a próxima data é avançada mesmo se houver falha, evitando novas tentativas a cada minuto.

## Preparação do servidor

Para a avaliação, não é necessário preencher configurações específicas de backup:

1. Execute os sete scripts de [Instalação](INSTALACAO.md#preparar-o-sql-server), começando pelo `00_create_login.sql`, com uma conta administrativa da instância.
2. Inicie o backend com a conexão normal do projeto.
3. Entre como administrador e abra **Backups**. A geração de uma cópia ao iniciar vem ligada por padrão.

O primeiro script prepara o login `agenda_user` como proprietário apenas da base dedicada `AgendaBanda`. Também cria em `master` uma função para localizar a pasta padrão e uma procedure assinada para inspecionar as cópias. A permissão adicional exigida por essa inspeção fica vinculada ao certificado do módulo; o login da aplicação não recebe `sysadmin`, `dbcreator` ou permissão global para criar bancos.

A assinatura segue o mecanismo de [módulos assinados do SQL Server](https://learn.microsoft.com/en-us/sql/relational-databases/tutorial-signing-stored-procedures-with-a-certificate).

O backend reutiliza servidor, credenciais e opções da conexão normal, abrindo uma conexão independente em `master`. A pasta de backup é obtida do SQL Server; quando a versão não informa uma pasta específica de backup, utiliza a pasta padrão de dados. O serviço SQL Server precisa manter acesso de leitura e gravação nessa pasta.

O download é habilitado automaticamente se essa mesma pasta estiver acessível ao processo Java. Se o SQL Server estiver em outra máquina ou contêiner, geração e restauração continuam no servidor; o download exige compartilhar ou montar a pasta e conceder leitura ao backend.

### Configurações opcionais e produção

As opções comentadas em [backend/.env.example](backend/.env.example) permitem substituir os padrões. Para a avaliação local, normalmente podem permanecer comentadas.

| Variável | Uso |
|---|---|
| `BACKUP_HABILITADO` | Habilita ou desabilita o recurso. Vem habilitado no perfil padrão e desabilitado no perfil `prod`. |
| `BACKUP_BANCO` | Banco alvo, por padrão `AgendaBanda`. Deve coincidir com a conexão da aplicação. |
| `BACKUP_JDBC_URL` | Conexão administrativa alternativa, obrigatoriamente em `master` e na mesma instância. |
| `BACKUP_USUARIO` / `BACKUP_SENHA` | Credenciais alternativas. Vazias, reutilizam as da aplicação. |
| `BACKUP_DIRETORIO_SQL` | Pasta absoluta no servidor SQL, substituindo a detecção automática. |
| `BACKUP_DIRETORIO_DOWNLOAD` | Caminho local ou montagem da mesma pasta, para download pelo Java. |
| `BACKUP_CATALOGO` | Diretório persistente do catálogo, por padrão `data/backup-admin`. |
| `BACKUP_TIMEOUT_SEGUNDOS` | Limite por comando SQL, por padrão 1800 segundos. |

Use barras normais nos caminhos do `.env`. Ao substituir a pasta SQL, as inspeções usam comandos nativos diretamente, e a conta administrativa alternativa deve ter as permissões correspondentes. O módulo assinado da instalação aceita somente arquivos com nome gerado pela aplicação na pasta padrão.

Em produção, o responsável pela instância deve definir conta administrativa, permissões e armazenamento antes de habilitar o recurso explicitamente. A propriedade de uma base existente permite restaurá-la; a associação apenas a `db_owner` não basta. Consulte [RESTORE e permissões](https://learn.microsoft.com/en-us/sql/t-sql/statements/restore-statements-transact-sql?view=sql-server-2017).

## Arquivos e histórico

Os arquivos são gerados com nomes próprios, como `agendabanda_<UUID>.bak`. A interface recebe o código do registro; não recebe nem aceita um caminho arbitrário do servidor.

O catálogo `catalogo.json`, as preferências e o próximo horário ficam no diretório `BACKUP_CATALOGO`. Na execução padrão, dentro de `backend`, o caminho é `backend/data/backup-admin`. Esse diretório deve ser protegido, persistente e incluído no plano de cópias da infraestrutura.

Manter esse catálogo fora da base restaurada preserva a lista de backups e o registro da própria restauração. O catálogo guarda código, responsável quando aplicável, origem, horários, estado, nome do arquivo e resultado. Não guarda senha ou credencial de acompanhamento. Nenhuma tabela adicional é exigida no banco da aplicação.

Use uma única instância do backend com esse catálogo. Um bloqueio de arquivo impede seu uso simultâneo por dois processos; uma trava de sessão no SQL Server também evita duas operações administrativas concorrentes para o mesmo banco.

Não há remoção automática de arquivos ou histórico nesta versão. Monitore espaço em disco e preserve juntos os arquivos e o catálogo. Se um arquivo for movido ou apagado externamente, sua verificação e restauração falharão, mesmo que o registro permaneça no histórico. Cópias feitas fora deste recurso não são importadas automaticamente.

## Geração de backup

A aplicação utiliza `BACKUP DATABASE` com `COPY_ONLY` e `CHECKSUM`, seguido de `RESTORE VERIFYONLY` e conferência do cabeçalho. O resultado só aparece como concluído depois dessas verificações.

Esses comandos usam uma conexão administrativa exclusiva, fora das transações JPA. O SQL Server não permite BACKUP dentro de transação explícita ou implícita. Consulte [BACKUP](https://learn.microsoft.com/en-us/sql/t-sql/statements/backup-transact-sql?view=sql-server-ver17).

A geração normal não bloqueia os usuários pelo modo de manutenção. Sua duração e uso de disco dependem do banco e da infraestrutura.

## Restauração pela interface

1. Entre com um administrador ativo e abra **Backups**.
2. Localize uma operação de backup concluída e selecione **Restaurar**.
3. Confira a data, o arquivo e o aviso de substituição dos dados.
4. Feche conexões externas com esse banco, inclusive consultas abertas por outros clientes SQL.
5. Informe sua senha atual e digite o nome do banco exatamente como apresentado.
6. Confirme e acompanhe o resultado na tela.

O sistema verifica arquivo, banco de origem, identificação da cópia e assinatura da estrutura. Backups com estrutura diferente da instalação atual exigem restauração assistida pelo responsável pelo banco.

Após a verificação, o backend bloqueia novos acessos à API, espera as requisições em andamento e pausa as tarefas periódicas. Cria uma cópia de proteção do estado atual, suspende o pool e fecha suas conexões antes de executar o RESTORE pela conexão em `master`.

Os arquivos lógicos da cópia são mapeados para os arquivos físicos da base atual. A restauração utiliza acesso exclusivo e substitui a base configurada; não permite escolher outro banco pela interface.

Ao final, o sistema confirma a disponibilidade e a estrutura restaurada, remove as sessões e cancela autorizações de recuperação de senha anteriores. Todos devem entrar novamente com as contas e senhas existentes no backup selecionado.

O histórico externo registra o resultado. A cópia anterior à restauração também fica disponível no histórico, permitindo outra restauração quando necessário. Acompanhamento é restrito à operação iniciada, com credencial temporária mantida apenas em memória no navegador; ela não permite consultar outras operações nem executar ações.

Requisições comuns durante a restauração recebem HTTP 503 com `BANCO_EM_MANUTENCAO`. O acompanhamento da operação continua disponível para quem iniciou a ação.

## Falhas e interrupções

- Se a verificação ou a cópia de proteção falhar antes de alterar o banco, a restauração é abortada e os acessos são liberados.
- Se houver falha depois de iniciar a alteração da base, a aplicação permanece em manutenção. O responsável pelo SQL Server deve verificar o estado do banco.
- Um marcador persistente impede que reiniciar o backend elimine essa pendência. Na inicialização seguinte, o backend só libera o acesso depois de verificar a base, sua estrutura e invalidar credenciais antigas.
- Operações interrompidas pelo encerramento do processo são registradas como interrompidas. Evite encerrar ou atualizar o backend enquanto houver uma restauração em andamento.
- Desativar o recurso não deve ser usado para contornar uma restauração pendente. Preserve o catálogo e restaure a disponibilidade da base primeiro.

Se o banco permanecer em `SINGLE_USER` após uma falha, o responsável pode conectar-se a `master`, identificar a operação interrompida e, após avaliar o estado da base, retornar o acesso com:

```sql
-- Exemplo para o nome padrao; executar somente depois de conferir o estado do banco.
ALTER DATABASE [AgendaBanda] SET MULTI_USER WITH ROLLBACK IMMEDIATE;
```

Esse comando não recupera uma base em estado `RESTORING` nem substitui a conclusão de um RESTORE. Nesses casos, a recuperação deve ser conduzida pelo responsável da instância. Após a correção, reinicie o backend para que a verificação seja repetida.

## Roteiro de demonstração

1. Execute os scripts de instalação, inicie o backend e mostre a cópia gerada automaticamente.
2. Desative essa preferência pela interface, salve e reinicie para comprovar a persistência.
3. Reative-a e configure um dia/horário próximo com o backend ligado.
4. Crie um evento identificável e gere um backup manual.
5. Edite esse evento e restaure a cópia anterior.
6. Entre novamente e confirme a recuperação do evento e dos demais registros.
7. Mostre o histórico da restauração e sua cópia de proteção.
8. Se a pasta estiver acessível ao backend, baixe um arquivo pelo histórico.

O bônus do enunciado exige demonstrar backup e restauração no SQL Server. A verificação de arquivo complementa essa demonstração; ela não substitui uma restauração real. Consulte [orientações de backup e restore da Microsoft](https://learn.microsoft.com/en-us/sql/relational-databases/backup-restore/back-up-and-restore-of-sql-server-databases/).
