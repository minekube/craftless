# Official Fabric Public Projection Endpoints Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the official/latest connected attach probe write public `/actions` and `/resources` projection endpoint evidence.

**Architecture:** Reuse existing daemon projection endpoints and extend only the official probe evidence harness. The probe writes raw JSON artifacts and records simple machine-readable counts/ids in `probe-result.json`; no product route, action descriptor, operation adapter, CLI gameplay catalog, or scenario logic changes.

**Tech Stack:** Kotlin/JVM test harness, Ktor Client, existing daemon public projection endpoints, Gradle through mise.

---

### Task 1: Capture Public Projection Endpoint Artifacts

**Files:**
- Modify: `driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/probe/OfficialFabricAttachProbe.kt`

- [x] **Step 1: Verify current projection artifacts are missing**

Run:

```sh
test -f driver-fabric-official/build/craftless-official-attach-probe/client-actions.json
test -f driver-fabric-official/build/craftless-official-attach-probe/client-resources.json
```

Expected: FAIL before implementation because the official probe has not
written either public projection endpoint artifact.

- [x] **Step 2: Fetch and write the endpoint bodies**

In `OfficialFabricAttachProbe.run()`, after `client-openapi.json` is written,
fetch:

```kotlin
val actionsText = http.get("$daemonUrl/clients/${config.clientId}/actions").bodyAsText()
val resourcesText = http.get("$daemonUrl/clients/${config.clientId}/resources").bodyAsText()
```

Write:

```kotlin
config.artifactsDir.resolve("client-actions.json").writeText(actionsText + "\n")
config.artifactsDir.resolve("client-resources.json").writeText(resourcesText + "\n")
```

- [x] **Step 3: Record public action/resource evidence in result JSON**

Add helper functions:

```kotlin
private fun jsonArraySize(text: String): Int =
    (probeJson.parseToJsonElement(text) as? JsonArray).orEmpty().size

private fun publicResourceIds(resourcesText: String): List<String> =
    (probeJson.parseToJsonElement(resourcesText) as? JsonArray)
        .orEmpty()
        .mapNotNull { element -> element.jsonObject["id"]?.jsonPrimitive?.content }
```

Add fields to `OfficialFabricAttachProbeResult`:

```kotlin
val publicActionCount: Int = 0,
val publicResourceIds: List<String> = emptyList(),
```

Pass:

```kotlin
publicActionCount = jsonArraySize(actionsText),
publicResourceIds = publicResourceIds(resourcesText),
```

- [x] **Step 4: Run focused official tests**

Run:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
```

Expected: PASS.

### Task 2: Connected Probe Evidence And Docs

**Files:**
- Create: `docs/superpowers/evidence/2026-06-28-official-fabric-public-projection-endpoints.md`
- Modify: `docs/project-completion-checklist.md`
- Modify: `docs/superpowers/phase-index.md`
- Modify: `docs/superpowers/plans/2026-06-28-163-official-fabric-public-projection-endpoints-plan.md`

- [x] **Step 1: Run the connected official attach probe**

Run:

```sh
rm -rf driver-fabric-official/build/craftless-official-attach-probe
CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_CONNECT=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_TIMEOUT_MS=180000 \
mise exec -- gradle :driver-fabric-official:officialFabricAttachProbe
```

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 2: Inspect projection endpoint evidence**

Run:

```sh
jq -r '"actions=" + (length | tostring)' driver-fabric-official/build/craftless-official-attach-probe/client-actions.json
jq -r '.[].id' driver-fabric-official/build/craftless-official-attach-probe/client-resources.json
jq -r '{publicActionCount, publicResourceIds}' driver-fabric-official/build/craftless-official-attach-probe/probe-result.json
```

Expected: `actions=0`, resource ids include `runtime`, `registry`, `event`,
`client`, `player`, `inventory`, `world`, and `entity`, and
`probe-result.json` records the same public projection data.

- [x] **Step 3: Record evidence and checklist status**

Record red check, focused tests, connected probe command, public actions/resources
artifacts, parsed result fields, and boundary notes in:

```text
docs/superpowers/evidence/2026-06-28-official-fabric-public-projection-endpoints.md
docs/project-completion-checklist.md
docs/superpowers/phase-index.md
```

- [x] **Step 4: Run final verification and push**

Run:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
mise run fabric-lane-check-latest-official
mise run ci
git diff --check
git status --short --branch
```

Expected: all commands pass and the worktree only contains Phase 163 files.

Commit and push:

```sh
git add docs/project-completion-checklist.md docs/superpowers/phase-index.md docs/superpowers/specs/2026-06-28-163-official-fabric-public-projection-endpoints-design.md docs/superpowers/plans/2026-06-28-163-official-fabric-public-projection-endpoints-plan.md docs/superpowers/evidence/2026-06-28-official-fabric-public-projection-endpoints.md driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/probe/OfficialFabricAttachProbe.kt
git commit -m "test: capture official fabric projection endpoints"
git push origin main
```

## Self-Review

- Spec coverage: the plan captures `/actions` and `/resources`, records counts
  and ids, preserves `actions=0`, and avoids gameplay/API/product endpoint
  changes.
- Placeholder scan: no task uses TBD/TODO/fill-in wording.
- Type consistency: `publicActionCount`, `publicResourceIds`,
  `client-actions.json`, and `client-resources.json` are named consistently.
