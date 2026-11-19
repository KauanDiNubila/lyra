# ADR-006: Food data sources

- **Date**: 2026-11-18
- **Status**: Proposed
- **Deciders**: Kauan Di Nubila
- **Tags**: domain, integrations

## Context and Problem Statement

Lyra needs nutrition data for Brazilian generic foods (rice, beans), packaged products (by barcode) and restaurant items (a McDonald's milkshake). No single API covers all three for Brazil at no cost.

Research on 2026-11-18 (prices and limits from third-party pages, to be confirmed on the official sites):

| Source | Coverage | Cost | Limitation |
|---|---|---|---|
| TACO (NEPA/Unicamp), 4th edition | Brazilian generic foods (about 600) | Free, reproduction allowed with citation | No official API; third-party APIs are unmaintained |
| TBCA (FoRC/USP) | Brazilian foods (3,000+) | Free to consult | No public API |
| Open Food Facts | Packaged products, barcode | Free, no key for reads | Crowdsourced, quality varies |
| USDA FoodData Central | Generic foods, US focus | Free, API key | English, US products |
| FatSecret Platform | Global, has a Brazil dataset | Free basic tier | Brazil data requires a paid country licence |
| Nutritionix (Syndigo) | Restaurants, natural language | Free only for testing | US focus, expensive paid tiers |

## Decision Drivers

1. Brazilian foods first.
2. No recurring cost for the MVP.
3. Searches must work offline once a food has been used.
4. Adding a source later must not touch the domain.

## Decision Outcome

One outbound port, `FoodCatalog`, with one adapter per source, queried in this order:

1. **User foods**: custom foods and quick adds already saved.
2. **TACO**, imported once into Lyra's database with a Flyway migration, citing the source.
3. **Open Food Facts** for barcodes and packaged products, with a `User-Agent` that identifies the app as their terms ask.
4. **USDA FoodData Central** as a fallback for generic foods.

A food chosen from an external source is stored locally on first use, so later searches hit the database.

Restaurant items are covered by **quick add** (name, portion, calories and macros typed by the user from the restaurant's published table) and are reused from step 1. An AI estimation adapter is a later RFC.

FatSecret and Nutritionix are rejected for the MVP because the data that matters (Brazil, restaurants) is behind paid licences.

### Consequences

- Good: zero cost, Brazilian data first, offline after first use.
- Good: each source is an isolated adapter with its own WireMock tests.
- Bad: restaurant items need manual input the first time.
- Bad: TACO has about 600 foods; TBCA could be imported later if coverage is not enough.
