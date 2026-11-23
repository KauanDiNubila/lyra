# ADR-007: Estratégia de branches, pull requests e CI

- **Data**: 2026-11-18
- **Status**: Proposta
- **Decisores**: Kauan Di Nubila
- **Tags**: processo, git, ci

## Contexto e problema

O trabalho não pode chegar direto na `main`. Toda mudança passa por um pull request que é revisado (incluindo o thermo-nuclear code quality review) e verificado pelo CI antes do merge.

## Fatores de decisão

1. A `main` sempre guarda código liberado e funcionando.
2. Toda mudança pode ser revisada por conta própria.
3. O CI é o portão, não a memória.
4. Pouco peso de processo para um projeto de uma pessoa só.

## Opções consideradas

1. **GitHub Flow**: branches curtas a partir da `main`, pull request de volta para a `main`.
2. **GitHub Flow com uma branch de integração `develop`**: branches curtas a partir da `develop`, pull request para a `develop`; a `develop` é liberada para a `main` por um pull request.
3. **GitFlow completo**: `develop`, `release/*`, `hotfix/*`.

## Decisão

Opção escolhida: **GitHub Flow com uma branch de integração `develop`**.

### Branches

| Branch | Finalidade | Criada a partir de | Entra em |
|---|---|---|---|
| `main` | Código liberado | | |
| `develop` | Integração do trabalho pronto; a **branch padrão** do repositório | `main` | `main`, por um pull request de release |
| `feat/<slice-or-topic>` | Comportamento novo | `develop` | `develop` |
| `fix/<topic>` | Correção de bug | `develop` | `develop` |
| `docs/`, `test/`, `refactor/`, `build/`, `ci/` | Outras mudanças | `develop` | `develop` |
| `hotfix/<topic>` | Correção urgente em código liberado | `main` | `main`, e depois a `main` é mergeada de volta na `develop` |

Os nomes de branch usam kebab-case, por exemplo `feat/slice-1-skeleton`.

### Commits e pull requests

- As mensagens de commit e os títulos dos pull requests seguem o [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `build:`, `ci:`, `chore:`, opcionalmente com escopo (`feat(diary): ...`).
- Os pull requests para a `develop` usam **Merge pull request** (um merge commit): todo commit da branch chega na `develop` com o mesmo hash que o CI testou, mais um merge commit que agrupa o pull request. Cada commit ainda precisa ser pequeno, compilar sozinho e ter uma mensagem válida, e o CI confere toda mensagem de commit do pull request. O squash merge está desativado nas configurações do repositório, e o ruleset da `develop` aceita só merge commits.
- A `develop` é a branch padrão, então o trabalho aparece no GitHub (visão padrão, gráfico de contribuições) no dia em que um pull request é mergeado, e não só na release.
- Os pull requests de release da `develop` para a `main` usam um **merge commit**, para a `main` manter o histórico do que cada release continha. O título deles é `release: <version or summary>`; os pull requests de hotfix para a `main` usam `fix: ...`.
- Sem trailers `Co-Authored-By` nem atribuição de ferramenta em commits ou pull requests.

### Ciclo do pull request

1. Criar a branch a partir da `develop`.
2. Implementar com testes; rodar as verificações localmente.
3. Antes de abrir o pull request, rodar o thermo-nuclear code quality review na branch; os achados são discutidos e os escolhidos são corrigidos.
4. Abrir o pull request; o CI precisa passar.
5. Fazer o merge.

### CI

Workflow `.github/workflows/ci.yml`, em pushes para `main` e `develop` e em pull requests para elas:

| Job | Passos |
|---|---|
| `conventional-commits` | Só em pull requests: toda mensagem de commit precisa seguir o Conventional Commits; os pull requests de release e de hotfix para a `main` são conferidos pelo título |
| `backend` | `./mvnw verify`: testes unitários, ArchUnit, testes de integração com Testcontainers, portão de cobertura do JaCoCo |
| `frontend` | `npm ci`, lint, testes, build |

Um novo push na mesma branch cancela a execução em andamento. O Dependabot abre pull requests semanais para a `develop` para Maven, npm e GitHub Actions, agrupando as atualizações menores e de patch.

Depois, quando houver o que conferir: CodeQL, testes de ponta a ponta com Playwright (a partir da slice 4), diff do contrato OpenAPI.

### Proteção de branch

O repositório é público, então o GitHub aplica rulesets no plano gratuito. A `main` e a `develop` rejeitam push direto, exclusão e force push, e exigem um pull request com os jobs de CI passando. Os rulesets são criados logo depois que as duas branches existirem no GitHub.

### Consequências

- Bom: a `main` só muda por uma release revisada.
- Bom: toda mudança tem um pull request e uma execução de CI, e cada commit pequeno fica no histórico.
- Ruim: o histórico da `develop` não é linear; leia com `git log --first-parent` para ver uma linha por pull request. Uma branch não deve ser reutilizada depois que o pull request dela é mergeado (as branches são apagadas automaticamente).
- Ruim: dois merges por mudança chegam à `main` (branch para `develop`, `develop` para `main`); aceitável porque as releases são feitas em lote.
- Ruim: os hotfixes precisam ser mergeados de volta na `develop`, senão a correção se perde na próxima release.
