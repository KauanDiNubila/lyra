# ADR-010: Núcleo compartilhado para erros de domínio e Problem Details

- **Data**: 2026-11-23
- **Status**: Proposta, alterada pela [ADR-012](012-user-identity-and-data-ownership.md) (o `UserId` entra no núcleo compartilhado)
- **Decisores**: Kauan Di Nubila
- **Tags**: arquitetura, erros, api

## Contexto e problema

A [ADR-008](008-food-catalog-import-and-api.md) colocou o `InvalidInputException` no domínio de `food` e o mapeou para Problem Details no adaptador web de `food`. A [ADR-009](009-diary-entries-and-api.md) fez o diário reaproveitá-lo: o domínio do diário lança uma exceção que mora em `food`, e o adaptador web do diário repete o handler e ainda trata o `FoodNotFoundException`, outra classe de `food`. O limite escrito na ADR-009 para essa cópia já foi atingido pelo próprio diário.

Todo módulo novo (plan, weight, identity) repetiria o problema: um tipo de erro que não pertence a ninguém e mais uma cópia do mesmo handler.

## Fatores de decisão

1. Um módulo não deve depender de outro só para lançar ou mapear um erro genérico.
2. Um único formato de erro e um único lugar que o produz.
3. Nenhuma regra de arquitetura é enfraquecida para isso funcionar ([ADR-001](001-hexagonal-modular-monolith.md)).
4. A parte compartilhada precisa continuar muito pequena.

## Opções consideradas

1. **Manter a cópia**: cada módulo repete o handler e continua importando a exceção de `food`. Rejeitada: a dependência de `food` permanece e cresce a cada módulo.
2. **Uma exceção e um handler por módulo**: sem compartilhar nada. Rejeitada: as mesmas duas classes seriam reescritas em todo módulo, e o cliente passaria a ver o mesmo erro com formatos diferentes assim que uma cópia divergisse.
3. **Um pacote de núcleo compartilhado `app.lyra.shared`**, organizado como um módulo (`domain`, `adapter.in.web`).
4. **Um módulo Maven separado** para o núcleo. Rejeitada: a [ADR-001](001-hexagonal-modular-monolith.md) mantém as fronteiras nos testes e deixa a divisão em módulos Maven para depois, se os testes se mostrarem insuficientes; uma unidade de build para três classes custa mais do que economiza.

## Decisão

Opção escolhida: **um pacote de núcleo compartilhado, `app.lyra.shared`**, com exatamente três classes:

| Classe | Papel |
|---|---|
| `shared.domain.InvalidInputException` | Uma regra do domínio foi violada pela entrada. Movida de `food.domain`. |
| `shared.domain.NotFoundException` | Classe base dos erros de "este id não existe". `FoodNotFoundException` e `DiaryEntryNotFoundException` estendem essa classe. |
| `shared.adapter.in.web.ProblemDetailsHandler` | Um único `@RestControllerAdvice` para a API inteira. |

O handler produz os mesmos corpos de hoje:

| Exceção | Status | Título | Detalhe |
|---|---|---|---|
| `InvalidInputException` | `400` | `Invalid input` | a mensagem da exceção |
| `NotFoundException` | `404` | `Not found` | a mensagem da exceção |

Os erros que o Spring gera antes de chegar ao domínio mantêm o Problem Details padrão do Spring, como na ADR-008. Os handlers dos adaptadores web de `food` e de `diary` são removidos.

### O que pode entrar no núcleo

- Só o vocabulário de erros técnicos de que todo módulo precisa e o mapeamento HTTP desse vocabulário.
- Nada que carregue um conceito de negócio. `Food`, `Quantity` e `Nutrients` ficam em `food`, e o núcleo nunca depende de outro módulo.
- Uma classe só entra quando pelo menos dois módulos já a usam. O que não atender isso fica no seu módulo.

### Regras de arquitetura

O núcleo é um pacote sob `app.lyra`, organizado como um módulo, então as regras atuais do ArchUnit valem para ele sem mudança: o domínio de um módulo pode depender do `domain` de outro módulo, e os módulos só se alcançam por `application.port.in` ou `domain`. Nenhuma regra é enfraquecida. Se alguma precisar mudar, isso exige uma ADR própria.

Uma regra nova, `sharedKernelDependsOnNoModule`, garante o que as outras não garantem: as classes de `app.lyra.shared` só dependem de `java`, `lombok`, `org.springframework` e do próprio `shared`. Sem ela, o `shared.domain` poderia importar o `domain` de um módulo e o build passaria.

### O que continua copiado (emenda de 2026-11-24)

Esta seção diz que o `QuantityJson` continua copiado "até que `Quantity` mude de lugar ou um terceiro módulo precise dela". O terceiro módulo (`plan`) passou a usá-lo na slice 4, então o gatilho foi atingido. Ele foi reavaliado e a decisão é **manter as três cópias** (em `food`, `diary` e `plan`), pelos motivos abaixo.

- Mover só o `Quantity` para o núcleo não elimina a cópia. Um `QuantityJson` compartilhado teria de ficar em `shared.adapter.in.web`, e os adaptadores web de outros módulos o alcançariam. A regra `modulesTalkOnlyThroughInboundPortsAndDomain` só permite que módulos se alcancem por `application.port.in` e `domain`, então seria preciso abrir uma exceção na regra (que exige uma ADR própria).
- O `Quantity` carrega um conceito de negócio de `food` (as unidades `GRAM`, `MILLILITER` e `PORTION`), e a regra de entrada acima diz que nada assim entra no núcleo.
- O custo seria de cerca de 44 arquivos com imports trocados, mais uma exceção de regra, para apagar três cópias de 12 linhas.
- Nenhuma slice planejada cria uma quarta cópia: o registro rápido (slice 5) fica dentro de `diary` e o peso (slice 7) usa quilos, não `Quantity`.
- Serializar o `Quantity` direto, sem o DTO, foi descartado: o erro de uma quantidade inválida sairia no formato padrão do Spring em vez do Problem Details `Invalid input`, e o schema do `openapi.json` mudaria de nome.

A cópia é revisitada quando um quarto módulo precisar de `Quantity`, quando uma cópia divergir das outras (uma correção feita em uma e esquecida nas demais) ou quando uma ADR permitir um kit web compartilhado.

O `QuantityJson` é um DTO web de cada módulo e usa `food.domain.Quantity`. Um núcleo que conhecesse `Quantity` passaria a depender de `food`, então a cópia no adaptador web do diário continua até que `Quantity` mude de lugar ou um terceiro módulo precise dela.

### Consequências

- Bom: o `diary` deixa de importar tipos de erro de `food`, e um módulo novo ganha o formato de erro só por lançar as exceções compartilhadas.
- Bom: de dois handlers para um, com um único alvo de teste para o formato do erro.
- Bom: nenhuma regra da ADR-001 muda.
- Ruim: o handler é global, então vale para todos os controllers, inclusive os futuros. É a intenção, mas um módulo não consegue dar outro status à mesma exceção sem um handler próprio.
- Ruim: um pacote compartilhado tende a virar depósito. O ArchUnit impede que o núcleo dependa de um módulo, mas a regra de entrada acima (só o que dois módulos já usam) depende dos reviews.
- Ruim: o handler global pode fazer o documento OpenAPI listar `400` ou `404` em operações que não os declaravam; qualquer mudança assim aparece no diff de `docs/api/openapi.json`. Foi o que aconteceu: `GET /api/foods` e `POST /api/foods` passaram a documentar um `404` que nunca devolvem, a mesma imprecisão que o contrato do diário já tinha. Foi aceito para não voltar a registrar cada controller no handler.
