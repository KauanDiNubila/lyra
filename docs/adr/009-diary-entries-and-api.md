# ADR-009: Diary: entries, calculation and API contract

- **Date**: 2026-11-19
- **Status**: Proposed
- **Deciders**: Kauan Di Nubila
- **Tags**: diary, domain, data, api

## Context and Problem Statement

Slice 3 of [RFC-001](../rfc/001-lyra-mvp.md) delivers the diary backend: log foods by date and meal, totals per meal and per day, edit and remove entries. The foods come from the catalog of [ADR-008](008-food-catalog-import-and-api.md) and each entry keeps a copy of the food ([ADR-003](003-nutrient-snapshot-on-log.md)). Still to decide: what a "day" and a "meal" are, how the macros are calculated, how entries are ordered, and what the HTTP contract looks like.

## Decision Outcome

### Day and meal

- A **day is derived**: `GET /api/diary/{date}` reads the entries of that date. There is no table of days, and a date without entries is an empty day (`200`, no meals, zero totals).
- A **meal is a name**: free text of 1 to 50 characters (trimmed) stored in each entry. The day groups entries by exact name, in the order of the first entry of each meal. Names are case-sensitive: "Almoço" and "almoço" are two meals.
- The client sends the date; the server never decides what "today" is.
- Entries have no owner until the identity module exists, the same rule as custom foods ([RFC-001](../rfc/001-lyra-mvp.md), open question 1).

### Entry and snapshot

An entry stores its id, date, meal name, `foodId`, and the snapshot of the food: name, reference amount and nutrients. The unit of the quantity must be the unit of the food's reference amount, and the quantity must be greater than zero; otherwise the domain rejects the entry. Only the quantity can be changed afterwards. Changing the meal or the food is not supported yet.

`diary_entry.food_id` is a foreign key to `food` with no cascade, so a food cannot be deleted while a diary entry points to it. There is no way to delete a food today.

### Calculation

- Macros of an entry = `nutrient × quantity ÷ reference amount`, for kcal, protein, carbohydrate and fat. The multiplication comes first and is exact; there is one division, with `MathContext.DECIMAL128` (34 digits).
- Meal total = sum of its entries; day total = sum of its meals. Fiber and sodium are kept in the snapshot but are not totalled.
- Nothing is rounded in the backend or in the API. The frontend rounds for display, as stated in the RFC.

### Order

Entries come back in the order they were logged. The table has an `entry_order` column, `generated always as identity`, which only grows, has no ties and does not depend on the clock. Changing the quantity of an entry keeps its place.

### HTTP contract

| Method | Path | Success | Errors |
|---|---|---|---|
| GET | `/api/diary/{date}` | `200` day with meals, entries and totals | `400` malformed date |
| POST | `/api/diary/{date}/entries` | `201` entry | `404` unknown food; `400` invalid meal name, quantity or unit, missing `foodId` or `quantity` |
| PATCH | `/api/diary/entries/{id}` | `200` entry | `404` unknown entry; `400` invalid quantity or unit, missing `quantity` |
| DELETE | `/api/diary/entries/{id}` | `204` | `404` unknown entry |

- Errors use Problem Details, as in ADR-008. Rule violations have the title `Invalid input`; a missing food or entry has the title `Not found`, with the exception message as detail. Errors raised by Spring before the domain is reached (malformed date or JSON, unknown unit) keep the default `Bad Request`.
- The delete is permanent and it answers `404` for an id that does not exist, so the client can tell a repeated delete from a successful one.
- The web adapter of the diary repeats two small pieces of the food adapter: `QuantityJson` and the handling of `InvalidInputException`. [ADR-001](001-hexagonal-modular-monolith.md) forbids a module from using the adapter of another, so the copy is intended. When a second module needs `InvalidInputException`, move it to a shared place and remove the copy.
- The OpenAPI document `docs/api/openapi.json` was extended with the three diary paths; no food path changed.

### Consequences

- Good: no table of days, so no day to create, lock or keep in sync with its entries.
- Good: past days never change when a food changes, and the calculation can be reproduced from the entry alone.
- Good: one decision for order and one for rounding, both stated once.
- Bad: meal names are free text and case-sensitive, so a typo makes a new meal. A fixed list of meals, or the meals of the plan, would solve it; slice 4 will revisit it.
- Bad: only the quantity can change. Moving an entry to another meal or swapping its food means deleting and logging again until slice 4.
- Bad: the API returns long decimals such as `0.3333333333333333333333333333333333`, and every client has to round.
- Bad: the diary does not pre-fill from the plan yet; the day is empty until entries are logged.
