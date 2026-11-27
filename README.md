# Lyra

**O plano é o padrão. O diário registra o que mudou.**

O diário alimentar para quem segue o plano de um nutricionista.

Todo rastreador de dieta começa do mesmo lugar: um dia em branco e uma busca por "arroz". Mas quem segue um plano come quase a mesma coisa todo dia, e digitar tudo de novo é trabalho que não precisava existir. O Lyra parte do que você já combinou com o seu nutricionista e anota só a diferença.

|  | Um rastreador comum | Lyra |
|---|---|---|
| O dia começa | Em branco | Preenchido com o seu plano |
| Comer o que estava no plano | Buscar e digitar cada item | Já está lá |
| Comer 100 g a mais de arroz | Um registro novo | Um ajuste de quantidade |
| O que você vê no fim do dia | Um total | O total, o plano e as metas lado a lado |

## O dia já nasce pronto

Cadastre o plano uma vez: refeições, alimentos, quantidades e as metas diárias de calorias, proteína, carboidrato e gordura. Depois disso, o dia de hoje já abre com tudo no lugar, e qualquer outra data se preenche com um toque.

## Você registra só o que mudou

Cada entrada guarda o que estava planejado e o que foi comido de verdade.

| Entrada | Exemplo |
|---|---|
| Planejada | Arroz, 150 g, como no plano |
| Ajustada | Planejado 150 g, comido 250 g: +100 g |
| Trocada | O frango do almoço virou peixe |
| Extra | Uma maçã que não estava no plano |
| Pulada | O lanche da tarde que ficou para outro dia |

A cada mudança, os totais da refeição e do dia são recalculados, e você vê como o dia ficou em relação ao plano e às metas.

## Qualquer comida, do jeito que ela aparece

- **A comida de todo dia.** A TACO, a tabela brasileira de composição de alimentos, com o arroz, o feijão e o frango do jeito que a gente come.
- **O produto do mercado.** Busque pelo código de barras e o Lyra traz os nutrientes do Open Food Facts.
- **O milkshake da estrada.** Comida de restaurante não tem tabela. Digite o nome, as calorias e os macros uma vez, e ela fica guardada para a próxima.
- **Os seus alimentos.** Cadastre o que a sua receita tem de diferente e use como qualquer outro.

## Números em que dá para confiar

- **Contas exatas.** Os macros são calculados com precisão decimal e arredondados só na hora de mostrar na tela.
- **O passado não muda.** Cada entrada guarda uma cópia do nome e dos nutrientes do alimento no dia em que foi registrada. Trocar o plano ou corrigir um alimento depois não reescreve os dias anteriores.

## Seus dados são seus

Entre com a sua conta Google. Cada conta é isolada das outras: ninguém vê o seu plano, o seu diário nem os seus alimentos. Você exporta tudo em JSON quando quiser e apaga a conta com todos os dados.

O peso corporal também entra no diário, dia a dia, com a variação de uma pesagem para a outra.

## Por dentro

Backend em Java 25 e Spring Boot, com arquitetura hexagonal e PostgreSQL. Frontend em React, Tailwind CSS e shadcn/ui, e uma landing page em Astro. Cada decisão de arquitetura está registrada em [docs/adr](docs/adr/).

## Fontes dos dados

- **TACO**, Tabela Brasileira de Composição de Alimentos, 4. ed., NEPA/Unicamp, 2011.
- **Open Food Facts**, sob a licença ODbL.

Criado por Kauan Di Nubila.
