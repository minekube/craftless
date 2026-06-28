# Packaged Older Lane Selection Smoke Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prove the supervisor create-client path selects the packaged representative older Fabric driver lane from a multi-entry manifest.

**Architecture:** Reuse the existing in-memory `LocalSessionApiServer` and `ConfiguredClientRuntimeDriverModProvider`. Extend only test metadata helpers so the daemon test can prepare a `1.20.6` Fabric runtime with a matching Fabric API artifact, then assert the launch plan stages the older packaged driver jar.

**Tech Stack:** Kotlin, Ktor test HTTP client, Gradle daemon tests, mise.

---

### Task 1: Governance And Red Test

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/specs/2026-06-28-139-packaged-older-lane-selection-smoke-design.md`
- Create: `docs/superpowers/plans/2026-06-28-139-packaged-older-lane-selection-smoke-plan.md`
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt`

- [x] **Step 1: Record Phase 139 governance**

  Add Phase 139 to the active sequence and checklist. State that this phase is
  packaged lane selection evidence only, not older runtime support completion.

- [x] **Step 2: Write the failing daemon smoke**

  Add a `LocalSessionApiServerTest` test named
  `prepared runtime selects packaged older fabric lane from manifest`. The
  test must:

  ```kotlin
  val distribution = Files.createTempDirectory("craftless-packaged-older-lane-selection")
  val currentDriverMod = distribution.resolve("mods/craftless-driver-fabric.jar")
  val olderDriverMod = distribution.resolve("mods/fabric-1.20.6/craftless-driver-fabric.jar")
  Files.createDirectories(olderDriverMod.parent)
  Files.writeString(currentDriverMod, "current-driver-mod")
  Files.writeString(olderDriverMod, "older-driver-mod")
  ```

  Then write a two-entry `driver-mods.json`, create a `1.20.6` Fabric client,
  and assert the staged launch mods contain `older-driver-mod` and not
  `current-driver-mod`.

- [x] **Step 3: Run the test red**

  ```sh
  mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.prepared runtime selects packaged older fabric lane from manifest'
  ```

  Expected: fail because the helper metadata does not yet prepare a `1.20.6`
  Fabric runtime.

### Task 2: Test Metadata Support

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt`

- [x] **Step 1: Add an older runtime metadata fixture**

  Add `preparedRuntimeMetadataFetcherWithOlderLane()` returning
  `ServerStaticCacheMetadataFetcher` entries for `1.21.6` and `1.20.6`,
  Fabric loader `0.19.3`, Fabric API `0.100.8+1.20.6`, Java major version 21,
  and older Fabric loader/profile jars.

- [x] **Step 2: Keep the existing current-lane helper unchanged**

  Existing tests that call `preparedRuntimeMetadataFetcher()` must continue to
  use the current `1.21.6` fixture.

### Task 3: Verification And Evidence

**Files:**
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-packaged-older-lane-selection-smoke.md`

- [x] **Step 1: Run focused daemon tests**

  ```sh
  mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.prepared runtime selects packaged older fabric lane from manifest'
  mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest*' --tests '*LocalSessionApiServerTest.*driver mod*'
  ```

- [x] **Step 2: Run local hygiene**

  ```sh
  git diff --check
  ```

- [x] **Step 3: Record evidence**

  Write the red test output, green test output, and caveat that launch/attach,
  generated OpenAPI, and gameplay evidence remain outstanding.

- [ ] **Step 4: Commit and push**

  ```sh
  git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-139-packaged-older-lane-selection-smoke-design.md docs/superpowers/plans/2026-06-28-139-packaged-older-lane-selection-smoke-plan.md docs/superpowers/evidence/2026-06-28-packaged-older-lane-selection-smoke.md daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt
  git commit -m "test: prove older driver lane selection"
  git push origin main
  ```

## Self-Review

- Spec coverage: selection smoke, older metadata fixture, focused tests,
  evidence, and no runtime-support claim are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no public gameplay API, static gameplay catalog, route family, or
  scenario shortcut.
