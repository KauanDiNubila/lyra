# ADR-003: Snapshot nutrients when an item is logged

- **Date**: 2026-11-18
- **Status**: Proposed
- **Deciders**: Kauan Di Nubila
- **Tags**: domain, data

## Context and Problem Statement

A diary entry refers to a food. Food data changes: the user edits a custom food, the plan is edited, or Open Food Facts (crowdsourced) corrects a product. If a diary entry only stores a reference to the food, past days silently change their totals, and the history the user relies on to analyse the diet becomes unreliable.

## Decision Drivers

1. Past diary days must never change unless the user edits them.
2. The calculation must be reproducible from what is stored in the entry.

## Considered Options

1. **Reference only**: the entry stores the food id and the quantity; nutrients are read from the food at query time.
2. **Snapshot**: the entry stores the food id plus a copy of the name, reference amount and nutrients at log time.
3. **Versioned foods**: every change to a food creates a new version; entries point to a version.

## Decision Outcome

Chosen option: **snapshot**.

The diary entry stores `foodId` (for reuse and statistics), `foodName`, `referenceAmount` and `nutrients` copied at the moment it is created or swapped. Changing the quantity of an entry keeps its snapshot. Changing the food of an entry takes a new snapshot.

Versioned foods are rejected because they give the same guarantee with more tables and more queries.

### Consequences

- Good: history is stable and every total can be recomputed from the entry alone.
- Good: external sources can be down or change without affecting past days.
- Bad: duplicated data per entry (a few columns); negligible for one user.
- Bad: correcting a wrong food does not fix past days automatically; a future "reapply food to past entries" action can do it explicitly.
