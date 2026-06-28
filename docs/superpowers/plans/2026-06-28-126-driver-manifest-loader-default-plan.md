# Driver Manifest Loader Default Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let packaged driver-mod manifests steer the default Fabric Loader version for exact Minecraft runtime lanes.

**Architecture:** Keep `ClientRuntimeDriverModProvider` as the supervisor/runtime boundary and add a non-abstract preferred-loader hint with a default `null` implementation. `ConfiguredClientRuntimeDriverModProvider` reads the configured manifest before cache preparation and returns the manifest loader version for the requested Fabric/Minecraft lane when no loader version was explicitly requested. `WorkspaceClientRuntimeDriverFactory` passes that hint into `CachePrepareRequest`, while strict manifest matching remains enforced after cache preparation.

**Tech Stack:** Kotlin/JVM daemon and CLI tests, kotlinx serialization, Gradle through mise.

---

### Task 1: Governance

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/specs/2026-06-28-126-driver-manifest-loader-default-design.md`
- Create: `docs/superpowers/plans/2026-06-28-126-driver-manifest-loader-default-plan.md`

- [x] **Step 1: Add Phase 126 to AGENTS.md**

  Add `126. driver manifest loader default.` to the active product-completion
  sequence and define the phase rule: this is supervisor/runtime lane selection
  only, with no gameplay API breadth and no new support claim.

- [x] **Step 2: Add Phase 126 to the checklist**

  Update the final gate text so Phase 126 is tracked as another incomplete
  support-enabling slice, not project completion evidence by itself.

### Task 2: Add Red Provider Hint Test

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/ConfiguredClientRuntimeDriverModProviderTest.kt`

- [x] **Step 1: Add preferred loader test**

  Add a test named `manifest provides preferred loader version for matching fabric lane` that:

  ```kotlin
  val preferred =
      provider.preferredLoaderVersion(
          ClientRuntimeDriverModRequest(
              loader = Loader.FABRIC,
              minecraftVersion = "1.21.6",
              loaderVersion = null,
          ),
      )

  assertEquals("0.16.14", preferred)
  ```

- [x] **Step 2: Run red provider test**

  ```sh
  mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest.*preferred loader*'
  ```

  Expected: fail before implementation because `preferredLoaderVersion` does
  not exist yet.

### Task 3: Add Red Create-Client Lane Test

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt`
- Modify: `cli/src/test/kotlin/com/minekube/craftless/cli/CraftlessCliTest.kt`

- [x] **Step 1: Add daemon create-client test**

  Add a test named `prepared runtime defaults loader version from driver mod provider preference` that starts an in-memory server with Fabric metadata listing `0.17.2` first and `0.16.14` second, creates a Fabric `1.21.6` client without `loaderVersion`, and asserts the launcher prepared manifest is:

  ```text
  cache/prepared/1.21.6-fabric-0.16.14.json
  ```

- [x] **Step 2: Add packaged CLI manifest test**

  Add a CLI packaged-distribution test that creates `driver-mods.json` for
  `1.21.6` / `0.16.14`, creates a Fabric `1.21.6` client without
  `loaderVersion`, and asserts HTTP 201 plus cached content
  `manifest-driver-mod`.

- [x] **Step 3: Run red create-client tests**

  ```sh
  mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.*defaults loader version from driver mod provider preference*'
  mise exec -- gradle :cli:test --tests '*CraftlessCliTest.*defaults loader version from packaged driver mod manifest*'
  ```

  Expected: fail before implementation because cache preparation defaults to
  `0.17.2` and the manifest-backed `0.16.14` lane is not selected.

### Task 4: Implement Preferred Loader Hint

**Files:**
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt`

- [x] **Step 1: Extend provider contract**

  Add this default method while preserving the `fun interface` single abstract
  method:

  ```kotlin
  fun preferredLoaderVersion(request: ClientRuntimeDriverModRequest): String? = null
  ```

- [x] **Step 2: Use preference before cache preparation**

  In `WorkspaceClientRuntimeDriverFactory.prepare`, compute:

  ```kotlin
  val preferredLoaderVersion =
      request.loaderVersion
          ?: driverModProvider.preferredLoaderVersion(
              ClientRuntimeDriverModRequest(
                  loader = request.loader,
                  minecraftVersion = request.version,
                  loaderVersion = null,
              ),
          )
  ```

  Pass `preferredLoaderVersion` as `CachePrepareRequest.loaderVersion`.

- [x] **Step 3: Implement configured manifest preference**

  In `ConfiguredClientRuntimeDriverModProvider`, when
  `CRAFTLESS_DRIVER_MOD_MANIFEST` exists and the request is Fabric with no
  explicit loader version, return the matching manifest entry loader version
  for the requested Minecraft version. Return `null` for non-Fabric requests,
  missing manifests, explicit loader requests, and manifest entries without a
  loader version.

### Task 5: Evidence, Verification, Commit

**Files:**
- Create: `docs/superpowers/evidence/2026-06-28-driver-manifest-loader-default.md`

- [x] **Step 1: Run focused tests**

  ```sh
  mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest.*preferred loader*' --tests '*LocalSessionApiServerTest.*defaults loader version from driver mod provider preference*'
  mise exec -- gradle :cli:test --tests '*CraftlessCliTest.*defaults loader version from packaged driver mod manifest*'
  ```

- [x] **Step 2: Run local gates**

  ```sh
  git diff --check
  mise run ci
  ```

- [ ] **Step 3: Record evidence and push**

  ```sh
  git add AGENTS.md daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt daemon/src/test/kotlin/com/minekube/craftless/daemon/ConfiguredClientRuntimeDriverModProviderTest.kt daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt cli/src/test/kotlin/com/minekube/craftless/cli/CraftlessCliTest.kt docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-126-driver-manifest-loader-default-design.md docs/superpowers/plans/2026-06-28-126-driver-manifest-loader-default-plan.md docs/superpowers/evidence/2026-06-28-driver-manifest-loader-default.md
  git commit -m "feat: prefer driver manifest loader defaults"
  git push origin main
  ```

## Self-Review

- Spec coverage: provider hint, create-client default lane, explicit loader
  override, no-manifest fallback, strict manifest miss preservation, evidence,
  and local gates are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: this phase changes supervisor/runtime lane selection only; it does
  not add gameplay actions, scenario shortcuts, route families, compiled lanes,
  or public support claims.
