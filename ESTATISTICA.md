# Estatística com dados simulados

[Voltar ao README](README.md)

Este roteiro atende aos itens de tabelas e gráficos, distribuição de frequências,
medidas de posição, dispersão e regressão linear da etapa de Estatística.
A entrega é uma **planilha Excel funcional**, com fórmulas, gráficos e interpretação.
As consultas SQL fornecem os dados e permitem conferir os cálculos.

## Origem e limites dos dados

Todos os registros da carga são **simulados**, sem representar atividades ou resultados reais
da banda. Identifique essa origem na planilha e no relatório.

O [script 06](database/06_insert_data.sql) mantém as contas de demonstração e gera:

- 120 eventos de janeiro a dezembro de 2026, identificados pelo prefixo `SIM-2026-`.
- Volumes mensais de 6 a 14 eventos, distribuídos entre ensaios, shows e reuniões.
- 94 eventos realizados, 17 agendados e 9 cancelados.
- 702 participações, com 2 a 10 músicos por evento; reuniões têm de 2 a 6.
- Presenças, ausências, confirmações, recusas e participações sem registro de presença.
- Durações programadas variadas. Nos eventos realizados, vão de 20 a 255 minutos.

O cenário tem como referência **01/11/2026**: os eventos não cancelados de janeiro a outubro
estão realizados; os de novembro e dezembro estão agendados. As datas são fixas e não
mudam ao instalar em outro dia. Os eventos não se sobrepõem.

`duracao_minutos` mede a **duração programada**, calculada pelo início e fim cadastrados.
O sistema não mede a duração efetiva. `participantes_previstos` conta todos os músicos
vinculados, inclusive os que recusaram ou ainda não responderam; não é presença efetiva.

A duração foi gerada a partir da quantidade prevista de músicos, do tipo do evento e
de uma variação adicional. A relação serve para praticar regressão com dados reproduzíveis;
não é uma descoberta sobre a banda nem demonstra causalidade.

## Preparar e extrair a base

1. Prepare uma base de demonstração seguindo os [scripts 00 a 06](INSTALACAO.md#preparar-o-sql-server).
   A carga `06` exige uma base vazia e bloqueia a repetição para preservar registros existentes.
   **O script 01 remove os dados anteriores**; não o execute sobre uma base que precise preservar.
2. No SSMS, abra [07_consultas_estatistica.sql](database/07_consultas_estatistica.sql) e execute
   o arquivo inteiro. Ele consulta os dados e usa apenas uma tabela temporária na sessão.
3. Para cada resultado, use **Copiar com Cabeçalhos** e cole no Excel, nas abas abaixo.
   Campos SQL `NULL` devem ficar vazios no Excel; não os transforme em zero.
4. Selecione os dados da aba `Eventos`, pressione **Ctrl+T**, marque que a tabela tem cabeçalhos
   e dê à tabela o nome `Eventos`. Faça o mesmo com o resultado de regressão, usando o nome `Regressao`.
5. Salve como `.xlsx`. Confira se as durações, contagens e percentuais foram importados como números.

| Resultado SQL | Aba | Conteúdo e uso |
| --- | --- | --- |
| 1 | `BaseGeral` | 120 eventos: calendário completo e identificação da origem. |
| 2 | `Eventos` | 94 realizados: base das medidas de posição e dispersão. |
| 3 | `Mensal` | 12 meses: gráfico de linhas de `total_eventos`. |
| 4 | `Tipos` | Frequência dos tipos entre os realizados: barras e setores. |
| 5 | `Frequencias` | Durações dos realizados em intervalos de 30 minutos: histograma. |
| 6 | `Regressao` | 57 ensaios realizados: músicos previstos × duração programada. |

Os números acima correspondem à carga original, antes de alterações pela aplicação.
O script permite mudar `@inicio` e `@fim`; os dois dias entram na consulta.
Por padrão, `@somente_simulados=1` seleciona apenas os eventos com prefixo `SIM-2026-`.
O valor `0` inclui também outros eventos; nesse caso, revise a descrição da origem na planilha.
Preserve o prefixo dos eventos de demonstração para que continuem sendo selecionados.

## Fórmulas e gráficos no Excel

Crie uma aba `Estatisticas`. Com o Excel em português do Brasil e a tabela `Eventos`, use:

| Medida | Fórmula sobre a duração programada | Unidade |
| --- | --- | --- |
| Quantidade | `=CONT.NÚM(Eventos[duracao_minutos])` | eventos |
| Média | `=MÉDIA(Eventos[duracao_minutos])` | minutos |
| Mediana | `=MED(Eventos[duracao_minutos])` | minutos |
| Moda | `=MODA.UNI(Eventos[duracao_minutos])` | minutos |
| Amplitude | `=MÁXIMO(Eventos[duracao_minutos])-MÍNIMO(Eventos[duracao_minutos])` | minutos |
| Variância amostral | `=VAR.A(Eventos[duracao_minutos])` | minutos² |
| Desvio padrão amostral | `=DESVPAD.A(Eventos[duracao_minutos])` | minutos |
| Coeficiente de variação | `=DESVPAD.A(Eventos[duracao_minutos])/MÉDIA(Eventos[duracao_minutos])` | formatar como percentual |

Este roteiro usa a convenção **amostral** para variância e desvio padrão, com divisor `n-1`.
Se a disciplina tratar o conjunto completo da simulação como população, use `VAR.P` e
`DESVPAD.P`, inclusive no coeficiente de variação, e explique a escolha. Os valores de conferência
abaixo usam a convenção amostral. A simulação não é uma amostra de observações reais.

Para manter as frequências funcionais na planilha, recalcule-as a partir da tabela `Eventos`:

- Na aba `Tipos`, com o nome do tipo em `A2`, use `=CONT.SE(Eventos[tipo];A2)`
  para a frequência absoluta. Se o resultado estiver em `B2`, use
  `=B2/CONT.NÚM(Eventos[duracao_minutos])` para a relativa e formate como percentual.
- Na aba `Frequencias`, com o limite inferior em `A2` e superior em `B2`, use
  `=CONT.SES(Eventos[duracao_minutos];">="&A2;Eventos[duracao_minutos];"<"&B2)`.
  As faixas são `[inferior, superior)`: 120 minutos pertence à faixa de 120 a menos de 150.
  Não use um histograma com a coluna de frequências como se ela fosse a observação original.
  Use as durações da tabela `Eventos` em um gráfico de histograma ou represente as faixas
  já contadas em colunas contíguas, sem espaçamento.
- Os percentuais SQL já vêm na escala de 0 a 100. Para aplicar o formato percentual do Excel
  diretamente, divida-os por 100; as fórmulas acima já produzem a escala de 0 a 1.

Faça os gráficos de linha com `Mensal`, de barras e setores com `Tipos` e o histograma das durações.
Identifique títulos, eixos, unidades, período e origem simulada.

## Regressão linear e dispersão

Use somente a tabela `Regressao`, que contém ensaios realizados. Separar a categoria evita
misturar as diferenças de duração próprias de shows, reuniões e ensaios.

- **X:** `x_participantes`, quantidade de músicos previstos.
- **Y:** `y_duracao_minutos`, duração programada em minutos.
- Insira um gráfico de **dispersão XY**, com X e Y nas posições acima.
- Adicione uma linha de tendência **linear**, exibindo a equação e o R², com intercepto livre.
- Calcule a correlação com `=CORREL(Regressao[x_participantes];Regressao[y_duracao_minutos])`.
- Para esse modelo linear com intercepto, o R² também pode ser conferido com
  `=CORREL(Regressao[x_participantes];Regressao[y_duracao_minutos])^2`.

O coeficiente **r** indica o sentido e a força da relação linear. O **R²** indica a proporção
da variação de Y explicada pelo ajuste; ele não indica o sentido da relação. Essa distinção
corrige a descrição imprecisa de R² presente no enunciado.

Explique o significado do coeficiente angular, o ajuste e as limitações da simulação.
Se incluir uma previsão, restrinja-a à faixa observada de 2 a 10 músicos e identifique-a
como estimativa da duração programada neste cenário simulado.

## Valores para conferir a carga original

Os cálculos sobre os 94 eventos realizados devem resultar aproximadamente em:

| Medida | Valor |
| --- | --- |
| Média | 138,3511 minutos |
| Mediana | 135 minutos |
| Moda | 150 minutos |
| Amplitude | 235 minutos |
| Variância amostral | 2.813,6496 minutos² |
| Desvio padrão amostral | 53,0438 minutos |
| Coeficiente de variação amostral | 38,3400% |

Na regressão dos 57 ensaios, a reta é aproximadamente **Y = 52,5006 + 13,9662 × X**,
com **r = 0,8682** e **R² = 0,7538**. Use esses valores para conferir suas fórmulas e seu
gráfico; mantenha os cálculos na planilha em vez de digitar apenas os resultados.

Finalize com uma aba `Interpretacao`, explicando os resultados, a diferença entre duração
programada e efetiva, a separação dos eventos por situação e o caráter simulado da base.
Uma taxa de presença sem lançamentos é indefinida, não zero; presenças de eventos agendados
ou cancelados não devem entrar na análise de comparecimento.
