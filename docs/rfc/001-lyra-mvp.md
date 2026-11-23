# RFC-001: MVP do Lyra

- **Data**: 2026-11-18
- **Status**: Rascunho
- **Autor**: Kauan Di Nubila
- **ADRs relacionadas**: [001](../adr/001-hexagonal-modular-monolith.md), [002](../adr/002-testing-strategy.md), [003](../adr/003-nutrient-snapshot-on-log.md), [004](../adr/004-pwa-with-astryx.md), [005](../adr/005-backend-stack.md), [006](../adr/006-food-data-sources.md)

## 1. Resumo

O Lyra é um rastreador de dieta construído em torno de um plano alimentar prescrito. O usuário cadastra o plano uma vez (refeições, alimentos e quantidades), e todo dia o Lyra abre um diário já preenchido a partir desse plano. O usuário então registra o que realmente aconteceu: mais arroz, um lanche pulado, um milkshake na estrada. Toda mudança recalcula os macros da refeição e do dia.

## 2. Motivação

Os rastreadores genéricos (MyFitnessPal, Cronometer, MacroFactor) começam de um dia vazio: o usuário registra tudo do zero. Quem segue o plano de um nutricionista come mais ou menos a mesma coisa todo dia, então registrar do zero é esforço perdido, e a ação mais comum, "comi o plano, mas 100 g a mais de arroz", não tem suporte de primeira classe.

O Lyra inverte o fluxo: **o plano é o padrão, o diário registra os desvios.**

## 3. Objetivos

1. Cadastrar um plano alimentar com refeições, alimentos, quantidades e metas diárias de macros.
2. Abrir o diário de qualquer data, já preenchido a partir do plano ativo.
3. Ajustar qualquer item planejado (aumentar ou diminuir a quantidade, trocar o alimento, removê-lo) e adicionar itens extras.
4. Recalcular os macros por item, por refeição e por dia depois de toda mudança, e mostrar a diferença em relação ao plano e às metas.
5. Encontrar alimentos por texto (alimentos genéricos brasileiros primeiro) e por código de barras.
6. Registrar comida de restaurante ou desconhecida com um quick add (nome mais calorias e macros) e reaproveitá-la depois.
7. Funcionar no celular em viagem: instalável, utilizável com uma conexão fraca.
8. Registrar o peso corporal.

## 4. Fora do escopo do MVP

- Recursos sociais (amigos, compartilhamento, sequências).
- Metas de calorias adaptativas a partir da tendência de peso.
- Registro por IA (texto livre, foto) e importação do PDF de um nutricionista.
- Micronutrientes além de fibra e sódio, controle de água, lembretes.
- Publicação na Play Store ou na App Store.

Cada um desses itens é candidato a um RFC futuro. A arquitetura não pode bloqueá-los (ver §7).

## 5. Modelo de domínio

### 5.1 Conceitos

| Conceito | Significado |
|---|---|
| **Food** | Algo que pode ser comido, com nutrientes por uma quantidade de referência (100 g, 100 ml ou 1 porção) e sua fonte (TACO, Open Food Facts, USDA, personalizado). |
| **Nutrients** | Value object: kcal, proteína, carboidrato, gordura, fibra e sódio, por quantidade de referência do alimento. O escalonamento pela quantidade fica na entrada do diário e a soma fica no `Macros` ([ADR-009](../adr/009-diary-entries-and-api.md)). |
| **Quantity** | Value object: quantidade mais unidade (`GRAM`, `MILLILITER`, `PORTION`). Nunca negativa. |
| **Diet plan** | Conjunto nomeado de refeições planejadas mais metas diárias. Um plano é o ativo por usuário por vez. |
| **Planned meal** | Um espaço do plano (café da manhã, almoço, ...) com uma ordem e um horário opcional. |
| **Planned item** | Um alimento e uma quantidade dentro de uma refeição planejada. |
| **Diary day** | O que o usuário comeu em uma data. É derivado das entradas daquela data e não tem tabela própria; uma data sem entradas é um dia vazio. A slice 4 vai criar as entradas do plano ativo quando a data for aberta. |
| **Diary entry** | Um item comido: a data, um nome de refeição em texto livre, um snapshot do alimento (nome e nutrientes) e a quantidade comida. A quantidade planejada e a origem chegam com a slice 4. |
| **Weight entry** | Peso corporal em uma data. |

### 5.2 Origem da entrada do diário

| Origem | Exemplo |
|---|---|
| `PLANNED` | Arroz 150 g, como planejado |
| `ADJUSTED` | Arroz planejado em 150 g, comido 250 g; a entrada guarda os dois valores e mostra +100 g |
| `SWAPPED` | Frango planejado trocado por peixe |
| `EXTRA` | Uma maçã que não estava no plano |
| `QUICK_ADD` | "Milkshake do McDonald's", 1 porção, com os nutrientes digitados pelo usuário |

Um item planejado que foi pulado é uma entrada com quantidade zero, para o diário continuar mostrando o que foi planejado e perdido.

### 5.3 Regras de cálculo

- Macros da entrada = `nutriente × quantidade ÷ snapshot.referenceAmount`, multiplicando primeiro e dividindo uma vez. As unidades precisam ser iguais; uma divergência é um erro de domínio, não uma conversão silenciosa. Os totais cobrem kcal, proteína, carboidrato e gordura ([ADR-009](../adr/009-diary-entries-and-api.md)).
- Total da refeição = soma das entradas dela. Total do dia = soma das refeições.
- Diferença em relação ao plano = total do dia menos o total planejado; em relação às metas = total do dia menos as metas.
- A aritmética usa `BigDecimal`. O arredondamento acontece só na borda da apresentação (uma casa decimal).

### 5.4 Por que um snapshot

Uma entrada do diário copia o nome e os nutrientes do alimento no momento em que é registrada. Se o plano mudar ou uma fonte externa corrigir um produto depois, os dias passados continuam como estavam. Ver [ADR-003](../adr/003-nutrient-snapshot-on-log.md).

## 6. Fontes de dados de alimentos

Todas as fontes ficam atrás de uma porta de saída (outbound port), `FoodCatalog`, com um adaptador por fonte. Detalhes e trade-offs na [ADR-006](../adr/006-food-data-sources.md).

| Ordem | Fonte | Usada para |
|---|---|---|
| 1 | Alimentos personalizados e quick adds do usuário | Qualquer coisa já registrada uma vez, inclusive itens de restaurante |
| 2 | TACO, importada para o banco do próprio Lyra | Alimentos genéricos brasileiros (arroz, feijão, frango) |
| 3 | Open Food Facts | Produtos embalados, principalmente por código de barras |
| 4 | USDA FoodData Central | Alternativa para alimentos genéricos que faltam na TACO |

Os resultados externos são guardados em cache no banco na primeira vez em que são escolhidos, então a busca seguinte é local e funciona offline.

Comida de restaurante (o caso do McDonald's) não tem fonte brasileira gratuita. O MVP a cobre com quick add mais reaproveitamento; um adaptador de estimativa por IA pode ser adicionado depois atrás da mesma porta.

## 7. Visão geral da arquitetura

- **Backend**: Java, arquitetura hexagonal (ports and adapters) como um monolito modular. Módulos: `food`, `plan`, `diary`, `weight`, `identity`, mais um pequeno núcleo compartilhado para tipos de erro ([ADR-010](../adr/010-shared-kernel-for-errors.md)). Ver [ADR-001](../adr/001-hexagonal-modular-monolith.md) e [ADR-005](../adr/005-backend-stack.md).
- **Frontend**: React 19 com o design system Astryx, entregue como um Progressive Web App. Ver [ADR-004](../adr/004-pwa-with-astryx.md).
- **Testes**: testes unitários para domain e application, testes de integração para os adaptadores, regras de arquitetura garantidas pelo ArchUnit. Ver [ADR-002](../adr/002-testing-strategy.md).

Os recursos futuros se encaixam como novos adaptadores ou novos módulos: o registro por IA é mais um adaptador do `FoodCatalog`, o social é um módulo novo que lê os dados do diário por uma porta de entrada (inbound port), um app nativo é mais um cliente da mesma API HTTP.

## 8. API HTTP (rascunho)

| Método | Caminho | Finalidade |
|---|---|---|
| GET | `/api/foods?query=` | Buscar alimentos entre as fontes |
| GET | `/api/foods/barcode/{code}` | Encontrar um produto embalado |
| POST | `/api/foods` | Criar um alimento personalizado |
| GET, POST | `/api/plans` | Listar e criar planos alimentares |
| GET, PUT | `/api/plans/{id}` | Ler e substituir um plano |
| POST | `/api/plans/{id}/activation` | Tornar um plano o ativo |
| GET | `/api/diary/{date}` | Ler o dia com suas refeições e totais (vazio quando não há entradas; a slice 4 o preenche a partir do plano) |
| POST | `/api/diary/{date}/entries` | Registrar um alimento em uma refeição |
| PATCH | `/api/diary/entries/{id}` | Mudar a quantidade (a troca do alimento chega com a slice 4) |
| DELETE | `/api/diary/entries/{id}` | Remover uma entrada |
| GET, POST | `/api/weights` | Listar e registrar o peso corporal |

O contrato das rotas já construídas é o `docs/api/openapi.json` que fica no repositório ([ADR-008](../adr/008-food-catalog-import-and-api.md), [ADR-009](../adr/009-diary-entries-and-api.md)); as outras rotas continuam sendo um rascunho.

## 9. Slices do MVP

Cada slice é construída primeiro o backend (domínio, adaptadores, testes, contrato da API) e depois a tela, cada uma em seu próprio pull request, antes de a slice seguinte começar.

1. Esqueleto do projeto: backend, frontend, CI, regras do ArchUnit.
2. Alimentos: importação da TACO, busca, alimento personalizado.
3. Diário: registrar alimentos por data e refeição, totais por refeição e por dia, editar e remover entradas. O backend está descrito na [ADR-009](../adr/009-diary-entries-and-api.md).
4. Plano alimentar: criar, editar, ativar; preencher o diário e mostrar a diferença em relação ao plano (ajustar, trocar).
5. Quick add e reaproveitamento.
6. Código de barras com o Open Food Facts.
7. Registro de peso.
8. PWA: instalar, leitura offline do diário de hoje.

O diário vem antes do plano porque é a menor peça que o usuário consegue usar todo dia e a que tem mais incerteza (editar quantidades e recalcular). O plano então o preenche. A ordem das slices é escolhida pelo que o usuário consegue usar mais cedo, pelo risco e pela dependência, nessa ordem.

## 10. Questões em aberto

1. **Autenticação.** Um usuário só no início, mas o social está no roadmap. Opções: Spring Security com cookies de sessão, ou um provedor externo. Até isso ser decidido, o MVP é de um usuário só e as entradas do diário não têm dono, a mesma regra dos alimentos personalizados ([ADR-008](../adr/008-food-catalog-import-and-api.md)); isso precisa de uma ADR própria antes de o app guardar dados de mais de uma pessoa.
2. **Escritas offline.** O MVP lê offline; enfileirar escritas offline e sincronizar depois é mais difícil (conflitos). Adiado para depois da slice 8.
3. ~~**Edição e formato da TACO.**~~ Resolvida na [ADR-008](../adr/008-food-catalog-import-and-api.md): planilha oficial, convertida para TSV e carregada por uma migração do Flyway.
4. **Hospedagem.** Não decidida; não bloqueia o desenvolvimento.
