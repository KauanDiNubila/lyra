# ADR-009: Diário: entradas, cálculo e contrato da API

- **Data**: 2026-11-19
- **Status**: Proposta, alterada em dois pontos pela [ADR-011](011-diet-plan-and-diary-from-plan.md) (quantidade zero para itens do plano e troca de alimento e de refeição) e pela [ADR-012](012-user-identity-and-data-ownership.md) (o diário passa a ter dono)
- **Decisores**: Kauan Di Nubila
- **Tags**: diário, domínio, dados, api

## Contexto e problema

A slice 3 do [RFC-001](../rfc/001-lyra-mvp.md) entrega o backend do diário: registrar alimentos por data e refeição, totais por refeição e por dia, editar e remover entradas. Os alimentos vêm do catálogo da [ADR-008](008-food-catalog-import-and-api.md) e cada entrada guarda uma cópia do alimento ([ADR-003](003-nutrient-snapshot-on-log.md)). Ainda era preciso decidir: o que são um "dia" e uma "refeição", como os macros são calculados, como as entradas são ordenadas e como é o contrato HTTP.

## Decisão

### Dia e refeição

- Um **dia é derivado**: `GET /api/diary/{date}` lê as entradas daquela data. Não existe tabela de dias, e uma data sem entradas é um dia vazio (`200`, sem refeições, totais zerados).
- Uma **refeição é um nome**: texto livre de 1 a 50 caracteres (sem espaços nas pontas) guardado em cada entrada. O dia agrupa as entradas pelo nome exato, na ordem da primeira entrada de cada refeição. Os nomes diferenciam maiúsculas de minúsculas: "Almoço" e "almoço" são duas refeições.
- O cliente envia a data; o servidor nunca decide o que é "hoje".
- As entradas não têm dono até o módulo identity existir, a mesma regra dos alimentos personalizados ([RFC-001](../rfc/001-lyra-mvp.md), questão em aberto 1).

### Entrada e snapshot

Uma entrada guarda o id, a data, o nome da refeição, o `foodId` e o snapshot do alimento: nome, quantidade de referência e nutrientes. A unidade da quantidade precisa ser a unidade da quantidade de referência do alimento, e a quantidade precisa ser maior que zero; do contrário o domínio rejeita a entrada. Depois de criada, só a quantidade pode mudar. Trocar a refeição ou o alimento ainda não é suportado.

`diary_entry.food_id` é uma chave estrangeira para `food` sem cascade, então um alimento não pode ser apagado enquanto uma entrada do diário apontar para ele. Hoje não existe como apagar um alimento.

### Cálculo

- Macros de uma entrada = `nutriente × quantidade ÷ quantidade de referência`, para kcal, proteína, carboidrato e gordura. A multiplicação vem primeiro e é exata; há uma única divisão, com `MathContext.DECIMAL128` (34 dígitos).
- Total da refeição = soma das entradas dela; total do dia = soma das refeições. Fibra e sódio ficam no snapshot, mas não são totalizados.
- Nada é arredondado no backend nem na API. O frontend arredonda para exibir, como diz o RFC.

### Ordem

As entradas voltam na ordem em que foram registradas. A tabela tem uma coluna `entry_order`, `generated always as identity`, que só cresce, não tem empates e não depende do relógio. Mudar a quantidade de uma entrada mantém o lugar dela.

### Contrato HTTP

| Método | Caminho | Sucesso | Erros |
|---|---|---|---|
| GET | `/api/diary/{date}` | `200` dia com refeições, entradas e totais | `400` data malformada |
| POST | `/api/diary/{date}/entries` | `201` entrada | `404` alimento desconhecido; `400` nome de refeição, quantidade ou unidade inválidos, `foodId` ou `quantity` ausentes |
| PATCH | `/api/diary/entries/{id}` | `200` entrada | `404` entrada desconhecida; `400` quantidade ou unidade inválidas, `quantity` ausente |
| DELETE | `/api/diary/entries/{id}` | `204` | `404` entrada desconhecida |

- Os erros usam Problem Details, como na ADR-008. As violações de regra têm o título `Invalid input`; um alimento ou uma entrada inexistente tem o título `Not found`, com a mensagem da exceção como detalhe. Os erros gerados pelo Spring antes de chegar ao domínio (data ou JSON malformados, unidade desconhecida) mantêm o `Bad Request` padrão.
- A exclusão é permanente e responde `404` para um id que não existe, para o cliente conseguir distinguir uma exclusão repetida de uma bem-sucedida.
- O adaptador web do diário repete duas pequenas partes do adaptador de `food`: o `QuantityJson` e o tratamento do `InvalidInputException`. A [ADR-001](001-hexagonal-modular-monolith.md) proíbe que um módulo use o adaptador de outro, então a cópia é aceita por enquanto. O limite já foi atingido pelo próprio diário, cujo domínio lança o `InvalidInputException` e cujo adaptador web trata o `FoodNotFoundException`, ambos do domínio de `food`. A correção é o núcleo compartilhado da [ADR-010](010-shared-kernel-for-errors.md), que guarda a exceção e um handler global e remove as duas cópias; ela entra num pull request próprio.
- O documento OpenAPI `docs/api/openapi.json` foi ampliado com os três caminhos do diário; nenhum caminho de `food` mudou.

### Consequências

- Bom: sem tabela de dias, então não há dia para criar, travar nem manter em sincronia com as entradas.
- Bom: os dias passados nunca mudam quando um alimento muda, e o cálculo pode ser reproduzido só a partir da entrada.
- Bom: uma decisão para a ordem e uma para o arredondamento, cada uma declarada uma vez.
- Ruim: os nomes de refeição são texto livre e diferenciam maiúsculas, então um erro de digitação cria uma refeição nova. Uma lista fixa de refeições, ou as refeições do plano, resolveriam isso; a slice 4 vai rever.
- Ruim: só a quantidade pode mudar. Mover uma entrada para outra refeição ou trocar o alimento dela significa apagar e registrar de novo até a slice 4.
- Ruim: a API devolve decimais longos como `0.3333333333333333333333333333333333`, e todo cliente precisa arredondar.
- Ruim: o diário ainda não é pré-preenchido pelo plano; o dia fica vazio até as entradas serem registradas.
