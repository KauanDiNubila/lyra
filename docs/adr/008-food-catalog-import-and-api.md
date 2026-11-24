# ADR-008: Catálogo de alimentos: importação da TACO, busca e contrato da API

- **Data**: 2026-11-19
- **Status**: Proposta, alterada pela [ADR-012](012-user-identity-and-data-ownership.md) (os alimentos personalizados passam a ter dono)
- **Decisores**: Kauan Di Nubila
- **Tags**: alimentos, dados, api

## Contexto e problema

A slice 2 do [RFC-001](../rfc/001-lyra-mvp.md) entrega o catálogo de alimentos: alimentos genéricos brasileiros da TACO ([ADR-006](006-food-data-sources.md)), busca por texto e alimentos personalizados. Três coisas precisaram ser decididas: como a planilha da TACO vira linhas no banco, como a busca se comporta e como é o contrato HTTP (incluindo os erros). Isso também fecha a questão em aberto 3 do RFC.

## Decisão

### Importação da TACO

Fonte: a planilha oficial da TACO publicada pelo NEPA/Unicamp (`tabela-taco-excel`, primeira aba), com 597 alimentos por 100 g. A reprodução é permitida com citação:

> NEPA/UNICAMP. Tabela Brasileira de Composição de Alimentos (TACO). 4. ed. rev. e ampl. Campinas: NEPA/UNICAMP, 2011.

A planilha é convertida uma vez por `tools/taco/convert_taco.py` em `backend/src/main/resources/db/taco/taco-foods.tsv`, que fica no repositório. A migração Java do Flyway `V2__Seed_taco_foods` a insere. Para regenerar:

```bash
python -I tools/taco/convert_taco.py <taco.xlsx> backend/src/main/resources/db/taco/taco-foods.tsv
```

O conversor lê os nutrientes pela posição da coluna, então primeiro confere o nome e a unidade de cada coluna no cabeçalho da planilha (`HEADER` no script) e aborta se alguma for diferente, em vez de guardar em silêncio uma coluna deslocada. Os testes dele rodam com `python -X utf8 -m unittest` dentro de `tools/taco`; eles não fazem parte do CI.

Regras de valor, a partir da legenda da planilha:

| Valor na planilha | Significado na TACO | Guardado como |
|---|---|---|
| número | medido | o número, arredondado em 4 casas decimais |
| `Tr` | traço, abaixo do limite de arredondamento ou de quantificação | `0` |
| `NA` | não se aplica (proteína em óleo, fibra em carne) | `0` |
| vazio | não analisado | desconhecido (`null`) |
| `*` | em reavaliação | desconhecido (`null`) |

- kcal, proteína, carboidrato e gordura são obrigatórios. Um alimento com qualquer um deles desconhecido é **ignorado**: 5 alimentos (iogurte de abacaxi, leite UHT desnatado e integral, cachaça, coco verde). Os usuários podem adicioná-los como alimentos personalizados.
- Fibra e sódio podem ser desconhecidos.
- A TACO calcula o carboidrato por diferença, então o erro de medição produz pequenos valores negativos (4 alimentos, entre -0,045 e 0 g). O carboidrato negativo é guardado como `0`. Qualquer outro valor negativo faz a conversão falhar.
- Resultado: 592 alimentos. Os ids são determinísticos (`UUID` a partir de `taco:<number>`), então todo ambiente tem os mesmos ids.

### Banco de dados

- As colunas de nutrientes e quantidades são `numeric` sem precisão nem escala: o banco nunca arredonda em silêncio; os valores são guardados como o domínio os entrega.
- `search_name` guarda o nome normalizado pelo `SearchText` do domínio (sem acentos, em minúsculas, com espaços simples). Mudar o `SearchText` exige uma migração que recalcule o `search_name`.

### Busca

- A consulta é normalizada como os nomes, a pontuação é removida e ela é dividida em termos; precisa de pelo menos 2 caracteres.
- Um alimento corresponde quando o nome normalizado dele contém **todos** os termos ("feijao cozido" encontra "Feijão, carioca, cozido").
- O adaptador de persistência escapa `%`, `_` e `\` em cada termo antes de montar o `LIKE`, então um termo é sempre texto literal, seja qual for o que o domínio remove.
- Ordem: alimentos personalizados primeiro, depois nomes mais curtos; nomes do mesmo tamanho vêm sem ordem definida, porque o collation do banco decidiria isso e o fake em memória seria diferente. No máximo 20 resultados. A ordem é aplicada pelo banco porque o limite também é, então ela pertence ao adaptador de persistência; o `FoodRepositoryContract` a declara uma vez e roda contra o fake em memória e contra o adaptador real.
- Até o módulo identity existir, os alimentos personalizados são visíveis para todos os usuários.

### Contrato HTTP

| Método | Caminho | Sucesso | Erros |
|---|---|---|---|
| GET | `/api/foods?query=` | `200` lista de alimentos | `400` consulta ausente ou com menos de 2 caracteres |
| POST | `/api/foods` | `201` alimento criado | `400` nome, quantidade, unidade ou nutrientes inválidos, objetos ausentes, JSON malformado |

- Os erros usam **Problem Details** ([RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)), `application/problem+json`, com `status`, `title` e `detail`.
- Toda regra é conferida no domínio, inclusive a ausência do objeto `referenceAmount` ou `nutrients` (sem bean validation no adaptador web). Esses erros têm o título `Invalid input` e a mensagem do domínio como detalhe. Os erros gerados pelo Spring antes de chegar ao domínio (JSON malformado, unidade desconhecida, `query` ausente) mantêm o Problem Details padrão do Spring, com o título `Bad Request`.
- Os números são devolvidos com precisão total; o arredondamento para exibição é feito pelo frontend.
- O documento OpenAPI fica no repositório em `docs/api/openapi.json`. O `OpenApiContractTest` falha quando o documento gerado é diferente, então qualquer mudança de contrato aparece no diff do pull request.
- O springdoc é uma dependência de teste: o documento é gerado no teste e a aplicação não expõe `/v3/api-docs`. O documento declara `application/json` para os sucessos e `application/problem+json` com o schema `ProblemDetail` para o `400`.
- Como a presença é conferida no domínio, o schema deixou de listar campos `required`. Um cliente que lê só o documento vê todo campo como opcional; o servidor continua respondendo `400` quando um falta.

### Consequências

- Bom: alimentos brasileiros disponíveis offline desde a primeira execução, com valores rastreáveis até a fonte.
- Bom: valores desconhecidos continuam desconhecidos em vez de virarem zero.
- Bom: um único formato de erro (Problem Details) para a API inteira, e um único lugar para cada regra.
- Ruim: o contrato não marca os campos obrigatórios. Marcá-los exige `swagger-annotations` no classpath principal (ou o bean validation de volta); rever se aparecer um segundo cliente.
- Ruim: o leite integral e outros quatro alimentos comuns ficam de fora até a TACO reavaliá-los ou o usuário adicioná-los.
- Ruim: a busca `contains` em todos os termos não tem índice; aceitável para centenas de linhas, a rever com um índice trigram quando os alimentos personalizados crescerem.
