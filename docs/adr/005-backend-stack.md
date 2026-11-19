# ADR-005: Backend stack

- **Date**: 2026-11-18
- **Status**: Proposed
- **Deciders**: Kauan Di Nubila
- **Tags**: backend, stack

## Context and Problem Statement

The backend is in Java (a project requirement). This ADR fixes the versions and the main libraries so the skeleton can be created.

## Decision Drivers

1. Long-term support on the Java version.
2. Mature ecosystem for the testing strategy in [ADR-002](002-testing-strategy.md).
3. Few dependencies.

## Decision Outcome

| Concern | Choice | Reason |
|---|---|---|
| Language | Java 25 (LTS) | Latest LTS when the project starts; records and sealed types for value objects and entry origins |
| Framework | Spring Boot 4.1.x | Web, validation, data, security in one consistent line |
| Build | Maven | Familiar, plain dependency management |
| Database | PostgreSQL | Relational data with real decimal types; same engine in tests via Testcontainers |
| Migrations | Flyway | Versioned SQL, reviewed like code |
| Persistence | Spring Data JPA, only inside `adapter/out/persistence` | The domain never sees JPA ([ADR-001](001-hexagonal-modular-monolith.md)) |
| HTTP clients | Spring `RestClient` | No extra dependency for Open Food Facts and USDA |
| API contract | springdoc-openapi | OpenAPI document generated and checked in CI |
| Tests | JUnit 5, AssertJ, Testcontainers, WireMock, ArchUnit, JaCoCo | See [ADR-002](002-testing-strategy.md) |

### Lombok

Lombok is used to remove boilerplate from classes that cannot be records: domain entities with identity and mutable state (`DietPlan`, `DiaryDay`, `DiaryEntry`) and JPA entities in the persistence adapters.

| Where | Use |
|---|---|
| Value objects and DTOs (`Nutrients`, `Quantity`, web requests and responses) | Java records, no Lombok |
| Domain entities | `@Getter`, `@RequiredArgsConstructor`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` on the id; no `@Setter`, no `@Data`: state changes only through domain methods that enforce the rules |
| JPA entities | `@Getter`, `@Setter`, `@NoArgsConstructor(access = PROTECTED)`; no `@Data` and no Lombok `equals`/`hashCode` (lazy loading and identity issues) |
| Services and adapters | `@RequiredArgsConstructor` for constructor injection |
| Logging | `@Slf4j` |

Lombok annotations used here have source retention and do not exist in the compiled classes, so the domain still has no runtime dependency outside the JDK ([ADR-001](001-hexagonal-modular-monolith.md)). `lombok.config` at the project root sets `lombok.addLombokGeneratedAnnotation = true` so JaCoCo ignores generated code.

### Consequences

- Good: a stack with long support and well-known tooling.
- Bad: Spring Boot 4 is recent; some third-party starters may lag behind.
- Bad: Lombok depends on compiler internals and has historically lagged behind new JDK releases; the Lombok version must support Java 25 before the skeleton is created.
