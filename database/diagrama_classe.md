# Diagrama de classe

```mermaid
classDiagram
    class Membro {
        int codigo
        String nome
        String cpf
        String email
        String login
        String senha
        Bit administrador
        Bit ativo
        String perfil
        String status_cadastro
        DateTime cadastro_decidido_em
        int cadastro_decidido_por
        DateTime identificadores_liberados_em
        int identificadores_liberados_por
        DateTime criado_em
    }

    class Musico {
        int codigo
        String perfil
        String instrumento
        String biografia
    }

    class Produtor {
        int codigo
        String perfil
        String especialidade
    }

    class Cidade {
        int codigo_cidade
        String nome
        char uf
    }

    class Local {
        int codigo_local
        String nome
        String endereco
        String complemento
        int codigo_cidade
    }

    class Agenda {
        int codigo_agenda
        String titulo
        String descricao
        DateTime dt_hora_in
        DateTime dt_hora_fim
        int codigo_local
        int cod_membro_criador
        int codigo_tipo
        String status
        DateTime criado_em
        DateTime atualizado_em
    }

    class Ensaio {
        int codigo_agenda
        String repertorio
        String observacao
    }

    class Show {
        int codigo_agenda
        int codigo_cidade
    }

    class Reuniao {
        int codigo_agenda
        String pauta
    }

    class TipoEvento {
        int codigo_tipo
        String nome
        String descricao
        String categoria_detalhe
        Bit ativo
    }

    class ParticipacaoEvento {
        int codigo_agenda
        int cod_musico
        String status_confirmacao
        Bit presente
        DateTime data_resposta
        String observacao
    }

    class RedefinicaoSenha {
        int codigo_redefinicao
        int codigo_membro
        String status
        String chave_hash
        DateTime solicitado_em
        DateTime expira_em
        int codigo_administrador
        DateTime decidido_em
        DateTime concluido_em
        DateTime encerrado_em
        String motivo_encerramento
    }

    class SessaoPersistida {
        int codigo_sessao
        String token_hash
        int codigo_membro
        DateTime criado_em
        DateTime expira_em
    }

    Membro "1" --> "0..1" Musico : possui perfil
    Membro "1" --> "0..1" Produtor : possui perfil
    Membro "1" --> "0..*" Agenda : cria
    Cidade "1" --> "0..*" Local : possui
    Cidade "1" --> "0..*" Show : recebe
    Local "1" --> "0..*" Agenda : sedia
    TipoEvento "1" --> "0..*" Agenda : classifica
    Agenda "1" --> "0..1" Ensaio : detalha
    Agenda "1" --> "0..1" Show : detalha
    Agenda "1" --> "0..1" Reuniao : detalha
    Agenda "1" --> "0..*" ParticipacaoEvento : registra
    Musico "1" --> "0..*" ParticipacaoEvento : participa
    Membro "1" --> "0..*" RedefinicaoSenha : solicita
    Membro "0..1" --> "0..*" RedefinicaoSenha : decide
    Membro "1" --> "0..*" SessaoPersistida : autentica
    Membro "0..1" --> "0..*" Membro : analisa cadastro
    Membro "0..1" --> "0..*" Membro : libera identificadores
```

O membro possui um único perfil operacional: `MEMBRO`, `MUSICO` ou `PRODUTOR`. Os subtipos usam uma FK composta `(codigo, perfil)` para `membros`, com `perfil` fixo no respectivo subtipo. Isso impede que o mesmo código esteja nas duas tabelas de perfil. O indicador `administrador` é independente dessa classificação.

`ParticipacaoEvento` é o único vínculo de músicos com qualquer evento, incluindo ensaios. A chave composta impede duplicação do mesmo músico no mesmo evento. Um índice filtrado garante uma única solicitação de senha aberta (`PENDENTE` ou `APROVADA`) por membro.

Membros são desativados para preservar seus vínculos. Locais com qualquer evento vinculado não podem ser excluídos, mesmo se o evento estiver cancelado ou realizado.

`Cidade` tem restrição única por `(nome, uf)`, sem distinção de caixa. `TipoEvento.categoria_detalhe` aceita somente `EVENTO`, `ENSAIO`, `SHOW` e `REUNIAO`. A participação só admite presença positiva quando confirmada, protegida por CHECK. A validação do horário/status para registrar presença e marcar um evento como realizado pertence aos serviços Java.

O cadastro público começa `PENDENTE`, inativo e sem administração. Somente uma decisão administrativa o torna `APROVADO`; `RECUSADO` continua sem acesso. A restrição SQL impede conta não aprovada de ficar ativa ou administrativa. O responsável e a data da decisão são preservados em `membros`.

Depois da recusa, um administrador pode liberar conjuntamente CPF, e-mail e login: os três ficam nulos e a liberação registra responsável/data, preservando a recusa original. A restrição SQL permite campos nulos apenas nesse estado. Índices únicos filtrados ignoram os nulos e mantêm os identificadores reservados únicos. O registro liberado não volta a ser aprovado; é necessário um novo cadastro.

Uma recuperação fica aberta somente em `PENDENTE` ou `APROVADA`, sempre com `expira_em`. Encerramentos usam `CONCLUIDA`, `RECUSADA`, `EXPIRADA` ou `CANCELADA`, com data e motivo. O índice filtrado deixa de abranger o pedido encerrado e permite uma nova solicitação.
