# Official Fabric Connected SSE Evidence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the official/latest connected attach probe write public client SSE evidence from `/clients/{id}/events:stream`.

**Architecture:** Reuse the existing Ktor daemon SSE route and extend only the official probe evidence harness. The probe records raw SSE plus parsed event types in its result JSON; no product route, action descriptor, operation adapter, or CLI gameplay catalog changes.

**Tech Stack:** Kotlin/JVM test harness, Ktor Client, existing daemon SSE endpoint, Gradle through mise.

---

### Task 1: Capture Official Client SSE Evidence

**Files:**
- Modify: `driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/probe/OfficialFabricAttachProbe.kt`

- [x] **Step 1: Verify the current probe artifact is missing**

Run:

```sh
test -f driver-fabric-official/build/craftless-official-attach-probe/client-events-stream.sse
```

Expected: FAIL with exit code `1` before implementation because the probe has
not written a client SSE artifact.

- [x] **Step 2: Fetch and write the client SSE stream**

In `OfficialFabricAttachProbe.run()`, after writing `client-openapi.json`, add:

```kotlin
val clientEventsStreamText = http.get("$daemonUrl/clients/${config.clientId}/events:stream").bodyAsText()
config.artifactsDir.resolve("client-events-stream.sse").writeText(clientEventsStreamText + "\n")
```

Use `clientEventsStreamText` when building `OfficialFabricAttachProbeResult`.

- [x] **Step 3: Add parsed event types to the result**

Add this helper near `connectedResourceIds(...)`:

```kotlin
private fun sseEventTypes(sse: String): List<String> =
    sse
        .lineSequence()
        .mapNotNull { line ->
            line.removePrefix("event: ").takeIf { eventType -> eventType != line }
        }.toList()
```

Add this field to `OfficialFabricAttachProbeResult`:

```kotlin
val streamedEventTypes: List<String> = emptyList(),
```

Pass:

```kotlin
streamedEventTypes = sseEventTypes(clientEventsStreamText),
```

- [x] **Step 4: Run focused official tests**

Run:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
```

Expected: PASS.

### Task 2: Connected Probe Evidence And Docs

**Files:**
- Create: `docs/superpowers/evidence/2026-06-28-official-fabric-connected-sse-evidence.md`
- Modify: `docs/project-completion-checklist.md`
- Modify: `docs/superpowers/phase-index.md`
- Modify: `docs/superpowers/plans/2026-06-28-162-official-fabric-connected-sse-evidence-plan.md`

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

- [x] **Step 2: Inspect SSE and OpenAPI evidence**

Run:

```sh
grep '^event: ' driver-fabric-official/build/craftless-official-attach-probe/client-events-stream.sse
jq -r '.streamedEventTypes[]' driver-fabric-official/build/craftless-official-attach-probe/probe-result.json
jq -r '"actions=" + (."x-craftless-actions" | length | tostring)' driver-fabric-official/build/craftless-official-attach-probe/client-openapi-connected.json
```

Expected: output includes `event: client.attached`, `event: client.connected`,
`client.attached`, `client.connected`, and `actions=0`.

- [x] **Step 3: Record evidence and checklist status**

Record red check, focused tests, connected probe command, SSE events,
`streamedEventTypes`, `actions=0`, and boundary notes in:

```text
docs/superpowers/evidence/2026-06-28-official-fabric-connected-sse-evidence.md
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

Expected: all commands pass and the worktree only contains Phase 162 files.

Commit and push:

```sh
git add docs/project-completion-checklist.md docs/superpowers/phase-index.md docs/superpowers/specs/2026-06-28-162-official-fabric-connected-sse-evidence-design.md docs/superpowers/plans/2026-06-28-162-official-fabric-connected-sse-evidence-plan.md docs/superpowers/evidence/2026-06-28-official-fabric-connected-sse-evidence.md driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/probe/OfficialFabricAttachProbe.kt
git commit -m "test: capture official fabric sse evidence"
git push origin main
```

## Self-Review

- Spec coverage: the plan captures official public SSE evidence, parses event
  types, preserves `actions=0`, and avoids new gameplay/API/product transport
  changes.
- Placeholder scan: no task uses TBD/TODO/fill-in wording.
- Type consistency: `streamedEventTypes`, `sseEventTypes(...)`, and
  `client-events-stream.sse` are named consistently.
