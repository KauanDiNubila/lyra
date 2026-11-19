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

Integration tests use Testcontainers and need Docker Desktop running. The application reads the database from `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` and fails to start without them; for local runs use `TestLyraApplication`, which starts PostgreSQL in a container.

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
2. Stop and remind the user to run `/thermo-nuclear-code-quality-review`, and send with the reminder the ready-to-paste command built from the template in "Code review context" below. Do not open the pull request before it runs.
3. When the findings arrive, give an opinion on each one (real problem or noise, fix now or not). Do not fix anything until the user chooses.
4. Fix the chosen findings, then open the pull request into `develop`.
5. Review the delivery item by item against the RFC slice it implements, not only for internal consistency.

## Code review context

When reminding the user to run the thermo-nuclear review, send one command in Brazilian Portuguese, so the review also answers in Portuguese. It has three parts: the fixed list below, the accepted items specific to that pull request, and what to push hard on.

Rules for the accepted list:

- Only decisions written in an ADR or explicitly agreed with the user go in; never "it looked fine".
- Every temporary item states its limit (until which slice or condition). Past the limit it is a finding again.
- The list never silences a contradiction of an ADR.

Fixed accepted decisions:

| Decision | Source | Limit |
|---|---|---|
| Lombok `@Getter`, `@RequiredArgsConstructor`, `@Slf4j` in domain and services | ADR-005 | |
| Records for value objects and DTOs instead of classes | ADR-005 | |
| In-memory fakes instead of Mockito in application tests | ADR-002 | |
| Snapshot of food name and nutrients duplicated in each diary entry | ADR-003 | |
| `allowEmptyShould(true)` on ArchUnit rules | Slice 1 | Until the first module exists (slice 2) |
| Spring starters in `pom.xml` without code using them yet | Slice 1 | Until slice 2 |
| Zero required approvals in the rulesets | ADR-007 | |

Fixed push-hard items: `domain` depending on anything outside the JDK; rounding outside the presentation edge; `@Data` or `@Setter` on domain entities; silent fallbacks in configuration or error handling; logic in the wrong module or layer; tests that only verify mocks.

Template:

```
/thermo-nuclear-code-quality-review Responda em português do Brasil. Contexto: Lyra, backend Java 25 com arquitetura hexagonal (ADR-001) e front React PWA com Astryx. Decisões aceitas, não apontar: <lista fixa e itens deste PR, cada um com o limite quando houver>. Apertar sem dó: <lista fixa e pontos de risco deste PR>.
```
