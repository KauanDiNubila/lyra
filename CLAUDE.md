# Lyra

Diet tracker built around a prescribed diet plan: the plan is the default, the diary records what changed. Backend in Java with hexagonal architecture, frontend as a React PWA with Tailwind CSS and shadcn/ui.

Read before working on a slice:

- [RFC-001: Lyra MVP](docs/rfc/001-lyra-mvp.md): scope, domain model, slices, open questions
- [ADRs](docs/adr/): every structural decision; a change that contradicts one needs a new ADR first

## Repository layout

| Path | Content |
|---|---|
| `backend/` | Spring Boot 4.1, Java 25, Maven wrapper |
| `frontend/` | React 19, Vite, Tailwind CSS v4, shadcn/ui, vite-plugin-pwa |
| `design/` | Design tokens shared by the app and the landing page |
| `docs/rfc/`, `docs/adr/` | RFCs and ADRs (MADR format, in Brazilian Portuguese) |
| `docs/api/openapi.json` | HTTP contract; `OpenApiContractTest` fails when the API drifts from it |
| `tools/taco/` | One-off converter from the TACO spreadsheet to the seed TSV |
| `.github/` | CI workflow and Dependabot |

## Commands

The machine's `JAVA_HOME` points to JDK 21; the backend needs 25.

```bash
cd backend && JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot" ./mvnw verify
cd frontend && npm run lint && npm test && npm run build
cd tools/taco && python -X utf8 -m unittest
```

Integration tests use Testcontainers and need Docker Desktop running. The application reads the database from `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` and fails to start without them. It also needs `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` (the Google sign-in client) and fails to start without them. For local runs use `TestLyraApplication`, which starts PostgreSQL in a container and fills the Google values with fake ones, so the sign-in only works when the real ones are exported. The session cookie is `Secure`, so a deployment needs HTTPS.

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

- After a context compaction, read this file again before the next step and follow it, including the pull request steps below.
- Code, identifiers, commits and pull request text in English. RFCs and ADRs in Brazilian Portuguese, keeping technical terms and identifiers in English. Talk to the user in Brazilian Portuguese.
- No code comments.
- Write the simplest code that works, readable by a junior developer: well-known names, short methods, plain Java and Spring features, no clever tricks. Add an abstraction only when two real cases need it, and when a pattern appears for the first time, explain it in the pull request.
- Use the ponytail skill (lite) on code tasks; load it again after a context compaction, even if the summary says it is active.
- Value objects and DTOs are records. Lombok follows ADR-005: no `@Data`, no `@Setter` on domain entities, no Lombok `equals`/`hashCode` on JPA entities.
- Money-like and nutrient arithmetic uses `BigDecimal`; rounding only at the presentation edge.
- Do not change external contracts (routes, JSON, error format, database schema already migrated) without saying so explicitly.
- No new dependency without a reason written in an ADR or the pull request.
- Frontend (ADR-017): use the shadcn/ui components in `frontend/src/components/ui` before writing custom UI, and add new ones with `npx shadcn@latest add <name>` instead of writing them by hand. Colours and radii come from `design/tokens.css`, never from loose values in classes. Every animation respects `prefers-reduced-motion`.

## Tests (ADR-002)

- Every change ships with tests in the same pull request.
- `domain`: unit tests without mocks. `application`: unit tests with in-memory fakes for outbound ports.
- Web adapters: `@WebMvcTest`. Persistence: `@DataJpaTest` with Testcontainers. External APIs: WireMock.
- JaCoCo fails the build below 90% line and branch coverage in `domain` and `application`.
- Every outbound port that has an in-memory fake has a `<Port>Contract` interface with the behaviour tests, implemented by the fake's test and by the real adapter's test.
- Test methods inherited from an interface do not run inside the `@DataJpaTest` transaction, so nothing is rolled back: an adapter test that writes cleans up with `@AfterEach`.
- Frontend: Vitest and Testing Library.

## Git workflow (ADR-007)

- Never commit or push to `main` or `develop` directly. Branch from `develop`: `feat/...`, `fix/...`, `docs/...`, `test/...`, `refactor/...`, `build/...`, `ci/...`.
- `develop` is the default branch. Pull requests into `develop` use Merge pull request (a merge commit), so every commit lands with the hash CI tested: keep commits small, each one building on its own, and every message in Conventional Commits (CI checks each one). Releases go from `develop` to `main` with a merge commit titled `release: ...`.
- No `Co-Authored-By` trailers and no tool attribution in commits or pull requests.
- Commit only when the user asks. "Por partes" means one small local commit per part. Push only on "commit e deploy".
- Say "merge", not "fundir".

## Before opening a pull request

1. Run backend and frontend checks locally and report what was and was not run.
2. Stop and remind the user to run `/thermo-nuclear-code-quality-review`, and send with the reminder the ready-to-paste command built from the template in "Code review context" below. Do not open the pull request before it runs. Say it also when asking to commit the last part of a pull request.
3. When the findings arrive, give an opinion on each one (real problem or noise, fix now or not). Do not fix anything until the user chooses.
4. Fix the chosen findings, then open the pull request into `develop`.
5. Review the delivery item by item against the RFC slice it implements, not only for internal consistency.

## Code review context

When reminding the user to run the thermo-nuclear review, send one short command in Brazilian Portuguese, so the review also answers in Portuguese. Keep it to two lines at most: the fixed lists below already live in this file, so the command only points to them and adds what is specific to the pull request. Never paste the fixed lists into it.

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
| Zero required approvals in the rulesets | ADR-007 | |
| TACO rules: `Tr` and `NA` stored as 0, foods with unknown macros skipped, negative carbohydrate stored as 0 | ADR-008 | |
| Generated `taco-foods.tsv` and `docs/api/openapi.json` committed | ADR-008 | |
| Contract without `required` fields; errors raised by Spring keep the default `Bad Request` title | ADR-008 | Until a second API client exists |
| TACO converter tests not wired to the CI | ADR-008 | Until the converter is run more than once |
| Three copies of `QuantityJson` (food, diary and plan web adapters) | ADR-010 | Until a fourth module needs `Quantity`, a copy diverges, or an ADR allows a shared web kit |

Fixed push-hard items: code a junior developer could not read at a glance; `domain` depending on anything outside the JDK; rounding outside the presentation edge; `@Data` or `@Setter` on domain entities; silent fallbacks in configuration or error handling; logic in the wrong module or layer; tests that only verify mocks.

Template:

```
/thermo-nuclear-code-quality-review Responda em português do Brasil. Siga a seção "Code review context" do CLAUDE.md. Aceito neste PR: <itens com limite, ou nada>. Apertar: <riscos deste PR>.
```
