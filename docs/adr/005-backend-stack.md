# ADR-005: Stack do backend

- **Data**: 2026-11-18
- **Status**: Proposta
- **Decisores**: Kauan Di Nubila
- **Tags**: backend, stack

## Contexto e problema

O backend é em Java (um requisito do projeto). Esta ADR fixa as versões e as principais bibliotecas para que o esqueleto possa ser criado.

## Fatores de decisão

1. Suporte de longo prazo na versão do Java.
2. Ecossistema maduro para a estratégia de testes da [ADR-002](002-testing-strategy.md).
3. Poucas dependências.

## Decisão

| Assunto | Escolha | Motivo |
|---|---|---|
| Linguagem | Java 25 (LTS) | Última LTS quando o projeto começa; records e sealed types para value objects e para as origens das entradas |
| Framework | Spring Boot 4.1.x | Web, validação, dados e segurança em uma linha consistente |
| Build | Maven | Familiar, gestão de dependências simples |
| Banco de dados | PostgreSQL | Dados relacionais com tipos decimais de verdade; o mesmo motor nos testes via Testcontainers |
| Migrações | Flyway | SQL versionado, revisado como código |
| Persistência | Spring Data JPA, só dentro de `adapter/out/persistence` | O domínio nunca enxerga o JPA ([ADR-001](001-hexagonal-modular-monolith.md)) |
| Clientes HTTP | Spring `RestClient` | Sem dependência extra para o Open Food Facts e a USDA |
| Contrato da API | springdoc-openapi | Documento OpenAPI gerado e conferido no CI |
| Testes | JUnit 5, AssertJ, Testcontainers, WireMock, ArchUnit, JaCoCo | Ver [ADR-002](002-testing-strategy.md) |

### Lombok

O Lombok é usado para tirar boilerplate de classes que não podem ser records: entidades de domínio com identidade e estado mutável (`DietPlan`, `DiaryDay`, `DiaryEntry`) e entidades JPA nos adaptadores de persistência.

| Onde | Uso |
|---|---|
| Value objects e DTOs (`Nutrients`, `Quantity`, requisições e respostas web) | Records do Java, sem Lombok |
| Entidades de domínio | `@Getter`, `@RequiredArgsConstructor`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` no id; sem `@Setter`, sem `@Data`: o estado só muda por métodos de domínio que aplicam as regras |
| Entidades JPA | `@Getter`, `@Setter`, `@NoArgsConstructor(access = PROTECTED)`; sem `@Data` e sem `equals`/`hashCode` do Lombok (problemas de lazy loading e de identidade) |
| Serviços e adaptadores | `@RequiredArgsConstructor` para injeção por construtor |
| Logging | `@Slf4j` |

As anotações do Lombok usadas aqui têm retenção de fonte e não existem nas classes compiladas, então o domínio continua sem dependência de execução fora do JDK ([ADR-001](001-hexagonal-modular-monolith.md)). O `lombok.config` na raiz do projeto define `lombok.addLombokGeneratedAnnotation = true` para o JaCoCo ignorar o código gerado.

### Consequências

- Bom: uma stack com suporte longo e ferramentas bem conhecidas.
- Ruim: o Spring Boot 4 é recente; alguns starters de terceiros podem ficar para trás.
- Ruim: o Lombok depende de partes internas do compilador e historicamente demorou a acompanhar as novas versões do JDK; a versão do Lombok precisa suportar o Java 25 antes de o esqueleto ser criado.
