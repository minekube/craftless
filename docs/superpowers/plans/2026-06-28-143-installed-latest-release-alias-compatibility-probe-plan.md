# Installed Latest Release Alias Compatibility Probe Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prove the installed packaged product's current `latest-release` Fabric behavior and record the exact latest/current blocker or runnable result.

**Architecture:** Use the packaged Craftless CLI distribution as the user-facing product surface. Capture live Mojang manifest data, start an isolated packaged supervisor, request a `latest-release` Fabric client, collect generated API evidence if it attaches or collect the unsupported/error artifact if it does not, then update docs without claiming final latest/current support.

**Tech Stack:** mise, packaged Craftless CLI, Ktor supervisor, Mojang version manifest, Fabric Loader/API resolution, driver-mod manifest selection.

---

### Task 1: Governance

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/specs/2026-06-28-143-installed-latest-release-alias-compatibility-probe-design.md`
- Create: `docs/superpowers/plans/2026-06-28-143-installed-latest-release-alias-compatibility-probe-plan.md`

- [x] **Step 1: Record Phase 143 governance**

  Add Phase 143 to the root phase list and checklist spec/plan section.

- [x] **Step 2: Write the design and plan**

  State that this phase refreshes installed `latest-release` behavior but does
  not satisfy final latest/current support unless a runnable attached 26.x lane
  is actually proven.

### Task 2: Capture Latest Manifest And Package

**Files:**
- Evidence root: `/tmp/craftless-packaged-latest-release-probe`

- [x] **Step 1: Capture live Mojang manifest**

  ```sh
  mkdir -p /tmp/craftless-packaged-latest-release-probe/artifacts
  curl -fsSL https://piston-meta.mojang.com/mc/game/version_manifest_v2.json \
    > /tmp/craftless-packaged-latest-release-probe/artifacts/version_manifest_v2.json
  jq -r '.latest' /tmp/craftless-packaged-latest-release-probe/artifacts/version_manifest_v2.json
  ```

  Expected: current latest release and snapshot are printed.

- [x] **Step 2: Refresh packaged CLI**

  ```sh
  mise run package-cli
  ```

  Expected: command passes and refreshes `build/docker/craftless`.

### Task 3: Run Installed Latest-Release Probe

**Files:**
- Evidence root: `/tmp/craftless-packaged-latest-release-probe`

- [x] **Step 1: Start packaged supervisor**

  ```sh
  rm -rf /tmp/craftless-packaged-latest-release-probe/workspace
  build/docker/craftless/bin/craftless server start \
    --port 18083 \
    --workspace /tmp/craftless-packaged-latest-release-probe/workspace
  ```

  Expected: server prints JSON with `url` set to `http://127.0.0.1:18083`.

- [x] **Step 2: Request latest-release Fabric client**

  ```sh
  CRAFTLESS_HTTP_REQUEST_TIMEOUT_MS=900000 \
  build/docker/craftless/bin/craftless clients create latest-cli \
    --api http://127.0.0.1:18083 \
    --version latest-release \
    --loader fabric \
    --offline-name LatestCli
  ```

  Expected: either a runnable client result or a clear packaged product error.
  Save stdout/stderr to
  `/tmp/craftless-packaged-latest-release-probe/artifacts/clients-create-latest-release.log`.

- [x] **Step 3: Collect result-specific artifacts**

  If the client is created and attached, run:

  ```sh
  build/docker/craftless/bin/craftless clients latest-cli openapi --api http://127.0.0.1:18083
  build/docker/craftless/bin/craftless clients latest-cli actions --api http://127.0.0.1:18083
  build/docker/craftless/bin/craftless clients latest-cli resources --api http://127.0.0.1:18083
  build/docker/craftless/bin/craftless clients latest-cli events --api http://127.0.0.1:18083
  ```

  If creation fails, collect prepared cache manifests and logs under the probe
  workspace:

  ```sh
  find /tmp/craftless-packaged-latest-release-probe/workspace -maxdepth 5 -type f \
    | sort
  ```

- [x] **Step 4: Stop managed processes**

  Stop `latest-cli` if it exists, stop the packaged supervisor, and verify:

  ```sh
  ps -axo pid,command | rg -i 'craftless|latest-cli|26\\.2|fabric-loader' | rg -v 'rg -i|exec_command|codex' || true
  ```

  Expected: no managed probe processes remain.

### Task 4: Evidence, Docs, And Push

**Files:**
- Modify: `docs/project-completion-checklist.md`
- Modify: `README.md`
- Create: `docs/superpowers/evidence/2026-06-28-installed-latest-release-alias-compatibility-probe.md`

- [x] **Step 1: Record evidence**

  Include live manifest values, package result, supervisor result,
  latest-release create result, generated API evidence if any, unsupported
  blocker if any, and cleanup proof.

- [x] **Step 2: Align README and checklist**

  Ensure README and checklist say latest/current support remains open unless
  this phase produced a runnable attached latest-release lane with generated
  API evidence.

- [x] **Step 3: Run local hygiene**

  ```sh
  git diff --check
  ```

- [ ] **Step 4: Commit and push**

  ```sh
  git add AGENTS.md README.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-143-installed-latest-release-alias-compatibility-probe-design.md docs/superpowers/plans/2026-06-28-143-installed-latest-release-alias-compatibility-probe-plan.md docs/superpowers/evidence/2026-06-28-installed-latest-release-alias-compatibility-probe.md
  git commit -m "docs: record installed latest release probe"
  git push origin main
  ```

## Self-Review

- Spec coverage: live manifest, package, packaged supervisor, packaged
  latest-release create, result-specific evidence, cleanup, docs, and push are
  covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no gameplay action, static gameplay catalog, version-specific public
  route family, or completion claim is added.
