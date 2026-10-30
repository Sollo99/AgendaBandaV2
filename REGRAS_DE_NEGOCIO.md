# Regras de negócio

[Voltar ao README](README.md)

## Eventos e participações

- Os horários da agenda seguem o fuso de São Paulo (`America/Sao_Paulo`). As respostas da API incluem offset explícito; formulários e calendário exibem esse mesmo fuso, independentemente do navegador.
- Eventos possuem os estados `AGENDADO`, `REALIZADO` e `CANCELADO`.
- O horário final deve ser posterior ao inicial. Um evento só pode ser marcado como realizado após o término.
- Eventos não cancelados reservam o local e os músicos vinculados. Sobreposições são rejeitadas; horários consecutivos são permitidos.
- Cada músico tem uma única participação por evento, independentemente de ser ensaio, show ou reunião.
- Ao editar um evento, participantes mantidos conservam confirmação, presença, observação e data de resposta. Novos participantes começam como `PENDENTE`.
- O músico pode responder somente pela própria participação, antes do início de um evento agendado.
- O administrador registra presença ou ausência após o início. Presença positiva exige confirmação.
- Registrar ausência preserva o status de confirmação e a data de resposta. A presença de um músico já confirmado também preserva sua data de resposta; confirmar e marcar presença é uma ação explícita para quem ainda não confirmou.
- Eventos realizados não podem ser reabertos ou excluídos. Registros de presença ou ausência também impedem a exclusão do evento e a remoção do participante correspondente.
- Um evento com presença registrada não pode ser cancelado ou reagendado para o futuro.

## Membros e cadastros

- Cada membro possui um único perfil operacional; a permissão administrativa é concedida separadamente.
- No próprio perfil, músicos editam instrumento e biografia e produtores editam a especialidade. A edição não altera o perfil operacional nem a permissão administrativa.
- A desativação de um membro preserva seus vínculos. O último administrador ativo não pode ser desativado nem perder sua permissão.
- Músicos inativos não ficam disponíveis para novas inclusões em eventos; suas participações existentes são preservadas.
- Locais vinculados a eventos não podem ser excluídos.
- Cidades são únicas pela combinação de nome e UF, sem distinção entre maiúsculas e minúsculas.
- CPF, e-mail, login, senhas e limites de tamanho são validados. Senhas novas exigem ao menos seis caracteres e no máximo 72 bytes em UTF-8.
- Um administrador pode liberar CPF, e-mail e login de um cadastro recusado para um novo pedido, preservando o registro da decisão.
