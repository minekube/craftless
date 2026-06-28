# Packaged Live Attach And Cold-Cache Usability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:systematic-debugging for smoke failures and superpowers:test-driven-development for code fixes.

**Goal:** Make the packaged CLI path survive real cold-cache client creation and prove live Fabric self-attach through public API/CLI.

**Architecture:** Keep the CLI on Ktor Client with an explicit timeout policy. Keep daemon cache integrity behavior intact, but download immutable Minecraft asset objects with bounded coroutine parallelism. Verify through packaged binaries rather than Gradle-only dev launches.

**Tech Stack:** Kotlin/JVM, Ktor Client/Server, kotlinx.coroutines, Gradle, mise.

---

### Task 1: Reproduce Packaged Timeout

- [x] Start packaged `craftless server start` with
  `CRAFTLESS_FABRIC_DRIVER_MOD=build/docker/craftless/mods/craftless-driver-fabric.jar`.
- [x] Run packaged `craftless clients create attach-smoke --version 1.21.6 --loader fabric --offline-name AttachSmoke`.
- [x] Confirm the pre-fix CLI times out while cache files continue increasing.

### Task 2: Add CLI Timeout Guard

- [x] Add a focused CLI test proving `CRAFTLESS_HTTP_REQUEST_TIMEOUT_MS` is
  honored.
- [x] Use one Ktor `HttpClient(CIO)` helper for CLI API calls with
  `HttpTimeout`.
- [x] Set the default request timeout to 15 minutes for real client creation
  and cold-cache flows.

### Task 3: Add Parallel Asset Fetch Guard

- [x] Add a focused daemon test proving asset object fetches overlap.
- [x] Add bounded coroutine parallelism only for independent asset object
  downloads.
- [x] Preserve existing `writeFetchedBytesArtifact` checksum and resume logic.

### Task 4: Verify Packaged Live Attach

- [x] Run focused tests:
  ```sh
  mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.CachePreparationServiceTest.cache preparation fetches independent asset objects concurrently' :cli:test --tests 'com.minekube.craftless.cli.CraftlessCliTest.client create uses configured api request timeout'
  ```
- [x] Run package smoke:
  ```sh
  mise run package-cli
  ```
- [x] Start packaged server with staged Fabric driver mod.
- [x] Create a real Fabric client through packaged CLI.
- [x] Observe `client.attached`.
- [x] Fetch generated actions/resources and SSE events through packaged CLI/API.
- [x] Stop the client/server and remove unrelated stale Minecraft test
  processes.

### Task 5: Document, Commit, Push

- [x] Update `AGENTS.md`.
- [x] Update `docs/project-completion-checklist.md`.
- [x] Record evidence.
- [ ] Run final focused gates and diff check.
- [ ] Commit and push directly to `main`.

## Self-Review

- Scope: distribution/runtime usability and live attach evidence only.
- Static gameplay scan: no new public gameplay action, route family, CLI
  gameplay catalog, Fabric binding, or scenario shortcut.
- HTTP stack: Ktor only.
