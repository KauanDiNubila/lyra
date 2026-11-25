# ADR-013: Busca por código de barras com o Open Food Facts

- **Data**: 2026-11-26
- **Status**: Proposta
- **Decisores**: Kauan Di Nubila
- **Tags**: alimentos, fontes externas, api, dados

## Contexto e problema

A slice 6 do [RFC-001](../rfc/001-lyra-mvp.md) é o código de barras: a pessoa escaneia ou digita o código de um produto embalado e o Lyra devolve o alimento com seus nutrientes. A [ADR-006](006-food-data-sources.md) já escolheu o Open Food Facts como a fonte desses produtos, atrás de uma porta de saída, e disse que um alimento de fonte externa é guardado localmente no primeiro uso. Faltam as decisões de como isso entra no código e no banco:

- onde o produto fica guardado e quem o enxerga;
- o que fazer quando o produto não existe, não tem dados suficientes ou a fonte está fora do ar;
- como o Lyra se identifica, que limites de tempo usa e como cumpre a licença.

Hoje o banco só conhece duas fontes. A restrição `food_owner_check` (migração V7) diz que `user_id` é nulo exatamente quando a fonte é `TACO`, e o `check` da fonte aceita só `TACO` e `CUSTOM`. A [ADR-012](012-user-identity-and-data-ownership.md) fez do alimento personalizado um dado do usuário, visível só a ele.

Como o Open Food Facts tem uma API pública, sem chave, com um limite de uso por IP e com a exigência de o cliente se identificar pelo `User-Agent`, a integração precisa ser leve e tolerante a falhas.

## Fatores de decisão

1. Um produto escaneado por duas pessoas deve ser o mesmo alimento, e uma correção do Open Food Facts não pode ficar presa em cópias antigas.
2. A busca por texto deve achar um produto já escaneado, sem nova chamada externa ([ADR-006](006-food-data-sources.md)).
3. A fonte externa pode falhar ou devolver dados ruins sem derrubar o app nem gravar lixo.
4. Nenhum dado pessoal vai para o Open Food Facts: só o código de barras.
5. Nenhuma dependência de produção nova.
6. A licença dos dados (ODbL) precisa ser cumprida.

## Opções consideradas

1. **Cache compartilhado**: o produto é guardado uma vez, sem dono, com a fonte `OPEN_FOOD_FACTS`, e todos o enxergam.
2. **Uma cópia por usuário**, como alimento personalizado.
3. **Sem cache**: consultar o Open Food Facts a cada escaneamento.

## Decisão

Opção 1, o cache compartilhado.

### Porta e adaptador

- O módulo `food` ganha a porta de saída `ProductCatalog`, com `findByBarcode(Barcode)`, e o adaptador `adapter/out/openfoodfacts`, que fala HTTP com o `RestClient` do Spring (já presente no `spring-boot-starter-webmvc`, sem dependência de produção nova).
- `Barcode` é um objeto de valor do domínio: de 8 a 14 dígitos (EAN-8, UPC-A, EAN-13 e ITF-14). Qualquer outra coisa é um erro de entrada (`400`), sem chamar a fonte.
- O caso de uso `LookupFoodByBarcodeUseCase` procura primeiro no banco; só se não achar chama a porta, e guarda o resultado. A rota é `GET /api/foods/barcode/{code}` e exige sessão como as outras.

### Cache compartilhado e visibilidade

- Uma migração (V8) aceita a fonte `OPEN_FOOD_FACTS` e troca a restrição do dono: `user_id` é obrigatório exatamente quando a fonte é `CUSTOM`. A TACO e o Open Food Facts continuam sem dono e visíveis a todos. Isso **altera** a regra da ADR-008 e da V7, que ligava a ausência de dono só à TACO.
- O código de barras fica em `source_reference` com o prefixo `off:`, que já é único, o que impede dois produtos iguais e uma colisão com as referências da TACO.
- Duas pessoas que escaneiam o mesmo produto novo ao mesmo tempo usam o mesmo lock advisory, calculado pelo código, para que só uma grave e as duas recebam a mesma linha.
- O produto guardado entra na busca por texto como qualquer alimento, e o `FoodResponse.source` ganha o valor `OPEN_FOOD_FACTS`.

### Os dados

- Os nutrientes vêm por 100 g (ou 100 ml, quando o produto traz `product_quantity_unit` em `ml`): calorias (`energy-kcal_100g`), proteína, carboidrato, gordura, fibra e sódio. O Open Food Facts dá o sódio em gramas, e o Lyra o guarda em miligramas, então o adaptador multiplica por 1000.
- O produto só é aceito com nome e com calorias, proteína, carboidrato e gordura presentes e válidos. Fibra e sódio podem faltar. Sem isso, a resposta é "produto não encontrado" (`404`, com a mensagem dizendo se o produto não existe ou não tem os dados), e a tela leva ao quick add com o nome já preenchido. O Lyra não completa nutriente que falta com zero, pelo mesmo motivo do quick add.
- Os valores também precisam ser possíveis para 100 g: calorias até 900 (o valor da gordura pura), proteína, carboidrato, gordura, fibra e sódio até 100 g cada, e proteína, carboidrato e gordura somando até 105 g (a folga cobre arredondamento). Fora disso o produto é tratado como dados insuficientes. O Open Food Facts é editado por voluntários, e o produto guardado é compartilhado e não expira, então um erro de digitação não pode entrar.
- O nome é o `product_name` com a primeira marca entre parênteses, quando a marca ajuda a distinguir produtos de nome genérico.
- O Open Food Facts responde `200` com `status: 0` para um código inexistente. O adaptador trata isso como produto não encontrado, e não como erro.

### Falhas e limites

- Tempo de conexão de 2 s e de leitura de 4 s. Erro de rede, tempo esgotado, `429` ou `5xx` viram `503` com Problem Details ("a fonte de produtos está indisponível"), sem nova tentativa automática: o app não deve segurar a tela, e a pessoa pode tentar de novo ou usar o quick add.
- O adaptador pede só os campos de que precisa (`fields=`), para a resposta ser pequena. Toda falha da fonte é registrada em log, em nível de aviso, com o código de barras e a causa, para que uma queda ou um bloqueio do `User-Agent` não passe despercebido.
- Um código que não existe não é guardado, então a mesma consulta repetida volta a ir ao Open Food Facts. Um usuário logado pode, assim, gastar a cota compartilhada de consultas, e o servidor pode ser bloqueado para todos. Na fase atual, só o autor e conhecidos usam o app, e isso é aceito. Antes de abrir o app ao público, o "não achei" deve ser guardado por algumas horas, ou as consultas limitadas por usuário.
- Como o produto é guardado na primeira consulta, o uso da API externa cresce com o número de produtos diferentes escaneados, não com o de escaneamentos.

### Identificação e privacidade

- O `User-Agent` é `Lyra/0.1 (https://github.com/KauanDiNubila/lyra)`, configurável por propriedade. Não leva e-mail nem nome de pessoa, e é a única informação sobre o Lyra que sai. A requisição leva só o código de barras. Quem escaneia não sai do Lyra: o Open Food Facts vê o endereço do servidor e o momento da primeira consulta de cada produto, sem saber de quem foi. Enquanto o repositório for privado, o endereço do `User-Agent` não leva a um contato; antes de abrir o app ao público é preciso um contato que funcione, como um site do projeto ou um e-mail dedicado.

### Licença

- Os dados do Open Food Facts são da licença ODbL: exigem citar a fonte e compartilhar sob a mesma licença uma base derivada que seja **publicada**. O banco do Lyra não é público, então a obrigação de compartilhar não se aplica hoje, mas a de citar vale já: a tela que mostra um produto desta fonte exibe o crédito, como a TACO. Se um dia o Lyra publicar a base de produtos, esta ADR precisa ser revista. A licença das imagens não entra, porque o Lyra não guarda nem exibe imagens.

### Testes

- O adaptador tem testes com o WireMock, como já prevêem a [ADR-002](002-testing-strategy.md) e a [ADR-006](006-food-data-sources.md): produto completo, produto sem dados, código inexistente (`status: 0`), litros em vez de gramas, `429`, `5xx` e tempo esgotado. O WireMock é a única dependência nova, e só de teste.
- O caso de uso é testado com um catálogo falso em memória, e há um teste de ponta a ponta com dois usuários que confirma que o produto guardado é o mesmo para os dois.

## Consequências

- Bom: um produto escaneado serve a todos, e a busca por texto o encontra sem chamada externa.
- Bom: a fonte externa pode falhar sem derrubar o app, e dado ruim nunca é gravado.
- Bom: nenhum dado pessoal sai do Lyra.
- Ruim: um erro do Open Food Facts em um produto já guardado só se corrige quando alguém atualizar o registro no Lyra, porque o cache não expira. A primeira versão aceita isso.
- Ruim: a restrição de dono do banco fica menos simples: a regra passa de "TACO não tem dono" para "só o alimento personalizado tem dono".
- Ruim: o Lyra passa a depender de uma API pública cujo limite de uso e disponibilidade ele não controla.

## Fora do escopo

- O escaneamento pela câmera e a tela do código de barras (slice 6, parte da tela), e a decisão do app do iPhone, que terá uma ADR própria.
- Renovar o cache quando o Open Food Facts atualiza um produto.
- Busca por nome no Open Food Facts, USDA e a TBCA.
- Imagens dos produtos.
