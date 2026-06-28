# Representative Older Fabric Real-Client Smoke Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Capture Codex-verifiable real-client smoke evidence for the representative older Minecraft `1.20.6` Fabric lane.

**Architecture:** Use the existing opt-in `fabricClientSmoke` harness after Phase 140, passing the older lane Gradle properties so the local server, runtime-lane evidence, and inner `runClient` process all use the same lane. Record artifacts and limitations in docs without adding product gameplay API breadth.

**Tech Stack:** Gradle, Fabric Loom, local Minecraft server smoke testkit, mise.

---

### Task 1: Governance

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/specs/2026-06-28-141-representative-older-fabric-real-client-smoke-design.md`
- Create: `docs/superpowers/plans/2026-06-28-141-representative-older-fabric-real-client-smoke-plan.md`

- [x] **Step 1: Record Phase 141 governance**

  State that representative older real-client smoke is diagnostic runtime
  evidence, not final honest survival completion and not installed packaged CLI
  proof.

### Task 2: Run The Older Smoke

**Files:**
- Evidence root: `/tmp/craftless-fabric-smoke-older-lane`

- [x] **Step 1: Run the smoke**

  ```sh
  rm -rf /tmp/craftless-fabric-smoke-older-lane &&
  CRAFTLESS_FABRIC_CLIENT_SMOKE=1 \
  CRAFTLESS_SMOKE_MINECRAFT_VERSION=1.20.6 \
  CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT=/tmp/craftless-fabric-smoke-older-lane \
  CRAFTLESS_SMOKE_ACTION_TIMEOUT_MS=420000 \
  mise exec -- gradle :driver-fabric:fabricClientSmoke \
    -Pcraftless.fabric.minecraftVersion=1.20.6 \
    -Pcraftless.fabric.yarnMappings=1.20.6+build.3 \
    -Pcraftless.fabric.loaderVersion=0.19.3 \
    -Pcraftless.fabric.apiVersion=0.100.8+1.20.6 \
    -Pcraftless.fabric.javaMajorVersion=21 \
    -Pcraftless.fabric.laneId=fabric-1-20-6-lane \
    -Pcraftless.fabric.providerId=fabric-1-20-6-lane \
    -Pcraftless.fabric.artifactKey=fabric-1-20-6-remap-jar \
    -Pcraftless.fabric.mappingsFingerprint=craftless-fabric-bindings-1-20-6
  ```

- [x] **Step 2: Inspect artifacts**

  Read:

  ```sh
  /tmp/craftless-fabric-smoke-older-lane/artifacts/runtime-lane.json
  /tmp/craftless-fabric-smoke-older-lane/artifacts/runtime-metadata.json
  /tmp/craftless-fabric-smoke-older-lane/artifacts/server-evidence.jsonl
  /tmp/craftless-fabric-smoke-older-lane/artifacts/gameplay-results.jsonl
  /tmp/craftless-fabric-smoke-older-lane/artifacts/client-events-stream.sse
  /tmp/craftless-fabric-smoke-older-lane/artifacts/client-openapi-connected.json
  /tmp/craftless-fabric-smoke-older-lane/artifacts/client-actions-connected.json
  /tmp/craftless-fabric-smoke-older-lane/artifacts/client-resources-connected.json
  ```

### Task 3: Evidence And Push

**Files:**
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-representative-older-fabric-real-client-smoke.md`

- [x] **Step 1: Record evidence**

  Include command result, artifact paths, runtime metadata, generated API
  artifacts, SSE event evidence, server evidence, generated action results, and
  the diagnostic limitation.

- [ ] **Step 2: Run docs hygiene**

  ```sh
  git diff --check
  ```

- [ ] **Step 3: Commit and push**

  ```sh
  git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-141-representative-older-fabric-real-client-smoke-design.md docs/superpowers/plans/2026-06-28-141-representative-older-fabric-real-client-smoke-plan.md docs/superpowers/evidence/2026-06-28-representative-older-fabric-real-client-smoke.md
  git commit -m "docs: record older fabric real smoke"
  git push origin main
  ```

## Self-Review

- Spec coverage: command, artifacts, runtime metadata, generated APIs, SSE,
  server evidence, action evidence, and limitations are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no public gameplay API, static gameplay catalog, route family, or
  scenario shortcut.
