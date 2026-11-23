# ADR-011: Plano alimentar e diário preenchido a partir do plano

- **Data**: 2026-11-23
- **Status**: Proposta
- **Decisores**: Kauan Di Nubila
- **Tags**: plano, diário, domínio, dados, api

## Contexto e problema

A slice 4 do [RFC-001](../rfc/001-lyra-mvp.md) inverte o fluxo do diário: o plano alimentar é o padrão e o diário registra os desvios. O usuário cadastra o plano uma vez, o diário abre preenchido por ele, e cada ajuste, troca, item pulado ou extra recalcula os totais e a diferença em relação ao plano e às metas.

Isso exige decisões que o diário da [ADR-009](009-diary-entries-and-api.md) não cobria: como o plano é guardado, quando e como um dia é preenchido, como uma entrada lembra o que foi planejado, o que significa pular um item e o que se pode mudar numa entrada. Duas regras da ADR-009 deixam de valer como estão (veja "O que muda na ADR-009").

## Fatores de decisão

1. Abrir ou consultar um dia nunca pode inventar histórico: o que o usuário não registrou não vira registro sozinho.
2. Um dia já preenchido não muda quando o plano muda ([ADR-003](003-nutrient-snapshot-on-log.md)).
3. A diferença em relação ao plano precisa continuar calculável mesmo depois de ajustar, trocar ou pular.
4. Preencher duas vezes, por clique repetido ou duas abas, não pode duplicar entradas.
5. Pouco estado novo: o que puder ser calculado não é guardado.

## Decisão

### Plano

Um `DietPlan` tem nome, metas diárias (kcal, proteína, carboidrato e gordura), refeições em ordem e, em cada refeição, itens em ordem (alimento e quantidade). A refeição tem nome e um horário opcional, que é só um campo (sem lembretes).

- Um plano precisa de pelo menos uma refeição, e cada refeição de pelo menos um item. Nomes de refeição são únicos dentro do plano, com comparação exata, como no diário.
- A quantidade de um item é maior que zero e na unidade da quantidade de referência do alimento, a mesma regra da entrada do diário. As metas não podem ser negativas.
- O plano guarda só o `foodId` de cada item; nome e nutrientes são lidos do alimento na hora de mostrar o plano. O snapshot continua sendo da entrada do diário, não do plano.
- Existe **um único plano ativo**, global: ainda não há autenticação, então o plano não tem dono, a mesma regra dos alimentos personalizados e das entradas do diário. O banco garante a unicidade (índice único parcial sobre o plano ativo), e ativar um plano desativa o anterior na mesma transação.
- Editar um plano troca o plano inteiro (`PUT`), sem editar item por item. Não há exclusão de plano nesta slice.

### Preencher o dia

- O `GET /api/diary/{date}` continua sem efeito colateral.
- `POST /api/diary/{date}/plan` preenche o dia: para cada item do plano ativo cria uma entrada com o nome da refeição, o snapshot do alimento e a quantidade planejada. Só preenche um dia **sem nenhuma entrada**; se o dia já tem entradas, não faz nada e devolve o dia. Sem plano ativo, responde `404`.
- Cada entrada preenchida guarda a posição de origem (refeição e item) numa coluna `planned_slot`, com restrição única por data. Dois preenchimentos simultâneos não duplicam: o segundo bate na restrição e devolve o dia já preenchido.
- A tela chama esse `POST` sozinha ao abrir um dia vazio de **hoje ou do futuro**. Em um dia **passado** vazio mostra um botão "Preencher com o plano", porque um dia passado vazio quer dizer que nada foi registrado, e não que o plano foi seguido.

### Origem e quantidade planejada

Toda entrada preenchida pelo plano guarda a **quantidade planejada** e os **macros planejados** (kcal, proteína, carboidrato e gordura), calculados e copiados na hora do preenchimento. A origem **não é guardada**, é calculada:

| Situação | Origem |
|---|---|
| Sem quantidade planejada | `EXTRA` |
| Mesmo alimento, mesma quantidade do plano | `PLANNED` |
| Mesmo alimento, outra quantidade, maior que zero | `ADJUSTED` |
| Quantidade zero | `SKIPPED` |
| Outro alimento no lugar do planejado | `SWAPPED` |

O `QUICK_ADD` do RFC fica para a slice 5. Como os macros planejados são copiados, mudar o plano depois não altera dias já preenchidos, e dias ainda não abertos pegam o plano novo.

### Pular um item

Pular é **quantidade zero**, permitida só para entradas que vieram do plano (que têm quantidade planejada). Assim o diário continua mostrando o que foi planejado e perdido, como o RFC pede. Quantidade zero numa entrada sem plano continua sendo um erro.

### Trocar o alimento e mudar a refeição

O `PATCH /api/diary/entries/{id}` passa a aceitar `foodId` e `mealName` além de `quantity`, com pelo menos um deles.

- Trocar o alimento tira um novo snapshot (ADR-003) e exige a quantidade na mesma requisição, porque a unidade pode mudar. A quantidade e os macros planejados não mudam.
- Mudar a refeição só muda o nome. Isso também corrige o erro de digitação que a ADR-009 deixou como consequência ruim.

### Apagar uma entrada do plano

Uma entrada que veio do plano **não pode ser apagada**: a API responde `400` com a orientação de pular (quantidade zero). Apagá-la faria o planejado sumir do total planejado e esconderia o que foi perdido. Entradas sem plano (extras) continuam podendo ser apagadas.

### Diferença e metas

A resposta do dia passa a trazer, para cada entrada, a origem e o planejado, e para o dia, o total planejado (soma dos macros planejados das entradas que têm plano, inclusive as puladas), a diferença `total do dia − total planejado` e a diferença para as metas. O formato exato é definido no PR do backend do diário e fica no `docs/api/openapi.json`.

As metas vêm do **plano ativo no momento da leitura**. Se o usuário trocar de plano, dias antigos passam a ser comparados com as metas novas. É uma limitação consciente: guardar as metas em cada dia seria estado a mais sem valor agora.

### Contrato HTTP

| Método | Caminho | Sucesso | Erros |
|---|---|---|---|
| GET | `/api/plans` | `200` lista de planos (id, nome, se está ativo) | |
| POST | `/api/plans` | `201` plano | `404` alimento desconhecido; `400` regra violada |
| GET | `/api/plans/{id}` | `200` plano | `404` |
| PUT | `/api/plans/{id}` | `200` plano | `404` plano ou alimento; `400` regra violada |
| POST | `/api/plans/{id}/activation` | `200` plano | `404` |
| POST | `/api/diary/{date}/plan` | `200` dia | `404` sem plano ativo |
| PATCH | `/api/diary/entries/{id}` | `200` entrada | `404`; `400` regra violada |
| DELETE | `/api/diary/entries/{id}` | `204` | `404`; `400` entrada do plano |

Os erros seguem o Problem Details das ADRs 008 e 010: o módulo `plan` lança as exceções do núcleo compartilhado.

### O que muda na ADR-009

- "A quantidade precisa ser maior que zero" passa a ter uma exceção: zero é permitido para entradas que vieram do plano. A migração relaxa o check de quantidade da tabela `diary_entry` e o domínio aplica a regra nova.
- "Só a quantidade pode mudar" deixa de valer: o alimento e a refeição também podem mudar.

### Consequências

- Bom: o `GET` do dia continua puro, e o preenchimento é um comando explícito e idempotente.
- Bom: a diferença para o plano continua calculável depois de ajustar, trocar ou pular, porque o planejado fica na própria entrada.
- Bom: a origem é calculada, então não existe estado de origem para ficar inconsistente.
- Bom: mudar o plano não reescreve a história.
- Ruim: cada entrada ganha 5 colunas novas (quantidade planejada, 4 macros planejados) mais a posição de origem; as entradas antigas ficam sem planejado e passam a ser extras.
- Ruim: não se pode apagar uma entrada do plano; quem preencheu o dia por engano precisa pular item por item.
- Ruim: as metas dos dias antigos acompanham o plano ativo de hoje.
- Ruim: um plano só, global, vale até existir autenticação.
