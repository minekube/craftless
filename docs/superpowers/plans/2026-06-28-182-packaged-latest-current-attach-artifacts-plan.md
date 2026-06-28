# Packaged Latest Current Attach Artifacts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and run a packaged latest-current probe that proves the packaged CLI can create, attach, connect, and capture generated live artifacts for `latest-release`.

**Architecture:** Reuse `LocalMinecraftServerSmoke` for the real Minecraft server and add a small packaged CLI action script for the Craftless daemon/client side. Use Bun through mise for HTTP/JSON helper calls inside the script, preserving the repo rule that dependencies come through mise.

**Tech Stack:** Bash, mise, Bun, Gradle, Ktor-backed Craftless packaged CLI, LocalMinecraftServerSmoke.

---

### Task 1: Red Distribution Guard

**Files:**
- Modify: `playwright/src/distribution.test.ts`

- [ ] **Step 1: Write the failing test**

Add a distribution test:

```ts
test("packaged latest current probe is a mise-managed product surface", () => {
  const mise = read(".mise.toml");
  const script = read("scripts/packaged-latest-current-probe.sh");

  expect(mise).toContain("[tasks.packaged-latest-current-probe]");
  expect(mise).toContain("CRAFTLESS_LOCAL_SERVER_SMOKE=1");
  expect(mise).toContain("scripts/packaged-latest-current-probe.sh");
  expect(mise).toContain("mise run package-cli");
  expect(script).toContain("build/docker/craftless/bin/craftless");
  expect(script).toContain("--version latest-release");
  expect(script).toContain("clients-create-latest-release.log");
  expect(script).toContain("client-openapi-connected.json");
  expect(script).toContain("client-rpc-subscribe.json");
  expect(script).toContain("mise exec -- bun");
  expect(script).not.toContain("task.survival");
});
```

- [ ] **Step 2: Verify red**

Run:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
```

Expected: FAIL because `scripts/packaged-latest-current-probe.sh` and the mise
task do not exist yet.

### Task 2: Add Packaged Probe Script

**Files:**
- Create: `scripts/packaged-latest-current-probe.sh`

- [ ] **Step 1: Create the script**

Create an executable Bash script that:

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
CRAFTLESS_BIN="$ROOT/build/docker/craftless/bin/craftless"
ARTIFACTS_DIR="${CRAFTLESS_SMOKE_ARTIFACTS_DIR:-$ROOT/build/craftless-packaged-latest-current-probe/artifacts}"
WORKSPACE="${CRAFTLESS_PACKAGED_LATEST_WORKSPACE:-$ROOT/build/craftless-packaged-latest-current-probe/workspace}"
CLIENT_ID="${CRAFTLESS_PACKAGED_LATEST_CLIENT_ID:-latest-current}"
SERVER_PORT="${CRAFTLESS_SMOKE_SERVER_PORT:?CRAFTLESS_SMOKE_SERVER_PORT is required}"
DAEMON_PORT="${CRAFTLESS_PACKAGED_LATEST_DAEMON_PORT:-18084}"
API="http://127.0.0.1:$DAEMON_PORT"

mkdir -p "$ARTIFACTS_DIR" "$WORKSPACE"
cp "$ROOT/build/docker/craftless/driver-mods.json" "$ARTIFACTS_DIR/packaged-driver-mods.json"
```

Then implement cleanup, daemon start, client create, attach polling, connect,
connected OpenAPI polling, artifact capture, JSON-RPC subscribe/query helpers,
client stop, and summary writing.

- [ ] **Step 2: Use Bun helpers**

Use `mise exec -- bun --eval '<javascript>'` for:

- polling `GET /events` for `client.attached`;
- polling `GET /clients/{id}/openapi.json` until client/player/inventory/world
  resources are present;
- POSTing JSON-RPC query/subscribe/unsubscribe requests;
- writing `packaged-probe-summary.json`.

- [ ] **Step 3: Make the script executable**

Run:

```sh
chmod +x scripts/packaged-latest-current-probe.sh
```

### Task 3: Add Mise Task

**Files:**
- Modify: `.mise.toml`

- [ ] **Step 1: Add task**

Add:

```toml
[tasks.packaged-latest-current-probe]
description = "Run packaged latest-release create/attach/connect artifact probe"
run = [
    "mise run package-cli",
    "CRAFTLESS_LOCAL_SERVER_SMOKE=1 CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT=build/craftless-packaged-latest-current-probe CRAFTLESS_SMOKE_MINECRAFT_VERSION=latest-release CRAFTLESS_SMOKE_ACTION_TIMEOUT_MS=900000 CRAFTLESS_SMOKE_ACTION_COMMAND_JSON='[\"scripts/packaged-latest-current-probe.sh\"]' mise exec -- gradle :testkit:classes :driver-fabric:fabricClientSmoke",
]
```

- [ ] **Step 2: Verify red guard becomes green**

Run:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
```

Expected: PASS.

### Task 4: Run Live Packaged Probe

**Files:**
- Create: `docs/superpowers/evidence/2026-06-28-packaged-latest-current-attach-artifacts.md`
- Modify: `docs/project-completion-checklist.md`
- Modify: `docs/superpowers/phase-index.md`

- [ ] **Step 1: Run the probe**

Run:

```sh
mise run packaged-latest-current-probe
```

Expected success artifacts:

- `build/craftless-packaged-latest-current-probe/artifacts/client-openapi-connected.json`
- `build/craftless-packaged-latest-current-probe/artifacts/client-actions.json`
- `build/craftless-packaged-latest-current-probe/artifacts/client-resources.json`
- `build/craftless-packaged-latest-current-probe/artifacts/client-events-stream.sse`
- `build/craftless-packaged-latest-current-probe/artifacts/client-rpc-openapi.json`
- `build/craftless-packaged-latest-current-probe/artifacts/client-rpc-actions.json`
- `build/craftless-packaged-latest-current-probe/artifacts/client-rpc-resources.json`
- `build/craftless-packaged-latest-current-probe/artifacts/client-rpc-subscribe.json`
- `build/craftless-packaged-latest-current-probe/artifacts/client-events-subscription-stream.sse`
- `build/craftless-packaged-latest-current-probe/artifacts/packaged-probe-summary.json`

- [ ] **Step 2: Record result**

If the probe succeeds, mark CL-03c.2, CL-03d, and CL-03e.3 complete with the
new evidence file. If it fails, leave them open and record the exact blocker,
artifact paths, and cleanup status in the evidence file.

- [ ] **Step 3: Verify repo checks**

Run:

```sh
mise exec -- bun test playwright
git diff --check
```

- [ ] **Step 4: Commit and push**

Run:

```sh
git add .mise.toml scripts/packaged-latest-current-probe.sh playwright/src/distribution.test.ts docs/project-completion-checklist.md docs/superpowers/phase-index.md docs/superpowers/specs/2026-06-28-182-packaged-latest-current-attach-artifacts-design.md docs/superpowers/plans/2026-06-28-182-packaged-latest-current-attach-artifacts-plan.md docs/superpowers/evidence/2026-06-28-packaged-latest-current-attach-artifacts.md
git commit -m "test: probe packaged latest current attach"
git push origin main
```

## Self-Review

- Spec coverage: the plan covers a red guard, script, mise task, live packaged
  run, evidence, checklist, and phase index.
- Placeholder scan: no TBD/TODO/fill-in placeholders remain.
- Type consistency: artifact names in the spec, plan, script requirements, and
  checklist all use the same `build/craftless-packaged-latest-current-probe`
  root and `client-*` artifact names.
