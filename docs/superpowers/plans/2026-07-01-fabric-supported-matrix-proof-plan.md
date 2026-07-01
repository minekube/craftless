# Fabric Supported Matrix Proof Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add automated product-surface proof for every currently supported Fabric support-target row.

**Architecture:** Reuse the existing packaged Fabric smoke harness and add one generic packaged Fabric lane probe script. Wire a `1.21.6` current-lane task and a supported-matrix task that discovers supported rows from the packaged daemon, validates them against `driver-mods.json`, and runs the generic lane probe for each supported descriptor. Guard the distribution surface with Playwright tests and record evidence in docs.

**Tech Stack:** Bash, Bun eval snippets, Gradle/mise task orchestration, GitHub Actions, Playwright/Bun tests.

---

### Task 1: Current-Lane Probe Script

**Files:**
- Create: `scripts/packaged-fabric-lane-probe.sh`
- Modify: `.mise.toml`

- [x] **Step 1: Create the generic packaged Fabric lane probe**

Create `scripts/packaged-fabric-lane-probe.sh`. It must:

- require `CRAFTLESS_PACKAGED_FABRIC_VERSION`;
- accept optional `CRAFTLESS_PACKAGED_FABRIC_LOADER_VERSION`;
- start the packaged `craftless daemon`;
- create a client through `craftless api /clients`;
- wait for `client.attached`;
- connect to the smoke server;
- capture generated OpenAPI/actions/resources/events/RPC artifacts;
- invoke one available generated no-argument action through JSON-RPC and
  `craftless api /clients/{id}:run`;
- write `packaged-probe-summary.json`.

- [x] **Step 2: Add the `packaged-current-lane-probe` mise task**

Add a task that runs:

```sh
mise run package-cli
CRAFTLESS_LOCAL_SERVER_SMOKE=1 \
CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT=build/craftless-packaged-current-lane-probe \
CRAFTLESS_SMOKE_MINECRAFT_VERSION=1.21.6 \
CRAFTLESS_PACKAGED_FABRIC_VERSION=1.21.6 \
CRAFTLESS_PACKAGED_FABRIC_LOADER_VERSION=0.19.3 \
CRAFTLESS_PACKAGED_FABRIC_LABEL=current-lane \
CRAFTLESS_PACKAGED_FABRIC_CLIENT_ID=current-lane \
CRAFTLESS_PACKAGED_FABRIC_DAEMON_PORT=18086 \
CRAFTLESS_PACKAGED_FABRIC_TIMEOUT_MS=900000 \
CRAFTLESS_SMOKE_ACTION_TIMEOUT_MS=900000 \
CRAFTLESS_SMOKE_ACTION_COMMAND_JSON="[\"$PWD/scripts/packaged-fabric-lane-probe.sh\"]" \
mise exec -- gradle :driver-fabric:fabricClientSmoke
```

Expected result: the task proves the packaged `1.21.6` lane can create,
attach, connect, expose generated OpenAPI, and invoke a generated action.

### Task 2: Supported Matrix Orchestration

**Files:**
- Modify: `.mise.toml`
- Create: `.github/workflows/fabric-support-matrix.yml`

- [x] **Step 1: Add `packaged-fabric-supported-matrix-probe`**

Add a mise task that packages the CLI and runs
`scripts/packaged-fabric-supported-matrix-probe.sh`. The script must start the
packaged daemon, fetch `/versions/runtime-targets` and
`/versions/support-targets`, validate supported rows against the packaged
`driver-mods.json`, write `probe-jobs.json`, and run
`scripts/packaged-fabric-lane-probe.sh` for each supported driver-mod
descriptor.

```sh
mise run package-cli
bash scripts/packaged-fabric-supported-matrix-probe.sh
```

Expected result: every currently supported packaged Fabric support target has
an automated product-surface probe generated from the public support-target
contract.

- [x] **Step 2: Add the scheduled/manual workflow**

Create `.github/workflows/fabric-support-matrix.yml` with:

- `workflow_dispatch`;
- a cron schedule;
- Ubuntu runner with mise;
- `mise run packaged-fabric-supported-matrix-probe`;
- artifact upload for the generated matrix root and the historical named probe
  artifact directories.

### Task 3: Distribution Guards And Evidence

**Files:**
- Modify: `playwright/src/distribution.test.ts`
- Modify: `docs/project-completion-checklist.md`
- Modify: `docs/superpowers/phase-index.md`
- Create: `docs/superpowers/evidence/2026-07-01-fabric-supported-matrix-proof.md`

- [x] **Step 1: Add Playwright distribution tests**

Add tests that assert:

- `.mise.toml` contains `packaged-current-lane-probe`;
- `.mise.toml` contains `CRAFTLESS_PACKAGED_FABRIC_VERSION=1.21.6`;
- `.mise.toml` contains `packaged-fabric-supported-matrix-probe`;
- `scripts/packaged-fabric-supported-matrix-probe.sh` fetches
  `/versions/support-targets` and writes `probe-jobs.json`;
- `.github/workflows/fabric-support-matrix.yml` runs
  `mise run packaged-fabric-supported-matrix-probe`.

- [x] **Step 2: Record evidence and phase status**

Add an evidence file with the commands run and their results. Update the
phase index and checklist with Phase 199.

- [x] **Step 3: Verify**

Run:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
git diff --check
```

For deeper proof when runtime time is acceptable, run:

```sh
mise run packaged-current-lane-probe
```

Expected result: focused distribution tests and whitespace checks pass; the
current-lane probe passes when run in a windowless-capable environment.
