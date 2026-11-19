# ADR-001: Hexagonal architecture in a modular monolith

- **Date**: 2026-11-18
- **Status**: Proposed
- **Deciders**: Kauan Di Nubila
- **Tags**: architecture, backend

## Context and Problem Statement

Lyra depends on several external systems for the same need (food data from TACO, Open Food Facts, USDA and, later, AI estimation) and will be consumed by more than one client (PWA now, maybe a native app later). The core rules, macro calculation and the plan versus diary logic, must stay testable without any of them.

## Decision Drivers

1. Domain rules testable with plain unit tests, no framework or database.
2. Swapping or adding a food source without touching the domain.
3. Few moving parts for a one-person project.

## Considered Options

1. **Hexagonal architecture (ports and adapters)**, following *Get Your Hands Dirty on Clean Architecture* (Hombergs).
2. **Clean Architecture** with its full ring vocabulary (entities, use cases, presenters, request and response models).
3. **Layered architecture** (controller, service, repository).
4. **Microservices**.

## Decision Outcome

Chosen option: **hexagonal architecture inside a modular monolith**.

Each module (`food`, `plan`, `diary`, `weight`, `identity`) has the same layout:

```
<module>/
  domain/
  application/
    port/in/
    port/out/
    service/
  adapter/
    in/web/
    out/persistence/
    out/<external>/
```

Rules:

- `domain` depends on nothing outside the JDK at runtime. Compile-time Lombok annotations are allowed ([ADR-005](005-backend-stack.md)).
- `application` depends only on `domain`.
- Adapters depend on `application` and `domain`, never the other way.
- A module talks to another only through the other's inbound ports.

These rules are enforced by ArchUnit tests ([ADR-002](002-testing-strategy.md)).

Clean Architecture is rejected because its extra layers (presenters, separate request and response models per use case) add files without adding value at this size. Layered architecture is rejected because the domain ends up depending on JPA and on external clients. Microservices are rejected because one person would pay the operational cost of several deployables with no scaling need to justify it.

### Consequences

- Good: the food source problem becomes adding an adapter.
- Good: domain and application tests run in milliseconds.
- Bad: more packages and mapping between JPA entities and domain objects.
- Bad: module boundaries are enforced by tests, not by the build; a module split into Maven modules is possible later if the tests prove insufficient.
