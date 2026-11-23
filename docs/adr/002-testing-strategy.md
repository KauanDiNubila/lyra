# ADR-002: Estratégia de testes

- **Data**: 2026-11-18
- **Status**: Proposta
- **Decisores**: Kauan Di Nubila
- **Tags**: testes, qualidade

## Contexto e problema

O objetivo é um projeto com cobertura de testes muito forte. "Testes unitários para absolutamente tudo" é o pedido inicial, mas testes unitários com mocks em controllers, mappers e repositories testam, na maior parte, os próprios mocks: passam enquanto o mapeamento HTTP real, o SQL ou o contrato de uma API externa estão quebrados. A estratégia precisa escolher o tipo certo de teste para cada camada.

## Fatores de decisão

1. Pegar defeitos reais, e não só aumentar um número de cobertura.
2. Feedback rápido para as regras de domínio (o cálculo de macros é o coração do app).
3. Confiança de que os adaptadores funcionam contra o banco de dados real e o formato real das APIs externas.

## Opções consideradas

1. **Testes unitários para toda classe**, com mocks em todos os colaboradores.
2. **Um teste por camada**: unitário onde mora a lógica, de integração onde mora o framework ou a rede.

## Decisão

Opção escolhida: **um teste por camada**.

| Camada | Tipo de teste | Ferramentas |
|---|---|---|
| `domain` | Unitário, sem mocks | JUnit 5, AssertJ |
| `application` | Unitário; as portas de saída (outbound ports) são trocadas por fakes em memória | JUnit 5, AssertJ |
| `adapter/in/web` | Teste de fatia (slice) do contrato HTTP (status, JSON, validação) | `@WebMvcTest` |
| `adapter/out/persistence` | Integração contra um PostgreSQL real | `@DataJpaTest`, Testcontainers |
| `adapter/out/<external>` | Integração contra respostas gravadas da API real | WireMock |
| Arquitetura | Regras de dependência da [ADR-001](001-hexagonal-modular-monolith.md) | ArchUnit |
| Ponta a ponta | Alguns fluxos críticos (abrir o diário, ajustar um item, ver os totais) | Playwright |
| Frontend | Componentes e hooks | Vitest, Testing Library |

Portão de cobertura: o JaCoCo falha o build abaixo de 90% de cobertura de linhas e de branches em `domain` e `application`. Os adaptadores não têm portão de porcentagem; são cobertos pelos testes de integração acima.

Fakes em memória são preferidos ao Mockito nos testes de application porque testam comportamento (o que foi salvo) em vez de interações (qual método foi chamado), e sobrevivem a refatorações.

### Consequências

- Bom: cada camada é testada pelo tipo de teste que realmente consegue quebrá-la.
- Bom: os testes de domínio continuam rápidos; os lentos são poucos e direcionados.
- Ruim: os testes de integração precisam do Docker rodando, localmente e no CI.
- Ruim: os fakes precisam ficar em sincronia com os contratos das portas; um teste de contrato compartilhado por porta reduz esse risco.
- Depois: testes de mutação (PIT) em `domain`, para checar que os testes realmente afirmam alguma coisa.
