# OpenAPI Route Authority Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make client route projections come from the generated per-client OpenAPI document rather than a separate driver action-list projection.

**Architecture:** Keep `openApiFor(clientId)` as the document authority. Add a daemon-local conversion from `OpenApiDocument.paths` back to `ApiRoute` metadata using the operation extensions emitted by protocol OpenAPI generation. Change public `routesFor(clientId)` to return that conversion.

**Tech Stack:** Kotlin, Ktor daemon service tests, Craftless protocol OpenAPI/route DTOs, Gradle through mise.

---

### Task 1: Add The Regression Test

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/ClientSessionServiceTest.kt`

- [ ] **Step 1: Write the failing test**

Add a test with a `DriverSession` whose `runtimeGraph()` exposes `player.chat`
but whose `actions()` throws. Assert that `routesFor("alice")` still includes
`/clients/alice/player:chat` and matches `openApiFor("alice").paths`.

- [ ] **Step 2: Run the focused test**

Run:

```sh
mise exec -- gradle :daemon:test --tests '*ClientSessionServiceTest.client route list is projected from generated runtime graph openapi*'
```

Expected: FAIL because current `routesFor(clientId)` calls `actions()`.

### Task 2: Use OpenAPI As Route Authority

**Files:**
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/ClientSessionService.kt`

- [ ] **Step 1: Add OpenAPI route conversion**

Add private helpers that map `OpenApiDocument.paths` `get` and `post`
operations into `ApiRoute`, preserving operation id, tag, owner, member,
target, source, return kind, and action id from `x-craftless` extensions.

- [ ] **Step 2: Change public routesFor**

Change `routesFor(clientId)` to call `openApiFor(clientId).toApiRoutes()`.
Keep the existing private `routesFor(clientId, actions)` helper for the
legacy fallback inside `openApiFor`.

- [ ] **Step 3: Run the focused test**

Run:

```sh
mise exec -- gradle :daemon:test --tests '*ClientSessionServiceTest.client route list is projected from generated runtime graph openapi*'
```

Expected: PASS.

### Task 3: Verify And Record Evidence

**Files:**
- Modify: `docs/superpowers/phase-index.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-openapi-route-authority.md`

- [ ] **Step 1: Run focused daemon tests**

Run:

```sh
mise exec -- gradle :daemon:test --tests '*ClientSessionServiceTest*'
```

Expected: PASS.

- [ ] **Step 2: Run broader verification**

Run:

```sh
mise exec -- gradle :daemon:test :protocol:test
git diff --check
```

Expected: PASS.

- [ ] **Step 3: Update docs**

Add Phase 168 to the phase index and checklist, and record the verification
commands in the evidence file.

- [ ] **Step 4: Commit and push**

Run:

```sh
git add daemon/src/main/kotlin/com/minekube/craftless/daemon/ClientSessionService.kt daemon/src/test/kotlin/com/minekube/craftless/daemon/ClientSessionServiceTest.kt docs/project-completion-checklist.md docs/superpowers/phase-index.md docs/superpowers/specs/2026-06-28-168-openapi-route-authority-design.md docs/superpowers/plans/2026-06-28-168-openapi-route-authority-plan.md docs/superpowers/evidence/2026-06-28-openapi-route-authority.md
git commit -m "refactor: derive client routes from openapi"
git push origin main
```
