# ADR-008: Food catalog: TACO import, search and API contract

- **Date**: 2026-11-19
- **Status**: Proposed
- **Deciders**: Kauan Di Nubila
- **Tags**: food, data, api

## Context and Problem Statement

Slice 2 of [RFC-001](../rfc/001-lyra-mvp.md) delivers the food catalog: Brazilian generic foods from TACO ([ADR-006](006-food-data-sources.md)), text search and custom foods. Three things had to be decided: how the TACO spreadsheet becomes rows, how search behaves, and what the HTTP contract (including errors) looks like. This also closes open question 3 of the RFC.

## Decision Outcome

### TACO import

Source: the official TACO spreadsheet published by NEPA/Unicamp (`tabela-taco-excel`, first sheet), 597 foods per 100 g. Reproduction is allowed with citation:

> NEPA/UNICAMP. Tabela Brasileira de Composição de Alimentos (TACO). 4. ed. rev. e ampl. Campinas: NEPA/UNICAMP, 2011.

The spreadsheet is converted once by `tools/taco/convert_taco.py` into `backend/src/main/resources/db/taco/taco-foods.tsv`, which is committed. The Flyway Java migration `V2__Seed_taco_foods` inserts it. To regenerate:

```bash
python -I tools/taco/convert_taco.py <taco.xlsx> backend/src/main/resources/db/taco/taco-foods.tsv
```

The converter reads nutrients by column position, so it first checks the name and unit of each column in the spreadsheet header (`HEADER` in the script) and aborts if any differs, instead of silently storing a shifted column. Its tests run with `python -X utf8 -m unittest` inside `tools/taco`; they are not part of the CI.

Value rules, from the spreadsheet legend:

| Spreadsheet value | Meaning in TACO | Stored as |
|---|---|---|
| number | measured | the number, rounded to 4 decimal places |
| `Tr` | trace, below the rounding or quantification limit | `0` |
| `NA` | not applicable (protein in oil, fiber in meat) | `0` |
| blank | not analysed | unknown (`null`) |
| `*` | under re-evaluation | unknown (`null`) |

- kcal, protein, carbohydrate and fat are required. A food with any of them unknown is **skipped**: 5 foods (pineapple yogurt, skimmed and whole UHT milk, cachaça, green coconut). Users can add them as custom foods.
- Fiber and sodium may be unknown.
- TACO computes carbohydrate by difference, so measurement error produces small negatives (4 foods, between -0.045 and 0 g). Negative carbohydrate is stored as `0`. Any other negative value fails the conversion.
- Result: 592 foods. Ids are deterministic (`UUID` from `taco:<number>`), so every environment has the same ids.

### Database

- Nutrient and amount columns are `numeric` without precision or scale: the database never rounds silently; values are stored as the domain gives them.
- `search_name` holds the name normalized by the domain's `SearchText` (no accents, lower case, single spaces). Changing `SearchText` requires a migration that recomputes `search_name`.

### Search

- The query is normalized like the names, punctuation is removed and it is split into terms; it needs at least 2 characters.
- A food matches when its normalized name contains **every** term ("feijao cozido" finds "Feijão, carioca, cozido").
- The persistence adapter escapes `%`, `_` and `\` in every term before building the `LIKE`, so a term is always literal text, whatever the domain strips.
- Order: custom foods first, then shorter names, then alphabetical. At most 20 results. The order is applied by the database because the limit is too, so it belongs to the persistence adapter; `FoodRepositoryContract` states it once and runs against both the in-memory fake and the real adapter.
- Until the identity module exists, custom foods are visible to every user.

### HTTP contract

| Method | Path | Success | Errors |
|---|---|---|---|
| GET | `/api/foods?query=` | `200` list of foods | `400` query missing or shorter than 2 characters |
| POST | `/api/foods` | `201` created food | `400` invalid name, amount, unit or nutrients, missing objects, malformed JSON |

- Errors use **Problem Details** ([RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)), `application/problem+json`, with `status`, `title` and `detail`.
- Every rule is checked in the domain, including a missing `referenceAmount` or `nutrients` object (no bean validation in the web adapter). These errors have the title `Invalid input` and the domain message as detail. Errors raised by Spring before the domain is reached (malformed JSON, unknown unit, missing `query`) keep Spring's default Problem Details, with the title `Bad Request`.
- Numbers are returned with full precision; rounding for display is done by the frontend.
- The OpenAPI document is committed at `docs/api/openapi.json`. `OpenApiContractTest` fails when the generated document differs, so any contract change shows up in the pull request diff.
- springdoc is a test dependency: the document is generated in the test and the application does not expose `/v3/api-docs`. The document declares `application/json` for successes and `application/problem+json` with the `ProblemDetail` schema for the `400`.
- Because presence is checked in the domain, the schema no longer lists `required` fields. A client reading only the document sees every field as optional; the server still answers `400` when one is missing.

### Consequences

- Good: Brazilian foods available offline from the first start, with values traceable to the source.
- Good: unknown values stay unknown instead of becoming zero.
- Good: one error format (Problem Details) for the whole API, and one place for each rule.
- Bad: the contract does not mark required fields. Marking them needs `swagger-annotations` in the main classpath (or bean validation again); revisit if a second client appears.
- Bad: whole milk and four other common foods are missing until TACO re-evaluates them or the user adds them.
- Bad: `contains` search on every term has no index; acceptable for hundreds of rows, to revisit with a trigram index when custom foods grow.
