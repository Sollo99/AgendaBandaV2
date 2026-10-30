# Guia de uso

[Voltar ao README](README.md)

## Sumário

- [Contas de demonstração](#contas-de-demonstração)
- [Editar o próprio perfil](#editar-o-próprio-perfil)
- [Cadastro de uma conta](#cadastro-de-uma-conta)
- [Recuperação de senha](#recuperação-de-senha)
- [Roteiro de avaliação](#roteiro-de-avaliação)

## Contas de demonstração

Após executar os scripts de preparação, estas contas estão disponíveis:

| Login | Senha | Acesso |
| --- | --- | --- |
| `admin` | `admin` | Administração e perfil de músico. |
| `usuario` | `usuario` | Músico sem permissão administrativa. |
| `teste` | `teste` | Músico sem permissão administrativa. |

Essas são **credenciais da aplicação**. O login `agenda_user` é exclusivo da conexão com o banco.

Na tela inicial, entre com uma das contas. A navegação apresenta **Agenda** e **Meu perfil**; administradores também acessam **Usuários**, **Locais**, **Cidades**, **Tipos de evento**, **Relatórios** e as solicitações de recuperação de senha.

## Editar o próprio perfil

Em **Meu perfil**, cada membro pode editar seus dados pessoais e alterar a senha. Músicos também editam instrumento e biografia; produtores editam a especialidade. Essas alterações preservam o perfil operacional e as permissões da conta.

## Cadastro de uma conta

1. Selecione **Criar conta**.
2. Informe os dados pessoais, as credenciais e o instrumento.
3. Entre como administrador e analise o pedido na tela **Usuários**.
4. Após a aprovação, o membro poderá entrar com seu login e senha.

O cadastro público cria um músico sem permissão administrativa. Contas pendentes ou recusadas não acessam a agenda.

## Recuperação de senha

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

Use **Marcar ausente** para registrar ausência diretamente, preservando a confirmação do músico. **Marcar presente** mantém uma confirmação existente; **Confirmar e marcar presente** confirma quem ainda não confirmou. O calendário e os detalhes exibem o nome cadastrado do tipo de evento; a cor corresponde à sua categoria de detalhe.
