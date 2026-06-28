# Representative Older Product Lane Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close CL-04 by proving the packaged representative older Minecraft `1.20.6` lane passes the same public product gate set as latest/current.

**Architecture:** Add a dedicated packaged older probe script and mise task, modeled on the CL-03 product probe but with separate client id, artifact root, daemon port, and concrete older Minecraft version. The probe uses only the packaged CLI/supervisor/API surfaces and selects generated operations from live per-client OpenAPI metadata.

**Tech Stack:** Bash, mise, Bun, Gradle, Ktor-backed Craftless packaged CLI, LocalMinecraftServerSmoke.

---

### Task 1: Red Distribution Guard

**Files:**

- Modify: `playwright/src/distribution.test.ts`

- [x] **Step 1: Add failing guard**

Add a test named `packaged representative older probe is a matching product surface` that reads `.mise.toml` and `scripts/packaged-representative-older-probe.sh`, then expects:

```ts
expect(mise).toContain("[tasks.packaged-representative-older-probe]");
expect(mise).toContain("CRAFTLESS_SMOKE_MINECRAFT_VERSION=1.20.6");
expect(mise).toContain("$PWD/scripts/packaged-representative-older-probe.sh");
expect(mise).toContain("mise run package-cli");
expect(script).toContain("build/docker/craftless/bin/craftless");
expect(script).toContain("--version 1.20.6");
expect(script).toContain("--loader-version 0.19.3");
expect(script).toContain("clients-create-representative-older.log");
expect(script).toContain("client-openapi-connected.json");
expect(script).toContain("client-rpc-subscribe.json");
expect(script).toContain("client-generated-action-selected.json");
expect(script).toContain("client-rpc-invoke-generated.json");
expect(script).toContain("client-cli-invoke-generated.log");
expect(script).toContain("x-craftless-actions");
expect(script).toContain('method: "invoke"');
expect(script).toContain('clients "$CLIENT_ID" run "$GENERATED_ACTION_ID"');
expect(script).toContain("mise exec -- bun");
expect(script).not.toContain("task.survival");
expect(script).not.toContain(":driver-fabric:runClient");
```

- [x] **Step 2: Verify red**

Run:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
```

Expected: fail because the older packaged probe script and task do not exist.

### Task 2: Add Packaged Older Probe

**Files:**

- Create: `scripts/packaged-representative-older-probe.sh`

- [x] **Step 1: Create executable script**

Create a Bash script with:

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
CRAFTLESS_BIN="$ROOT/build/docker/craftless/bin/craftless"
ARTIFACTS_DIR="${CRAFTLESS_SMOKE_ARTIFACTS_DIR:-$ROOT/build/craftless-packaged-representative-older-probe/artifacts}"
WORKSPACE="${CRAFTLESS_PACKAGED_OLDER_WORKSPACE:-$ROOT/build/craftless-packaged-representative-older-probe/workspace}"
CLIENT_ID="${CRAFTLESS_PACKAGED_OLDER_CLIENT_ID:-representative-older}"
SERVER_PORT="${CRAFTLESS_SMOKE_SERVER_PORT:?CRAFTLESS_SMOKE_SERVER_PORT is required}"
DAEMON_PORT="${CRAFTLESS_PACKAGED_OLDER_DAEMON_PORT:-18085}"
TIMEOUT_MS="${CRAFTLESS_PACKAGED_OLDER_TIMEOUT_MS:-300000}"
API="http://127.0.0.1:$DAEMON_PORT"
GENERATED_ACTION_ID=""
```

Use the same product flow as the latest probe:

1. start packaged daemon;
2. capture `supervisor-openapi.json`;
3. run `craftless clients create "$CLIENT_ID" --version 1.20.6 --loader fabric --loader-version 0.19.3 --offline-name OlderProduct`;
4. wait for `client.attached`;
5. run `craftless clients "$CLIENT_ID" connect --host 127.0.0.1 --port "$SERVER_PORT"`;
6. wait for connected generated OpenAPI with resources `client`, `player`, `inventory`, and `world`;
7. capture actions/resources/events/query/subscription artifacts;
8. select an available no-required-argument action from `x-craftless-actions`;
9. invoke the selected action through JSON-RPC `method: "invoke"`;
10. invoke the selected action through `craftless clients "$CLIENT_ID" run "$GENERATED_ACTION_ID"`;
11. write `packaged-probe-summary.json`;
12. stop the client and packaged daemon in cleanup.

Use `mise exec -- bun --eval` for JSON/HTTP helpers. Do not use `node`, `npm`,
`jq`, or Python.

- [x] **Step 2: Make executable**

Run:

```sh
chmod +x scripts/packaged-representative-older-probe.sh
```

### Task 3: Add Mise Task

**Files:**

- Modify: `.mise.toml`

- [x] **Step 1: Add task**

Add:

```toml
[tasks.packaged-representative-older-probe]
description = "Run packaged representative older create/attach/connect artifact probe"
run = [
    "mise run package-cli",
    "CRAFTLESS_LOCAL_SERVER_SMOKE=1 CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT=build/craftless-packaged-representative-older-probe CRAFTLESS_SMOKE_MINECRAFT_VERSION=1.20.6 CRAFTLESS_SMOKE_ACTION_TIMEOUT_MS=900000 CRAFTLESS_PACKAGED_OLDER_TIMEOUT_MS=900000 CRAFTLESS_SMOKE_ACTION_COMMAND_JSON=\"[\\\"$PWD/scripts/packaged-representative-older-probe.sh\\\"]\" mise exec -- gradle :driver-fabric:fabricClientSmoke",
]
```

- [x] **Step 2: Verify guard green**

Run:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
```

Expected: pass.

### Task 4: Run Live Older Product Probe

**Files:**

- Create: `docs/superpowers/evidence/2026-06-28-representative-older-product-lane.md`
- Modify: `docs/project-completion-checklist.md`
- Modify: `docs/superpowers/phase-index.md`

- [x] **Step 1: Run probe**

Run:

```sh
mise run packaged-representative-older-probe
```

- [x] **Step 2: Inspect artifacts**

Read:

```sh
driver-fabric/build/craftless-packaged-representative-older-probe/artifacts/packaged-probe-summary.json
driver-fabric/build/craftless-packaged-representative-older-probe/artifacts/client-generated-action-selected.json
driver-fabric/build/craftless-packaged-representative-older-probe/artifacts/client-rpc-invoke-generated.json
driver-fabric/build/craftless-packaged-representative-older-probe/artifacts/client-cli-invoke-generated.log
driver-fabric/build/craftless-packaged-representative-older-probe/artifacts/server-evidence.jsonl
```

- [x] **Step 3: Record evidence**

If the probe exits `0`, write the evidence file and mark CL-04 `[x]`.
If it exits non-zero, write the exact blocker, artifact paths, and next
diagnostic command; do not close CL-04.

### Task 5: Final Verification, Commit, Push

- [x] Run:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
git diff --check
```

- [ ] Commit and push directly to `main`.

## Self-Review

- Spec coverage: this plan covers representative version choice, packaged
  create/attach/connect, generated OpenAPI/projections, SSE, JSON-RPC
  query/subscription, generated invocation, adaptive CLI invocation, evidence,
  checklist, phase index, commit, and push.
- Placeholder scan: no TODO/TBD/fill-in placeholders remain.
- Type consistency: artifact roots and names consistently use
  `build/craftless-packaged-representative-older-probe` and `client-*`
  artifacts.
