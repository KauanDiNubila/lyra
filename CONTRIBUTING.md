# Contributing to Lyra

How to set up the project, run the checks and work with the branches. For what Lyra is, see the [README](README.md).

## Repository layout

| Path | Content |
|---|---|
| `backend/` | Spring Boot 4.1, Java 25, Maven wrapper |
| `frontend/` | React 19, Vite, Astryx, vite-plugin-pwa |
| `docs/rfc/`, `docs/adr/` | RFCs and architecture decision records (in Brazilian Portuguese) |
| `docs/api/openapi.json` | HTTP contract, checked by `OpenApiContractTest` |
| `tools/taco/` | One-off converter from the TACO spreadsheet to the seed TSV |
| `.github/` | CI workflow and Dependabot |

Read [RFC-001](docs/rfc/001-lyra-mvp.md) for the scope and the domain model, and the [ADRs](docs/adr/) for every structural decision. A change that contradicts an ADR needs a new ADR first.

## Architecture

The backend is a modular monolith with hexagonal architecture. Each module (`food`, `plan`, `diary` and `identity`, with `weight` to come) lives under `app.lyra.<module>`, next to a small `shared` kernel for ids and errors:

```
domain/                     plain Java, no Spring, no JPA
application/port/in/        use case interfaces
application/port/out/       what the application needs from outside
application/service/        use case implementations
adapter/in/web/             controllers
adapter/out/persistence/    JPA entities and repositories
```

`domain` depends only on the JDK, `application` never depends on an adapter, and a module reaches another one only through its `application.port.in` or its `domain`. `HexagonalArchitectureTest` enforces these rules in the build, so never weaken a rule to make a build pass. See [ADR-001](docs/adr/001-hexagonal-modular-monolith.md).

Every user owns their plans, diary entries and custom foods, and every repository query filters by the signed in user. The session cookie is `Secure`, `HttpOnly` and `SameSite=Lax`, and every write needs the CSRF token. See [ADR-012](docs/adr/012-user-identity-and-data-ownership.md).

## Running locally

You need JDK 25, Node.js, and Docker running (the database runs in a container). The machine's `JAVA_HOME` must point to JDK 25 for the backend commands.

**Backend**, on port 8080:

```bash
cd backend
./mvnw spring-boot:test-run
```

`spring-boot:test-run` starts `TestLyraApplication`, which brings up PostgreSQL in a container and fills the Google settings with fake values. The sign in only works with real ones: create an OAuth client in the Google console (type "Web application", origin `http://localhost:5173`, redirect URI `http://localhost:5173/login/oauth2/code/google`) and export `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` before starting. Never put them in the repository.

**Frontend**, on port 5173:

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server proxies `/api`, `/oauth2` and `/login` to the backend and keeps the browser's `Host`, so the sign in returns to the right address.

Outside the test entry point, the application needs `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`, and it fails to start without them. The session cookie is `Secure`, so a deployment needs HTTPS.

## Tests and checks

```bash
cd backend && ./mvnw verify
cd frontend && npm run lint && npm test && npm run build
cd tools/taco && python -X utf8 -m unittest
```

- The backend integration tests use Testcontainers, so Docker must be running.
- JaCoCo fails the build below 90% line and branch coverage in `domain` and `application`.
- Every change ships with tests in the same pull request. The strategy is in [ADR-002](docs/adr/002-testing-strategy.md).
- When the API changes, `OpenApiContractTest` fails until you review the diff of `backend/target/openapi.json` and copy it to `docs/api/openapi.json`. The `/v3/api-docs` endpoint is off by default and only that test turns it on.

## Food data

The Brazilian foods come from the TACO table. They are seeded by a database migration from a generated file, and `tools/taco/` is the converter that produced it. Users can add custom foods, which only they can see. Sources and rules are in [ADR-006](docs/adr/006-food-data-sources.md) and [ADR-008](docs/adr/008-food-catalog-import-and-api.md).

## Workflow

- `develop` is the default branch. Work happens on branches named `feat/...`, `fix/...`, `docs/...`, `test/...`, `refactor/...`, `build/...` or `ci/...`, and never directly on `main` or `develop`.
- Pull requests into `develop` use a merge commit. Every commit follows [Conventional Commits](https://www.conventionalcommits.org/), and CI checks each one. Releases go from `develop` to `main` with a merge commit titled `release: ...`. See [ADR-007](docs/adr/007-branching-and-ci.md).
- Code, commits and pull request text are in English. RFCs and ADRs are in Brazilian Portuguese.
- No code comments, and no new dependency without a reason in an ADR or in the pull request.
