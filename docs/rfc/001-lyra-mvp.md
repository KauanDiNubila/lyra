# RFC-001: Lyra MVP

- **Date**: 2026-11-18
- **Status**: Draft
- **Author**: Kauan Di Nubila
- **Related ADRs**: [001](../adr/001-hexagonal-modular-monolith.md), [002](../adr/002-testing-strategy.md), [003](../adr/003-nutrient-snapshot-on-log.md), [004](../adr/004-pwa-with-astryx.md), [005](../adr/005-backend-stack.md), [006](../adr/006-food-data-sources.md)

## 1. Summary

Lyra is a diet tracker built around a prescribed diet plan. The user registers the plan once (meals, foods and quantities), and every day Lyra opens a diary pre-filled from that plan. The user then records what actually happened: more rice, a skipped snack, a milkshake on the road. Every change recalculates the macros of the meal and of the day.

## 2. Motivation

Generic trackers (MyFitnessPal, Cronometer, MacroFactor) start from an empty day: the user logs everything from scratch. Someone following a nutritionist's plan eats roughly the same thing every day, so logging from scratch is wasted effort and the most common action, "I ate the plan, but 100 g more rice", has no first-class support.

Lyra inverts the flow: **the plan is the default, the diary records the deviations.**

## 3. Goals

1. Register a diet plan with meals, foods, quantities and daily macro targets.
2. Open a diary for any date, pre-filled from the active plan.
3. Adjust any planned item (quantity up or down, swap the food, remove it) and add extra items.
4. Recalculate macros per item, per meal and per day after every change, and show the difference against the plan and the targets.
5. Find foods by text (Brazilian generic foods first) and by barcode.
6. Log restaurant or unknown food with a quick add (name plus calories and macros) and reuse it later.
7. Work on the phone while travelling: installable, usable with a weak connection.
8. Log body weight.

## 4. Non-goals for the MVP

- Social features (friends, sharing, streaks).
- Adaptive calorie targets based on the weight trend.
- AI logging (free text, photo) and importing a nutritionist's PDF.
- Micronutrients beyond fiber and sodium, water tracking, reminders.
- Publishing to the Play Store or the App Store.

Each of these is a candidate for a later RFC. The architecture must not block them (see §7).

## 5. Domain model

### 5.1 Concepts

| Concept | Meaning |
|---|---|
| **Food** | Something that can be eaten, with nutrients per a reference amount (100 g, 100 ml or 1 portion) and its source (TACO, Open Food Facts, USDA, custom). |
| **Nutrients** | Value object: kcal, protein, carbohydrate, fat, fiber, sodium. Supports `scale(factor)` and `plus(other)`. |
| **Quantity** | Value object: amount plus unit (`GRAM`, `MILLILITER`, `PORTION`). Never negative. |
| **Diet plan** | Named set of planned meals plus daily targets. One plan is active per user at a time. |
| **Planned meal** | A slot in the plan (breakfast, lunch, ...) with an order and an optional time. |
| **Planned item** | A food and a quantity inside a planned meal. |
| **Diary day** | What the user ate on a date. Created from the active plan the first time the date is opened. |
| **Diary entry** | One eaten item: a snapshot of the food (name and nutrients), the quantity eaten, the planned quantity when it came from the plan, and its origin. |
| **Weight entry** | Body weight on a date. |

### 5.2 Diary entry origin

| Origin | Example |
|---|---|
| `PLANNED` | Rice 150 g, as planned |
| `ADJUSTED` | Rice planned 150 g, eaten 250 g; the entry keeps both and shows +100 g |
| `SWAPPED` | Planned chicken replaced by fish |
| `EXTRA` | An apple that was not in the plan |
| `QUICK_ADD` | "McDonald's milkshake", 1 portion, with nutrients typed by the user |

A planned item that was skipped is an entry with quantity zero, so the diary still shows what was planned and missed.

### 5.3 Calculation rules

- Entry nutrients = `snapshot.nutrients.scale(quantity / snapshot.referenceAmount)`. The units must match; a mismatch is a domain error, not a silent conversion.
- Meal total = sum of its entries. Day total = sum of its meals.
- Difference against the plan = day total minus the planned total; against the targets = day total minus the targets.
- Arithmetic uses `BigDecimal`. Rounding happens only at the presentation edge (one decimal place).

### 5.4 Why a snapshot

A diary entry copies the food's name and nutrients at the moment it is logged. If the plan changes or an external source corrects a product later, past days stay as they were. See [ADR-003](../adr/003-nutrient-snapshot-on-log.md).

## 6. Food data sources

All sources sit behind one outbound port, `FoodCatalog`, with one adapter per source. Details and trade-offs in [ADR-006](../adr/006-food-data-sources.md).

| Order | Source | Used for |
|---|---|---|
| 1 | Custom foods and quick adds of the user | Anything already logged once, including restaurant items |
| 2 | TACO, imported into Lyra's own database | Brazilian generic foods (rice, beans, chicken) |
| 3 | Open Food Facts | Packaged products, mainly by barcode |
| 4 | USDA FoodData Central | Fallback for generic foods missing from TACO |

External results are cached in the database the first time they are chosen, so the next search is local and works offline.

Restaurant food (the McDonald's case) has no free Brazilian source. The MVP covers it with quick add plus reuse; an AI estimation adapter can be added later behind the same port.

## 7. Architecture overview

- **Backend**: Java, hexagonal architecture (ports and adapters) as a modular monolith. Modules: `food`, `plan`, `diary`, `weight`, `identity`. See [ADR-001](../adr/001-hexagonal-modular-monolith.md) and [ADR-005](../adr/005-backend-stack.md).
- **Frontend**: React 19 with the Astryx design system, shipped as a Progressive Web App. See [ADR-004](../adr/004-pwa-with-astryx.md).
- **Tests**: unit tests for domain and application, integration tests for adapters, architecture rules enforced by ArchUnit. See [ADR-002](../adr/002-testing-strategy.md).

Future features fit as new adapters or new modules: AI logging is another `FoodCatalog` adapter, social is a new module that reads diary data through an inbound port, a native app is another client of the same HTTP API.

## 8. HTTP API (draft)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/foods?query=` | Search foods across sources |
| GET | `/api/foods/barcode/{code}` | Find a packaged product |
| POST | `/api/foods` | Create a custom food |
| GET, POST | `/api/plans` | List and create diet plans |
| GET, PUT | `/api/plans/{id}` | Read and replace a plan |
| POST | `/api/plans/{id}/activation` | Make a plan the active one |
| GET | `/api/diary/{date}` | Read the day, creating it from the active plan if needed |
| POST | `/api/diary/{date}/entries` | Add an extra item or a quick add |
| PATCH | `/api/diary/{date}/entries/{id}` | Change the quantity or swap the food |
| DELETE | `/api/diary/{date}/entries/{id}` | Remove an extra item |
| GET, POST | `/api/weights` | List and log body weight |

The contract will be specified with OpenAPI before the first endpoint is implemented.

## 9. MVP slices

Each slice is delivered end to end (domain, adapters, tests, UI) before the next starts.

1. Project skeleton: backend, frontend, CI, ArchUnit rules.
2. Foods: TACO import, search, custom food.
3. Diet plan: create, edit, activate.
4. Diary: open from plan, adjust, swap, extra, totals.
5. Quick add and reuse.
6. Barcode with Open Food Facts.
7. Weight log.
8. PWA: install, offline read of today's diary.

## 10. Open questions

1. **Authentication.** Single user at first, but social is on the roadmap. Options: Spring Security with session cookies, or an external provider. Needs its own ADR before slice 3.
2. **Offline writes.** The MVP reads offline; queuing writes offline and syncing later is harder (conflicts). Deferred to after slice 8.
3. **TACO edition and format.** The 4th edition is published as PDF and spreadsheets; the import format must be confirmed before slice 2.
4. **Hosting.** Not decided; does not block development.
