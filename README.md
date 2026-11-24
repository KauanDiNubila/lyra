# Lyra

A diet tracker built around the plan your nutritionist gave you.

Most trackers start from an empty day and make you type everything again. If you follow a prescribed plan, you eat about the same thing every day, so that effort is wasted. Lyra turns it around: **the plan is the default, and the diary only records what changed.**

## How it works

1. **Register your plan once.** Meals, foods, quantities and daily targets (calories, protein, carbohydrate and fat).
2. **Open any day already filled.** Lyra copies the active plan into that day's diary.
3. **Record only the differences.** More rice, a swapped food, a skipped item, something extra. Totals per meal and per day are recalculated, and you see how the day compares to the plan and to your targets.

Foods come from the Brazilian TACO table, and you can add your own. Your plans, diary and custom foods are yours alone: every account is isolated from the others.

## Status

Lyra is under development and not available to the public yet.

- Done: foods (TACO search and custom foods), diary, diet plan, filling the diary from the plan, Google sign in and per user data.
- Next: exporting and deleting your account.
- Planned: quick add for restaurant food, barcode lookup with Open Food Facts, body weight, and an installable app that reads today's diary offline.

The full scope and the reasoning are in [RFC-001](docs/rfc/001-lyra-mvp.md).

## Built with

Java 25 and Spring Boot on the backend (hexagonal architecture, PostgreSQL) and a React PWA with the Astryx design system on the frontend. The architectural decisions are recorded in [docs/adr](docs/adr/), in Brazilian Portuguese.

## Running it locally

You need JDK 25, Node.js and Docker.

```bash
cd backend && ./mvnw spring-boot:test-run
cd frontend && npm install && npm run dev
```

Then open `http://localhost:5173`. The setup details (Google sign in, environment variables, tests and workflow) are in [CONTRIBUTING.md](CONTRIBUTING.md).

## Data sources and credits

- **TACO**, the Brazilian Food Composition Table, from the University of Campinas.
- **Open Food Facts** for barcodes, once that slice is built.

## License

Not decided yet.
