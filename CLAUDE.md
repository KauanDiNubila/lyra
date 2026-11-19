# Lyra

Diet tracker built around a prescribed diet plan: the plan is the default, the diary records what changed. Backend in Java with hexagonal architecture, frontend as a React PWA with the Astryx design system.

Read before working on a slice:

- [RFC-001: Lyra MVP](docs/rfc/001-lyra-mvp.md): scope, domain model, slices, open questions
- [ADRs](docs/adr/): every structural decision; a change that contradicts one needs a new ADR first

## Repository layout

| Path | Content |
|---|---|
| `backend/` | Spring Boot 4.1, Java 25, Maven wrapper |
| `frontend/` | React 19, Vite, Astryx, vite-plugin-pwa |
| `docs/rfc/`, `docs/adr/` | RFCs and ADRs (MADR format, in English) |
| `.github/` | CI workflow and Dependabot |

## Commands

The machine's `JAVA_HOME` points to JDK 21; the backend needs 25.

```bash
cd backend && JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot" ./mvnw verify
cd frontend && npm run lint && npm test && npm run build
```

Integration tests use Testcontainers and need Docker Desktop running.

## Architecture rules (ADR-001)

Each backend module (`food`, `plan`, `diary`, `weight`, `identity`) under `app.lyra.<module>`:

```
domain/                 plain Java, no Spring, no JPA
application/port/in/    use case interfaces
application/port/out/   interfaces the application needs from outside
application/service/    use case implementations
adapter/in/web/         controllers
adapter/out/persistence/  JPA entities and repositories
adapter/out/<external>/   external API clients
```

- `domain` depends only on the JDK and other `domain` packages.
- `application` never depends on `adapter`.
- A module reaches another only through its `application.port.in` or `domain`.
- These rules are enforced by `HexagonalArchitectureTest`; never weaken a rule to make a build pass.

## Code conventions

- Code, identifiers, commits and docs in English. Talk to the user in Brazilian Portuguese.
- No code comments.
- Use the ponytail skill (lite) on code tasks; load it again after a context compaction, even if the summary says it is active.
- Value objects and DTOs are records. Lombok follows ADR-005: no `@Data`, no `@Setter` on domain entities, no Lombok `equals`/`hashCode` on JPA entities.
- Money-like and nutrient arithmetic uses `BigDecimal`; rounding only at the presentation edge.
- Do not change external contracts (routes, JSON, error format, database schema already migrated) without saying so explicitly.
- No new dependency without a reason written in an ADR or the pull request.
- Frontend: use Astryx components before writing custom UI; Astryx versions are pinned and upgraded deliberately. `frontend/CLAUDE.md` holds the Astryx component index and workflow, managed by `npx astryx init`; refresh it after an Astryx upgrade instead of editing it by hand.

## Tests (ADR-002)

- Every change ships with tests in the same pull request.
- `domain`: unit tests without mocks. `application`: unit tests with in-memory fakes for outbound ports.
- Web adapters: `@WebMvcTest`. Persistence: `@DataJpaTest` with Testcontainers. External APIs: WireMock.
- JaCoCo fails the build below 90% line and branch coverage in `domain` and `application`.
- Frontend: Vitest and Testing Library.

## Git workflow (ADR-007)

- Never commit or push to `main` or `develop` directly. Branch from `develop`: `feat/...`, `fix/...`, `docs/...`, `test/...`, `refactor/...`, `build/...`, `ci/...`.
- `develop` is the default branch. Pull requests into `develop` are rebase merged, so every commit lands as it is: keep commits small, each one building on its own, and every message in Conventional Commits (CI checks each one). Releases go from `develop` to `main` with a merge commit titled `release: ...`.
- No `Co-Authored-By` trailers and no tool attribution in commits or pull requests.
- Commit only when the user asks. "Por partes" means one small local commit per part. Push only on "commit e deploy".
- Say "merge", not "fundir".

## Before opening a pull request

1. Run backend and frontend checks locally and report what was and was not run.
2. Stop and remind the user to run `/thermo-nuclear-code-quality-review`. Do not open the pull request before it runs.
3. When the findings arrive, give an opinion on each one (real problem or noise, fix now or not). Do not fix anything until the user chooses.
4. Fix the chosen findings, then open the pull request into `develop`.
5. Review the delivery item by item against the RFC slice it implements, not only for internal consistency.
