# Configuração e segurança

[Voltar ao README](README.md)

O arquivo [backend/.env.example](backend/.env.example) reúne as opções de conexão, acesso e retenção. As configurações mais relevantes são:

| Configuração | Uso |
| --- | --- |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` e `DB_PASSWORD` | Conexão com o SQL Server. |
| `DB_URL` | URL JDBC completa para configuração personalizada da conexão. |
| `SERVER_PORT` | Porta do backend; padrão `8080`. |
| `CORS_ALLOWED_ORIGINS` | Origens autorizadas a acessar a API; padrão `http://localhost:5173`. |
| `AUTH_COOKIE_SECURE` | Use `true` ao publicar em HTTPS; o valor `false` atende ao desenvolvimento local em HTTP. |
| `SPRING_PROFILES_ACTIVE` | O perfil `prod` força cookies `Secure`, inclusive atrás de proxy HTTPS. |
| `FORWARD_HEADERS_STRATEGY` e `PROXY_IPS_REGEX` | Configuração dos cabeçalhos encaminhados por um proxy confiável. |
| `AUTH_RECUPERACAO_PENDENTE_HORAS` e `AUTH_RECUPERACAO_APROVADA_HORAS` | Validade do pedido e do código de recuperação. |
| `AUDITORIA_EVENTOS_DIAS` e `AUDITORIA_OPERACOES_DIAS` | Retenção dos históricos; padrões de 365 e 180 dias. |

- Senhas são armazenadas como hash BCrypt. Tokens de sessão e códigos de recuperação também têm somente seus hashes persistidos.
- A sessão tem duração de oito horas e utiliza cookie `HttpOnly` com `SameSite=Strict`. Operações de escrita exigem token CSRF, preparado pelo frontend.
- O login tem limitação por IP e por falhas da combinação conta/IP. Cadastro e recuperação possuem limites por IP, pela combinação identificador/IP e um teto global próprio; excesso retorna HTTP 429. Um IP não esgota o limite por identificador de outras origens.
- Alterar a senha encerra as sessões do membro e invalida autorizações de recuperação abertas.
- Alterações administrativas adquirem um lock transacional compartilhado entre as instâncias da aplicação para proteger a existência do último administrador ativo.
- Relatórios e históricos exigem permissão administrativa. O histórico de membros omite CPF, e-mail, login e credenciais; campos livres de eventos podem conter informações inseridas pelos usuários.
- A limpeza dos históricos e das sessões é feita por procedures em lotes, com agendamento Java. Os históricos respeitam os prazos configurados, sem arquivamento automático.

Os contadores de tentativas são mantidos em memória para a execução em uma única instância. Ao reiniciar a aplicação, esses contadores são reiniciados; uma implantação com múltiplas instâncias precisa de controle compartilhado no proxy ou em um armazenamento comum.

Em uma publicação, altere as credenciais de demonstração, utilize HTTPS, ative `SPRING_PROFILES_ACTIVE=prod` e configure a origem real do frontend. Para acesso direto ao backend, mantenha `FORWARD_HEADERS_STRATEGY=NONE`. Se houver proxy reverso, configure `NATIVE` e restrinja `PROXY_IPS_REGEX` aos IPs confiáveis do proxy, que deve controlar os cabeçalhos encaminhados.
Ao publicar, utilize uma conta de banco com permissões limitadas às operações da aplicação. O script `00` concede `db_owner` para a preparação local; essa permissão deve ser revista na implantação. Configure `DB_URL` com validação do certificado do SQL Server, de acordo com a infraestrutura utilizada.

Prefira servir a interface e encaminhar `/api` pela mesma origem. Cookies `SameSite=Strict` não atendem a uma interface e uma API em sites diferentes apenas por habilitar CORS.
