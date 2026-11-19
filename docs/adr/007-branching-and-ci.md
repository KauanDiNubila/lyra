# ADR-007: Branching strategy, pull requests and CI

- **Date**: 2026-11-18
- **Status**: Proposed
- **Deciders**: Kauan Di Nubila
- **Tags**: process, git, ci

## Context and Problem Statement

Work must not land directly on `main`. Every change goes through a pull request that is reviewed (including the thermo-nuclear code quality review) and checked by CI before it is merged.

## Decision Drivers

1. `main` always holds released, working code.
2. Every change is reviewable on its own.
3. CI is the gate, not memory.
4. Low process overhead for a one-person project.

## Considered Options

1. **GitHub Flow**: short branches from `main`, pull request back to `main`.
2. **GitHub Flow with a `develop` integration branch**: short branches from `develop`, pull request to `develop`; `develop` is released to `main` through a pull request.
3. **Full GitFlow**: `develop`, `release/*`, `hotfix/*`.

## Decision Outcome

Chosen option: **GitHub Flow with a `develop` integration branch**.

### Branches

| Branch | Purpose | Created from | Merges into |
|---|---|---|---|
| `main` | Released code | | |
| `develop` | Integration of finished work; the repository's **default branch** | `main` | `main`, through a release pull request |
| `feat/<slice-or-topic>` | New behaviour | `develop` | `develop` |
| `fix/<topic>` | Bug fix | `develop` | `develop` |
| `docs/`, `test/`, `refactor/`, `build/`, `ci/` | Other changes | `develop` | `develop` |
| `hotfix/<topic>` | Urgent fix on released code | `main` | `main`, then `main` is merged back into `develop` |

Branch names use kebab-case, for example `feat/slice-1-skeleton`.

### Commits and pull requests

- Commit messages and pull request titles follow [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `build:`, `ci:`, `chore:`, optionally with a scope (`feat(diary): ...`).
- Pull requests into `develop` use **Merge pull request** (a merge commit): every commit of the branch lands on `develop` with the same hash CI tested, plus one merge commit that groups the pull request. Each commit must still be small, build on its own and have a valid message, and CI checks every commit message of the pull request. Squash merge is disabled in the repository settings, and the `develop` ruleset accepts only merge commits.
- `develop` is the default branch, so the work shows up on GitHub (default view, contribution graph) the day a pull request is merged, not only on release.
- Release pull requests from `develop` into `main` use a **merge commit**, so `main` keeps the history of what each release contained. Their title is `release: <version or summary>`; hotfix pull requests into `main` use `fix: ...`.
- No `Co-Authored-By` trailers or tool attribution in commits or pull requests.

### Pull request cycle

1. Branch from `develop`.
2. Implement with tests; run the checks locally.
3. Before opening the pull request, run the thermo-nuclear code quality review on the branch; findings are discussed and the chosen ones fixed.
4. Open the pull request; CI must pass.
5. Merge.

### CI

Workflow `.github/workflows/ci.yml`, on pushes to `main` and `develop` and on pull requests into them:

| Job | Steps |
|---|---|
| `conventional-commits` | Pull requests only: every commit message must match Conventional Commits; release and hotfix pull requests into `main` are checked by title |
| `backend` | `./mvnw verify`: unit tests, ArchUnit, Testcontainers integration tests, JaCoCo coverage gate |
| `frontend` | `npm ci`, lint, tests, build |

A new push to the same branch cancels the run in progress. Dependabot opens weekly pull requests into `develop` for Maven, npm and GitHub Actions, grouping minor and patch updates.

Later, when there is something to check: CodeQL, Playwright end-to-end tests (from slice 4), OpenAPI contract diff.

### Branch protection

The repository is public, so GitHub enforces rulesets on the free plan. `main` and `develop` reject direct pushes, deletion and force pushes, and require a pull request with the CI jobs passing. The rulesets are created right after both branches exist on GitHub.

### Consequences

- Good: `main` only changes through a reviewed release.
- Good: every change has a pull request and a CI run, and each small commit is kept in the history.
- Bad: the history of `develop` is not linear; read it with `git log --first-parent` to see one line per pull request. A branch must not be reused after its pull request is merged (branches are deleted automatically).
- Bad: two merges per change reach `main` (branch to `develop`, `develop` to `main`); acceptable because releases are batched.
- Bad: hotfixes must be merged back into `develop` or the fix is lost on the next release.
