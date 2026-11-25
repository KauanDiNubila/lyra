# ADR-016: Registro de peso

- **Data**: 2026-11-25
- **Status**: Proposta
- **Decisores**: Kauan Di Nubila
- **Tags**: peso, api, dados

## Contexto e problema

A slice 7 do [RFC-001](../rfc/001-lyra-mvp.md) é o registro do peso corporal (objetivo 8). O RFC prevê o módulo `weight` e rascunha as rotas `GET` e `POST /api/weights`. Faltam as regras: quantos pesos por dia, que valores são aceitos, como corrigir um erro, e como o peso entra na exportação e na exclusão da conta da [ADR-012](012-user-identity-and-data-ownership.md).

## Fatores de decisão

1. Registrar o peso tem de ser um gesto só, sem pensar em criar ou editar.
2. Um erro de digitação precisa ser fácil de corrigir.
3. Valores impossíveis não entram no banco.
4. O peso é um dado do usuário: só ele o vê, ele sai na exportação e some ao apagar a conta.
5. Nenhuma dependência nova.

## Decisão

### Um peso por dia

- O usuário tem **no máximo um peso por data**. Uma pesagem por dia é o que um diário de dieta precisa para ver a tendência. Mais de uma pesagem no mesmo dia exigiria escolher qual vale.
- Registrar o peso de uma data que já tem peso **troca** o valor. Por isso a rota é `PUT /api/weights/{date}`, com o corpo `{"kilograms": 82.4}`, e responde `200` com o peso guardado. É idempotente: repetir a mesma chamada dá o mesmo resultado. Isso troca o `POST` rascunhado no RFC, porque o `POST` sugere criar um recurso novo a cada chamada.
- `GET /api/weights` lista todos os pesos do usuário, da data mais antiga para a mais nova. Um registro por dia dá cerca de 365 linhas por ano, então a lista inteira cabe numa resposta, sem paginação nem filtro de período. Um filtro entra quando a tela precisar.
- `DELETE /api/weights/{date}` apaga o peso da data e responde `204`. Uma data sem peso responde `404`.

### Valores aceitos

- O peso é em **quilogramas**, entre **20 e 500**, com **no máximo duas casas decimais**. Fora disso a resposta é `400` com Problem Details, como os outros erros de entrada ([ADR-010](010-shared-kernel-for-errors.md)). Os limites só barram erros de digitação, como 8240 em vez de 82,40. O valor é guardado como veio, sem arredondar.
- A data pode ser qualquer uma, inclusive futura, como no diário. Recusar datas futuras exigiria saber o fuso do usuário, e o ganho é pequeno.

### Módulo e banco

- O módulo `weight` segue a [ADR-001](001-hexagonal-modular-monolith.md): o domínio tem o `WeightEntry` (um record com a data e os quilogramas, que valida os valores), e a aplicação tem os casos de uso `RecordWeightUseCase`, `ListWeightsUseCase` e `DeleteWeightUseCase` e a porta `WeightRepository`.
- A persistência segue a [ADR-005](005-backend-stack.md): uma entidade JPA com chave composta (`user_id` e `entry_date`), leitura e exclusão por JPQL, e o upsert como consulta nativa, porque o JPA não tem `on conflict`.
- A tabela `weight_entry` tem a chave primária `(user_id, entry_date)` e `user_id` com `on delete cascade` para `app_user`, então apagar a conta apaga os pesos, e o teste que vigia as chaves estrangeiras cobre a tabela nova. A coluna `kilograms` é `numeric(5, 2)` com uma restrição dos mesmos limites do domínio.
- A troca do peso do dia é um único `insert ... on conflict do update`. Duas gravações ao mesmo tempo para a mesma data não criam duas linhas nem falham: a última vale. Por isso este módulo não precisa do lock advisory dos outros.

### Exportação

- A exportação da conta ganha a lista `weights`, com `date` e `kilograms`. É um campo novo no JSON, sem mudar os que existem, então a versão da exportação continua `1`.

## Consequências

- Bom: registrar e corrigir o peso é a mesma chamada, e o app não precisa saber se o dia já tinha peso.
- Bom: o peso segue as mesmas regras de dono, exportação e exclusão dos outros dados.
- Ruim: não dá para guardar duas pesagens no mesmo dia (de manhã e à noite).
- Ruim: a rota difere do rascunho do RFC (`PUT` por data em vez de `POST`).

## Fora do escopo

- A tela do peso, o gráfico e a média móvel.
- Outras unidades (libras), medidas do corpo e percentual de gordura.
- Metas de peso e ajuste automático de calorias.
