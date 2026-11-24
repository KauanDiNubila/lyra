# ADR-012: Identidade de usuário e dono dos dados

- **Data**: 2026-11-24
- **Status**: Proposta
- **Decisores**: Kauan Di Nubila
- **Tags**: identidade, segurança, dados, arquitetura, api

## Contexto e problema

Hoje o Lyra é de um usuário só. O [RFC-001](../rfc/001-lyra-mvp.md) deixou a autenticação como questão em aberto (seção 10, item 1) e disse que isso precisa de uma ADR própria antes de o app guardar dados de mais de uma pessoa. Essa hora chegou, porque a intenção é abrir o app a outras pessoas.

O estado atual, por decisão registrada em cada ADR:

- os alimentos personalizados são visíveis a todos ([ADR-008](008-food-catalog-import-and-api.md));
- as entradas do diário não têm dono ([ADR-009](009-diary-entries-and-api.md));
- existe um único plano ativo, global, e os locks do plano e do dia são globais ([ADR-011](011-diet-plan-and-diary-from-plan.md)).

O backend não tem nenhuma dependência de segurança. Dieta, peso e hábitos alimentares são dados de saúde, que a LGPD trata como dados pessoais sensíveis, então o erro de isolar mal os dados de duas pessoas é caro.

## Fatores de decisão

1. Menor superfície de segurança possível, porque o projeto é mantido por uma pessoa só.
2. Funcionar bem no PWA, que fala com o backend na mesma origem.
3. Os módulos de negócio não conhecem o Spring Security: a arquitetura da [ADR-001](001-hexagonal-modular-monolith.md) continua valendo.
4. O isolamento entre usuários precisa ser verificado por teste, não só combinado.
5. Guardar o mínimo de dados pessoais e permitir apagar e exportar a conta.
6. A porta fica aberta para outros métodos de login depois, sem refazer o modelo.

## Opções consideradas

1. **Login social OIDC (Google) com sessão em cookie.** Spring Security `oauth2Login`, sem guardar senha.
2. **E-mail e senha próprios.** Spring Security com tabela de usuários e hash de senha. Rejeitada por enquanto: obriga a cuidar de hash, recuperação de senha, verificação de e-mail, bloqueio de força bruta e envio de e-mail, uma superfície grande para quem desenvolve sozinho.
3. **Serviço gerenciado** (Auth0, Clerk, Keycloak e parecidos), com o backend validando tokens JWT. Rejeitada: custo recorrente que cresce com os usuários, mais uma peça de infraestrutura e dependência de um vendor, sem necessidade clara hoje.
4. **Link mágico por e-mail.** Rejeitada por enquanto: exige envio confiável de e-mail.

## Decisão

Opção escolhida: **login social com Google e sessão em cookie**, com **login obrigatório** e **dono em todos os dados de usuário**.

### Autenticação

- Login com Google pelo `oauth2Login` do Spring Security. O servidor guarda a sessão e entrega um cookie `HttpOnly`, `Secure` e `SameSite=Lax`.
- CSRF com o padrão para SPA do Spring: um cookie legível com o token e o cabeçalho `X-XSRF-TOKEN` em toda escrita. O frontend adiciona o cabeçalho na função `send`.
- Toda rota de `/api/**` exige sessão. Sem sessão a API responde `401` no formato Problem Details da [ADR-010](010-shared-kernel-for-errors.md). Ficam abertos só o fluxo de login (`/oauth2/**` e `/login/**`) e o endpoint de saúde.
- Não há uso anônimo: um modo de teste sem conta exigiria migrar dados ao criar a conta, e isso complica o modelo sem ganho claro agora.
- Outros provedores de login e e-mail com senha podem entrar depois, atrás do mesmo módulo `identity`, sem mudar o dono dos dados.

### Módulo `identity`

- Novo módulo `app.lyra.identity`, com a estrutura da [ADR-001](001-hexagonal-modular-monolith.md).
- `User` no domínio: id (UUID), provedor, identificador do usuário no provedor, e-mail, nome e data de criação. O par provedor e identificador é único.
- Guardamos só o que o provedor entrega (e-mail e nome). Não há senha.
- A porta de entrada `GetCurrentUserUseCase` devolve o usuário da sessão. O Spring Security fica só no adaptador web de `identity`.
- `GET /api/me` devolve o usuário atual, e `POST /api/logout` encerra a sessão.

### Como os outros módulos conhecem o usuário

- O núcleo compartilhado ganha `shared.domain.UserId`, um record sobre um UUID, e um resolvedor de argumento de controller em `shared.adapter.in.web` que lê o principal da sessão e entrega o `UserId` ao método do controller. Isso é uma emenda à [ADR-010](010-shared-kernel-for-errors.md): é um identificador técnico, sem regra de negócio, usado por vários módulos, o que cumpre a regra de entrada do núcleo. O núcleo pode depender de `org.springframework`, então o Spring Security não o quebra.
- O resolvedor entrega o `UserId` a **qualquer parâmetro de controller desse tipo**, sem anotação. Uma anotação `@CurrentUser` em `shared.adapter.in.web` foi tentada e recusada pelo ArchUnit: um controller de outro módulo a importaria, e a regra `modulesTalkOnlyThroughInboundPortsAndDomain` só deixa módulos se alcançarem por `application.port.in` e `domain`. O `UserId` mora em `shared.domain`, que a regra permite, e o resolvedor é registrado pelo Spring sem que nenhum módulo o importe. O documento OpenAPI ignora o tipo `UserId`, para o id da sessão não aparecer como parâmetro da API.
- Os adaptadores web passam o `UserId` nos comandos dos casos de uso. `application` e `domain` nunca importam o Spring Security.

### Dono dos dados

- `diet_plan`, `diary_entry` e `food` ganham `user_id uuid not null` com chave estrangeira para o usuário e `on delete cascade`, para que apagar a conta apague os dados. Os alimentos da TACO continuam globais: `user_id` fica nulo exatamente quando a origem é `TACO`, e uma restrição de banco garante isso.
- As restrições únicas passam a valer por usuário: um plano ativo **por usuário** e uma posição de preenchimento por data **por usuário**.
- Os dois locks advisory (da troca de plano e do preenchimento do dia) passam a incluir o usuário na chave, para que o preenchimento de uma pessoa não espere o de outra.
- Toda consulta dos repositórios recebe o dono. Um recurso de outro usuário responde `404`, não `403`, para não revelar que ele existe.
- Esta decisão **altera** a regra do plano ativo global da ADR-011, a visibilidade dos alimentos personalizados da ADR-008 e a ausência de dono do diário da ADR-009.

### Verificação do isolamento

Um conjunto de testes de ponta a ponta, com dois usuários, confirma que um não lê, não muda, não apaga nem ativa nada do outro em `food`, `plan` e `diary`. Esses testes são condição para o merge da etapa de dono dos dados.

### Dados pessoais

- Antes de abrir o app ao público: endpoints para exportar os dados do usuário (JSON) e para apagar a conta, política de privacidade e consentimento. A parte técnica é planejada aqui; o texto da política e a revisão da conformidade com a LGPD ficam fora desta ADR e merecem revisão jurídica.
- O banco de desenvolvimento não tem dados reais, então a migração que cria o dono apaga as linhas de teste sem dono em vez de inventar um dono para elas.

## Consequências

- Bom: sem senha, a superfície de segurança é pequena, e a recuperação da conta fica com o provedor.
- Bom: o cookie na mesma origem se encaixa no PWA, e os módulos de negócio continuam sem conhecer o Spring Security.
- Bom: o isolamento é verificado por teste, e o módulo `identity` já guarda um id estável e o e-mail para o que vier depois (inclusive cobrança).
- Ruim: depende do Google. Quem não tem ou não quer uma conta Google fica de fora até existir outro método.
- Ruim: o fluxo de cookie com CSRF acrescenta cuidado no frontend, e todo teste de API passa a precisar de um usuário autenticado.
- Ruim: os locks por usuário e os índices por usuário deixam as migrações e os repositórios mais trabalhosos.
- Ruim: o `UserId` entra no núcleo compartilhado, que tende a virar depósito; a regra de entrada continua dependendo dos reviews.

## Fora do escopo

- Hospedagem (questão 4 do RFC): ela define o endereço de retorno do login e o cookie `Secure`, e precisa estar decidida antes de configurar o provedor.
- Política de privacidade, termos de uso e a análise de conformidade com a LGPD.
- Cobrança e planos pagos.
