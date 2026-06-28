# Latest Official Mapping Lane Probe Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an executable latest/current Fabric lane compile probe that uses the official/Mojang-mapping boundary and records status evidence without claiming support.

**Architecture:** Keep the existing verified Yarn/remap compiled lanes untouched. Add a separate mise probe task and architecture guard test so 26.x work is measured through official mapping mode and status artifacts instead of static matrix entries or gameplay shortcuts. Official mode removes the Yarn `mappings` dependency for the latest lane.

**Tech Stack:** mise, Gradle, Fabric Loom, Kotlin/JVM, Fabric API, repository phase docs.

---

### Task 1: Guard the latest official probe task

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Write the failing test**

Add a test named:

```kotlin
@Test
fun `mise latest lane probe uses official mapping boundary not yarn remap lane`()
```

It should read `.mise.toml` and assert that:

- `fabric-lane-check-latest-official` exists;
- `craftless.fabric.mappingMode=official` is passed;
- `craftless.fabric.minecraftVersion=26.2` is passed;
- `craftless.fabric.apiVersion=0.153.0+26.2` is passed;
- `craftless.fabric.javaMajorVersion=25` is passed;
- `fabric-lane-check-latest-official.status` is written;
- `fabric-lane-check-latest-official.log` is written;
- the latest task block does not pass `craftless.fabric.yarnMappings`.

- [x] **Step 2: Run the test red**

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.mise latest lane probe uses official mapping boundary not yarn remap lane*'
```

Expected before implementation: failure because `.mise.toml` has no latest
official mapping probe task.

### Task 2: Implement the mise probe

**Files:**
- Modify: `.mise.toml`

- [x] **Step 1: Add `fabric-lane-check-latest-official`**

The task should run a Gradle compile/resource/catalog probe with:

```sh
mise exec -- gradle :driver-fabric:compileKotlin :driver-fabric:processResources :driver-fabric:writeFabricDriverLaneCatalog \
  -Pcraftless.fabric.mappingMode=official \
  -Pcraftless.fabric.minecraftVersion=26.2 \
  -Pcraftless.fabric.loaderVersion=0.19.3 \
  -Pcraftless.fabric.apiVersion=0.153.0+26.2 \
  -Pcraftless.fabric.javaMajorVersion=25 \
  -Pcraftless.fabric.laneId=fabric-latest-official-lane \
  -Pcraftless.fabric.providerId=fabric-latest-official-lane \
  -Pcraftless.fabric.artifactKey=fabric-latest-official-jar \
  -Pcraftless.fabric.mappingsFingerprint=craftless-fabric-official-bindings-26-2 \
  -Pcraftless.fabric.distributionPath=mods/fabric-26.2/craftless-driver-fabric.jar
```

- [x] **Step 2: Record status**

Write:

- `build/reports/fabric-lane-check-latest-official.log`;
- `build/reports/fabric-lane-check-latest-official.status`.

If the Gradle command exits 0, write `status=compiled`. Otherwise write
`status=source-compatibility-blocked` plus a concrete blocker code. If the
current remap plugin rejects official mode with `Configuration 'mappings' has
no dependencies`, write `blockers=loom-remap-requires-mappings`.

- [x] **Step 3: Run the guard test green**

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.mise latest lane probe uses official mapping boundary not yarn remap lane*'
```

### Task 3: Execute and record the probe

**Files:**
- Create: `docs/superpowers/evidence/2026-06-28-latest-official-mapping-lane-probe.md`
- Modify: `docs/project-completion-checklist.md`

- [x] **Step 1: Run the latest official probe**

```sh
mise run fabric-lane-check-latest-official
```

- [x] **Step 2: Record evidence**

Record the command, status file, and blocker or compiled result. State
explicitly that the result is not a support claim until packaged launch, attach,
generated OpenAPI/actions/resources, SSE, and public gameplay evidence pass.

- [ ] **Step 3: Verify and commit**

```sh
git diff --check
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.mise latest lane probe uses official mapping boundary not yarn remap lane*'
git add AGENTS.md .mise.toml docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-145-latest-official-mapping-lane-probe-design.md docs/superpowers/plans/2026-06-28-145-latest-official-mapping-lane-probe-plan.md docs/superpowers/evidence/2026-06-28-latest-official-mapping-lane-probe.md driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt
git commit -m "build: add latest official fabric lane probe"
git push origin main
```
