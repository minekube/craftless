# User-Facing Usability Docs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close CL-05 by proving external users and agents can install, run, inspect, stream, invoke, and debug Craftless from packaged surfaces and current docs.

**Architecture:** Keep product behavior unchanged except for CLI help dispatch. Add focused tests for group help, refresh stale docs to match CL-03/CL-04, and record real installer/Docker/CLI evidence in an evidence file before closing the checklist gate.

**Tech Stack:** Kotlin/JVM CLI, JUnit, Bash, Docker runtime image, install script, Bun docs guards through `mise exec -- bun`.

---

### Task 1: CLI Help Guard

**Files:**
- Modify: `cli/src/test/kotlin/com/minekube/craftless/cli/CraftlessCliTest.kt`
- Modify: `cli/src/main/kotlin/com/minekube/craftless/cli/Main.kt`

- [x] **Step 1: Add failing clients help test**

Add a test that runs `CraftlessCli.run(listOf("clients", "--help"), ...)` and
expects exit code `0`, output containing `Usage: craftless clients <command>`,
`clients create`, `clients <id> run <action>`, and generated OpenAPI wording,
with empty stderr.

- [x] **Step 2: Verify red**

Run:

```sh
mise exec -- gradle :cli:test --tests '*CraftlessCliTest.clients help prints stable and adaptive command guidance*'
```

Expected before implementation: fail because `clients --help` returns unknown
command.

- [x] **Step 3: Implement help dispatch**

Add non-network help handling for `clients --help`, `cache --help`,
`runtimes --help`, and `server --help`. Keep gameplay help generic and
adaptive; do not list static gameplay aliases.

- [x] **Step 4: Verify green**

Run:

```sh
mise exec -- gradle :cli:test --tests '*CraftlessCliTest.clients help prints stable and adaptive command guidance*'
```

Expected: pass.

### Task 2: User-Facing Docs Guard And Refresh

**Files:**
- Modify: `playwright/src/distribution.test.ts`
- Modify: `README.md`
- Modify: `docs/roadmap.md`
- Verify: `.agents/skills/craftless-public-gameplay-agent/SKILL.md`

- [x] **Step 1: Add failing docs freshness guard**

Extend the distribution test to reject stale README wording:

```ts
expect(readme).not.toContain("gameplay actions still empty");
expect(readme).not.toContain("final completion still requires a refreshed run after latest/current compatibility work");
expect(readme).toContain("Latest/current `26.2` and representative older `1.20.6` packaged lanes are verified");
```

- [x] **Step 2: Verify red**

Run:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
```

Expected before README refresh: fail on stale README wording.

- [x] **Step 3: Refresh docs**

Update README and roadmap to describe CL-03/CL-04 as closed packaged product
lanes, CL-05 as active, and generated OpenAPI/SSE/JSON-RPC plus adaptive CLI
as the user workflow.

- [x] **Step 4: Verify green**

Run:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
```

Expected: pass.

### Task 3: Run User Smokes And Evidence

**Files:**
- Create: `docs/superpowers/evidence/2026-06-28-user-facing-usability-docs.md`
- Modify: `docs/project-completion-checklist.md`
- Modify: `docs/superpowers/phase-index.md`

- [x] **Step 1: Package CLI**

Run:

```sh
mise run package-cli
```

- [x] **Step 2: Install smoke**

Run the install script into a temporary install directory using the current
published release and verify the installed binary returns `ok=true` for:

```sh
craftless server start --once --port 0 --workspace <tmp-workspace>
```

- [x] **Step 3: Docker smoke**

Run:

```sh
docker build -t craftless:cl05 .
docker run --rm craftless:cl05 /opt/craftless/bin/craftless server start --once --port 0 --workspace /tmp/craftless
```

Expected: JSON with `ok=true`. If Docker is unavailable, fix the local Docker
runtime if possible; otherwise do not close CL-05.

- [x] **Step 4: CLI help smoke**

Run packaged CLI help commands:

```sh
build/docker/craftless/bin/craftless --help
build/docker/craftless/bin/craftless clients --help
build/docker/craftless/bin/craftless clients sample run --help
```

Record expected non-network help for the first two and generated-action help
failure without a running daemon for the third.

- [x] **Step 5: Record evidence and close CL-05**

Write the evidence file with commands, outputs, and docs grep results. Mark
CL-05 `[x]` only after install and Docker smokes pass.

### Task 4: Final Verification, Commit, Push

- [x] Run:

```sh
mise exec -- gradle :cli:test --tests '*CraftlessCliTest.clients help prints stable and adaptive command guidance*'
mise exec -- bun test playwright/src/distribution.test.ts
git diff --check
```

- [x] Commit and push directly to `main`.

## Self-Review

- Spec coverage: covers README, roadmap, install, Docker, reusable Action docs
  through README, adaptive CLI help, agent skill docs, evidence, checklist, and
  phase index.
- Placeholder scan: no TBD or fill-in placeholders.
- Type consistency: command names and evidence paths match the active
  checklist CL-05 gate.
