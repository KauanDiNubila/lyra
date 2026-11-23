# ADR-006: Fontes de dados de alimentos

- **Data**: 2026-11-18
- **Status**: Proposta
- **Decisores**: Kauan Di Nubila
- **Tags**: domínio, integrações

## Contexto e problema

O Lyra precisa de dados nutricionais para alimentos genéricos brasileiros (arroz, feijão), produtos embalados (por código de barras) e itens de restaurante (um milkshake do McDonald's). Nenhuma API cobre os três casos para o Brasil sem custo.

Pesquisa feita em 2026-11-18 (preços e limites vindos de páginas de terceiros, a confirmar nos sites oficiais):

| Fonte | Cobertura | Custo | Limitação |
|---|---|---|---|
| TACO (NEPA/Unicamp), 4ª edição | Alimentos genéricos brasileiros (cerca de 600) | Gratuita, reprodução permitida com citação | Sem API oficial; as APIs de terceiros não são mantidas |
| TBCA (FoRC/USP) | Alimentos brasileiros (mais de 3.000) | Consulta gratuita | Sem API pública |
| Open Food Facts | Produtos embalados, código de barras | Gratuito, sem chave para leitura | Colaborativo, a qualidade varia |
| USDA FoodData Central | Alimentos genéricos, foco nos EUA | Gratuito, com chave de API | Em inglês, produtos dos EUA |
| FatSecret Platform | Global, tem um conjunto de dados do Brasil | Plano básico gratuito | Os dados do Brasil exigem uma licença paga por país |
| Nutritionix (Syndigo) | Restaurantes, linguagem natural | Gratuito só para testes | Foco nos EUA, planos pagos caros |

## Fatores de decisão

1. Alimentos brasileiros em primeiro lugar.
2. Nenhum custo recorrente no MVP.
3. As buscas precisam funcionar offline depois que um alimento foi usado.
4. Adicionar uma fonte depois não pode mexer no domínio.

## Decisão

Uma porta de saída (outbound port), `FoodCatalog`, com um adaptador por fonte, consultadas nesta ordem:

1. **Alimentos do usuário**: alimentos personalizados e quick adds já salvos.
2. **TACO**, importada uma vez para o banco do Lyra com uma migração do Flyway, citando a fonte.
3. **Open Food Facts** para códigos de barras e produtos embalados, com um `User-Agent` que identifica o app, como os termos deles pedem.
4. **USDA FoodData Central** como alternativa para alimentos genéricos.

Um alimento escolhido de uma fonte externa é guardado localmente no primeiro uso, então as buscas seguintes acertam o banco.

Os itens de restaurante são cobertos pelo **quick add** (nome, porção, calorias e macros digitados pelo usuário a partir da tabela publicada pelo restaurante) e são reaproveitados a partir do passo 1. Um adaptador de estimativa por IA fica para um RFC futuro.

FatSecret e Nutritionix são rejeitados no MVP porque os dados que importam (Brasil, restaurantes) estão atrás de licenças pagas.

### Consequências

- Bom: custo zero, dados brasileiros em primeiro lugar, offline depois do primeiro uso.
- Bom: cada fonte é um adaptador isolado, com seus próprios testes de WireMock.
- Ruim: os itens de restaurante exigem digitação manual na primeira vez.
- Ruim: a TACO tem cerca de 600 alimentos; a TBCA poderia ser importada depois se a cobertura não bastar.
