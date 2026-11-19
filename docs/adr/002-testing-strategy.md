# ADR-002: Testing strategy

- **Date**: 2026-11-18
- **Status**: Proposed
- **Deciders**: Kauan Di Nubila
- **Tags**: testing, quality

## Context and Problem Statement

The goal is a project with very strong test coverage. "Unit tests for absolutely everything" is the starting request, but unit tests with mocks on controllers, mappers and repositories mostly test the mocks: they pass while the real HTTP mapping, SQL or external API contract is broken. The strategy must pick the right kind of test for each layer.

## Decision Drivers

1. Catch real defects, not only raise a coverage number.
2. Fast feedback for domain rules (macro calculation is the heart of the app).
3. Confidence that adapters work against the real database and the real shape of external APIs.

## Considered Options

1. **Unit tests for every class**, mocking all collaborators.
2. **Test per layer**: unit where logic lives, integration where the framework or the network lives.

## Decision Outcome

Chosen option: **test per layer**.

| Layer | Test type | Tools |
|---|---|---|
| `domain` | Unit, no mocks | JUnit 5, AssertJ |
| `application` | Unit; outbound ports replaced by in-memory fakes | JUnit 5, AssertJ |
| `adapter/in/web` | Slice test of the HTTP contract (status, JSON, validation) | `@WebMvcTest` |
| `adapter/out/persistence` | Integration against real PostgreSQL | `@DataJpaTest`, Testcontainers |
| `adapter/out/<external>` | Integration against recorded responses of the real API | WireMock |
| Architecture | Dependency rules from [ADR-001](001-hexagonal-modular-monolith.md) | ArchUnit |
| End to end | A few critical flows (open diary, adjust item, see totals) | Playwright |
| Frontend | Components and hooks | Vitest, Testing Library |

Coverage gate: JaCoCo fails the build below 90% line and branch coverage in `domain` and `application`. Adapters have no percentage gate; they are covered by the integration tests above.

In-memory fakes are preferred over Mockito in application tests because they test behaviour (what was saved) instead of interactions (which method was called), and survive refactoring.

### Consequences

- Good: every layer is tested by the kind of test that can actually break it.
- Good: domain tests stay fast; the slow tests are few and targeted.
- Bad: integration tests need Docker running locally and in CI.
- Bad: fakes must be kept in sync with the port contracts; a shared contract test per port mitigates this.
- Later: mutation testing (PIT) on `domain` to check that the tests really assert something.
