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
        int codigo_administrador
        DateTime decidido_em
        DateTime concluido_em
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
```

O membro possui um único perfil operacional: `MEMBRO`, `MUSICO` ou `PRODUTOR`. Os subtipos usam uma FK composta `(codigo, perfil)` para `membros`, com `perfil` fixo no respectivo subtipo. Isso impede que o mesmo código esteja nas duas tabelas de perfil. O indicador `administrador` é independente dessa classificação.

`ParticipacaoEvento` é o único vínculo de músicos com qualquer evento, incluindo ensaios. A chave composta impede duplicação do mesmo músico no mesmo evento. Um índice filtrado garante uma única solicitação de senha aberta (`PENDENTE` ou `APROVADA`) por membro.

Membros são desativados para preservar seus vínculos. Locais com qualquer evento vinculado não podem ser excluídos, mesmo se o evento estiver cancelado ou realizado.
