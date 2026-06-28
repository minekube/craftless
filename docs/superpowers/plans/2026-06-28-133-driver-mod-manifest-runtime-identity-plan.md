# Driver Mod Manifest Runtime Identity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make driver-mod manifest selection honor resolved runtime identity
fields so multi-version support cannot accidentally select an incompatible
driver artifact.

**Architecture:** Keep the private Fabric driver lane catalog as the build-time
source of truth. Project runtime identity metadata into the public packaged
manifest. Use optional manifest fields for backward compatibility, but enforce
them whenever the prepared runtime has the corresponding resolved value.

**Tech Stack:** Kotlin/JVM daemon tests, Gradle packaging tests, kotlinx
serialization, mise local verification.

---

### Task 1: Governance

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/specs/2026-06-28-133-driver-mod-manifest-runtime-identity-design.md`
- Create: `docs/superpowers/plans/2026-06-28-133-driver-mod-manifest-runtime-identity-plan.md`

- [x] **Step 1: Add Phase 133 to AGENTS.md**

  Define it as runtime-artifact safety, not support expansion.

- [x] **Step 2: Add Phase 133 to checklist**

  Track it as support-enabling work that does not satisfy latest/older support
  by itself.

### Task 2: Add Red Tests

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/ConfiguredClientRuntimeDriverModProviderTest.kt`
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Assert generated packaged manifests include runtime identity**

  Verify `driver-mods.json` includes `fabricApiVersion`, `javaMajorVersion`,
  and `mappingsFingerprint`, and still excludes `artifactKey` and
  `distributionPath`.

- [x] **Step 2: Assert mismatched Fabric API manifest entry is rejected**

  Configure a manifest entry for the same loader/Minecraft pair but a different
  `fabricApiVersion`; request the resolved Fabric API version and expect a
  manifest miss.

### Task 3: Implement Manifest Projection And Selection

**Files:**
- Modify: `.mise.toml`
- Modify: `cli/build.gradle.kts`
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt`
- Modify: `driver-fabric/build.gradle.kts`

- [x] **Step 1: Project runtime identity fields**

  Add `fabricApiVersion`, `javaMajorVersion`, and `mappingsFingerprint` to the
  generated public manifest.

- [x] **Step 2: Extend driver-mod request and manifest entry models**

  Add nullable `fabricApiVersion`, `javaMajorVersion`, and
  `mappingsFingerprint` request/entry fields.

- [x] **Step 3: Enforce known identity fields**

  Match optional manifest fields only when absent or equal to the request.
  Continue accepting old manifests with missing fields.

- [x] **Step 4: Populate request identity from prepared cache**

  Derive Fabric API version from the prepared Fabric API mod artifact and Java
  major version from the Java runtime selection requirement.

### Task 4: Evidence, Verification, Commit

**Files:**
- Create: `docs/superpowers/evidence/2026-06-28-driver-mod-manifest-runtime-identity.md`

- [x] **Step 1: Run focused tests**

  ```sh
  mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest.*'
  mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.*driver mod*'
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.cli driver mod manifest projection carries runtime identity not build fields*'
  mise exec -- gradle :cli:test --tests '*CraftlessCliTest.*driver mod manifest*'
  mise run package-cli
  ```

- [x] **Step 2: Run local gates**

  ```sh
  git diff --check
  mise run ci
  ```

- [ ] **Step 3: Commit and push**

  ```sh
  git add .mise.toml AGENTS.md docs/project-completion-checklist.md cli/build.gradle.kts driver-fabric/build.gradle.kts daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt daemon/src/test/kotlin/com/minekube/craftless/daemon/ConfiguredClientRuntimeDriverModProviderTest.kt daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt docs/superpowers/specs/2026-06-28-133-driver-mod-manifest-runtime-identity-design.md docs/superpowers/plans/2026-06-28-133-driver-mod-manifest-runtime-identity-plan.md docs/superpowers/evidence/2026-06-28-driver-mod-manifest-runtime-identity.md
  git commit -m "fix: match driver manifests by runtime identity"
  git push origin main
  ```

## Self-Review

- Spec covers packaging, selection, backward compatibility, and non-goals.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no runtime operation change, gameplay API, public route, compiled
  lane, Fabric dependency change, or support claim.
