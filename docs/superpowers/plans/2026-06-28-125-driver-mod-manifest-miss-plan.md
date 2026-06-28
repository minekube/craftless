# Driver Mod Manifest Miss Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make packaged driver-mod manifests authoritative for Fabric runtime-lane selection.

**Architecture:** Keep `ClientRuntimeDriverModProvider` returning a nullable path for unconfigured providers, but make `ConfiguredClientRuntimeDriverModProvider` throw when a configured manifest has no matching Fabric lane. The existing server create-client error path turns that into HTTP 400, preventing launches without a compatible Craftless driver mod.

**Tech Stack:** Kotlin/JVM daemon and CLI tests, kotlinx serialization, Gradle through mise.

---

### Task 1: Add Red Provider Guard

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/ConfiguredClientRuntimeDriverModProviderTest.kt`

- [x] **Step 1: Replace fallback-on-manifest-miss expectation**

  Change the manifest-miss test to assert that `modFor(...)` throws
  `IllegalArgumentException` for a Fabric request when
  `CRAFTLESS_DRIVER_MOD_MANIFEST` is configured and contains no matching
  runtime lane, even if `CRAFTLESS_FABRIC_DRIVER_MOD` is set.

- [x] **Step 2: Add no-manifest fallback guard**

  Add a separate test proving `CRAFTLESS_FABRIC_DRIVER_MOD` still works when
  `CRAFTLESS_DRIVER_MOD_MANIFEST` is absent.

- [x] **Step 3: Run red provider tests**

  ```sh
  mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest.*'
  ```

  Expected: fail before implementation because manifest misses still fall back
  to the single Fabric driver mod.

### Task 2: Add Red Packaged CLI Guard

**Files:**
- Modify: `cli/src/test/kotlin/com/minekube/craftless/cli/CraftlessCliTest.kt`

- [x] **Step 1: Add packaged manifest miss test**

  Add a test named `server start rejects packaged driver mod manifest misses`.
  It should create a distribution root containing:

  - `driver-mods.json` for `1.21.6` / `0.17.2`;
  - `mods/craftless-driver-fabric.jar` containing `fallback-driver-mod`.

  Start `craftless server start --once`, post a Fabric client create request
  for `version=1.21.6` and `loaderVersion=0.16.14`, and assert:

  - client creation returns HTTP 400;
  - the response mentions `driver mod manifest`;
  - no `cache/mods/craftless` directory exists.

- [x] **Step 2: Run red CLI test**

  ```sh
  mise exec -- gradle :cli:test --tests '*CraftlessCliTest.*driver mod manifest*'
  ```

  Expected: fail before implementation because the fallback driver jar is
  copied and client creation succeeds.

### Task 3: Implement Authoritative Manifest Miss

**Files:**
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt`

- [x] **Step 1: Split configured manifest lookup**

  In `ConfiguredClientRuntimeDriverModProvider.modFor`, check
  `CRAFTLESS_DRIVER_MOD_MANIFEST` first. If present, decode it and try to find
  a matching entry.

- [x] **Step 2: Throw on Fabric manifest miss**

  If the manifest is present, no entry matches, and `request.loader == Loader.FABRIC`,
  throw:

  ```kotlin
  IllegalArgumentException(
      "driver mod manifest has no Fabric entry for ${request.minecraftVersion} ${request.loaderVersion ?: "default-loader"}"
  )
  ```

- [x] **Step 3: Preserve unconfigured fallback**

  Only use `CRAFTLESS_FABRIC_DRIVER_MOD` when no manifest is configured.

### Task 4: Governance, Evidence, Verification

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-driver-mod-manifest-miss.md`

- [x] **Step 1: Record Phase 125 governance/checklist**
- [x] **Step 2: Record red/green/local evidence**
- [x] **Step 3: Run focused and full local gates**

  ```sh
  mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest.*' --tests '*LocalSessionApiServerTest.*driver mod manifest*'
  mise exec -- gradle :cli:test --tests '*CraftlessCliTest.*driver mod manifest*'
  git diff --check
  mise run ci
  ```

- [x] **Step 4: Commit and push**

  ```sh
  git add AGENTS.md daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt daemon/src/test/kotlin/com/minekube/craftless/daemon/ConfiguredClientRuntimeDriverModProviderTest.kt cli/src/test/kotlin/com/minekube/craftless/cli/CraftlessCliTest.kt docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-125-driver-mod-manifest-miss-design.md docs/superpowers/plans/2026-06-28-125-driver-mod-manifest-miss-plan.md docs/superpowers/evidence/2026-06-28-driver-mod-manifest-miss.md
  git commit -m "fix: reject driver mod manifest lane misses"
  git push origin main
  ```

## Self-Review

- Spec coverage: manifest authority, Fabric miss error, exact match, fallback
  without manifest, packaged CLI guard, governance, evidence, and local gates
  are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no gameplay action, route family, CLI gameplay catalog, Fabric
  binding, scenario shortcut, compiled lane, public version-specific API, or
  support claim.
