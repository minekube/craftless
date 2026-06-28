# Craftless Project Completion Checklist

This is the active completion board. It is not a phase archive, design doc, or
dumping ground for every task ever attempted.

Craftless is complete only when every CL gate below is `[x]`, the named
evidence files are fresh, local verification passed, the worktree is clean, and
`main` is pushed.

Status legend: `[ ]` open, `[~]` in progress, `[x]` closed with evidence, `[!]`
blocked with an exact blocker and next command.

## Operating Rules

- Work top-down from **Current Execution Packet**.
- Put specs in `docs/superpowers/specs/`.
- Put implementation plans in `docs/superpowers/plans/`.
- Put command transcripts and artifact summaries in
  `docs/superpowers/evidence/`.
- Put phase index entries in `docs/superpowers/phase-index.md`.
- Put durable rules in `docs/agent-operating-contract.md` or
  `docs/agent-module-contracts.md`.
- Do not append phase history or raw logs to this file.
- Do not close a gate from compile-only output, old evidence, remote CI
  waiting, hand-maintained gameplay catalogs, or scenario shortcuts.

## Gate Board

| Gate | Status | Done Means | Evidence |
| --- | --- | --- | --- |
| CL-01 | [x] | Public gameplay authority is generated runtime graph/OpenAPI, not `/actions` or static lists. | Phases 171-173. |
| CL-02 | [x] | Static gameplay catalog regressions are guarded; transitional Fabric bootstrap cannot become public API authority. | Phase 178. |
| CL-03 | [x] | Latest/current packaged lane creates, attaches, connects, captures generated API/stream artifacts, and invokes a generated operation through JSON-RPC plus adaptive CLI for Minecraft `26.2`. | `docs/superpowers/evidence/2026-06-28-latest-current-generated-primitive-smoke.md` |
| CL-04 | [x] | Representative older packaged lane passes the same public product gate set as CL-03 for Minecraft `1.20.6`. | `docs/superpowers/evidence/2026-06-28-representative-older-product-lane.md` |
| CL-05 | [~] | External users and agents can install, run, inspect, stream, invoke, and debug Craftless without reading source. | `docs/superpowers/evidence/2026-06-28-user-facing-usability-docs.md` |
| CL-06 | [ ] | Final local release-quality gates pass after CL-01 through CL-05. | `docs/superpowers/evidence/2026-06-28-final-local-release-gates.md` |
| CL-07 | [ ] | Honest survival gameplay is performed through public generated API/CLI only. | `docs/superpowers/evidence/2026-06-28-final-public-gameplay.md` |
| CL-08 | [ ] | Final state is committed, pushed to `main`, and clean. | `docs/superpowers/evidence/2026-06-28-final-completion.md` |

## Current Execution Packet

Close CL-05 next. Do not start CL-06 or CL-07 except for docs-only cleanup that
keeps this board accurate.

1. [ ] Audit README, install script, Dockerfile, reusable GitHub Action, CLI
   help, and agent skill docs against the current product shape.
2. [ ] Remove or rewrite stale docs that imply an active TypeScript SDK,
   previous brand name, `.dev` domain, HMC final-driver path, static gameplay
   SDK, server-cheat completion, or scenario shortcut usage.
3. [ ] Verify install script smoke from a clean temporary install directory.
4. [ ] Verify Docker runtime smoke from the copied packaged artifact image.
5. [ ] Verify adaptive CLI help/examples for generated gameplay invocation.
6. [ ] Write CL-05 evidence:
   `docs/superpowers/evidence/2026-06-28-user-facing-usability-docs.md`.
7. [ ] Mark CL-05 `[x]` only if the evidence proves the external-user and
   agent usability gate below.
8. [ ] Update `docs/superpowers/phase-index.md`, run focused verification,
   commit, and push.

## Closed CL-04 Product Gate Set

The representative older lane must prove the same external product behavior as
CL-03, using Minecraft `1.20.6` as the older compatibility lane.

- [x] Packaged CLI exists before the probe runs: `mise run package-cli`.
- [x] Packaged CLI creates or attaches the client through supervisor API.
- [x] Runtime/cache/Fabric resolution uses shared product services.
- [x] Java selection comes from managed runtime resolution, not random local
  Java installs.
- [x] Fabric Loader/API and Craftless driver artifacts come from the packaged
  manifest/cache path.
- [x] Client connects to a real local server.
- [x] Connected per-client OpenAPI is captured.
- [x] `/clients/{id}/actions` projection is captured as evidence only.
- [x] `/clients/{id}/resources` projection is captured as evidence only.
- [x] SSE event stream is captured.
- [x] JSON-RPC query captures OpenAPI/actions/resources.
- [x] JSON-RPC subscription captures filtered stream output.
- [x] A generated invocable operation is selected from live
  `x-craftless-actions`.
- [x] JSON-RPC `method: "invoke"` invokes that generated operation.
- [x] Adaptive CLI invokes that generated operation.
- [x] Probe uses no `task.*` invocation, no server-provisioned inventory, no
  static gameplay catalog, and no `:driver-fabric:runClient` shortcut.

## Remaining Gates

### CL-05: External User And Agent Usability

- [ ] README explains install script, packaged CLI, Docker runtime image,
  reusable GitHub Action, supervisor OpenAPI, generated per-client OpenAPI,
  adaptive CLI, SSE, JSON-RPC query/subscription, cache behavior, and evidence
  expectations.
- [ ] README and docs contain no active TypeScript SDK positioning, previous
  brand name, `.dev` domain, HMC-as-final-driver wording, static gameplay SDK
  wording, or server-cheat completion wording.
- [ ] CLI help/examples are adaptive where gameplay is involved.
- [ ] Docker runtime smoke proves the image uses a copied packaged Craftless
  artifact instead of building the software at container runtime.
- [ ] Install script smoke proves a fresh user can run the packaged CLI.
- [ ] Agent skill docs teach generated OpenAPI/SSE/JSON-RPC composition,
  missing-primitive reporting, and no scenario shortcuts.

### CL-06: Final Local Release Gates

- [ ] `mise run lint`
- [ ] `mise run architecture-check`
- [ ] `mise run ci`
- [ ] `mise run package-cli`
- [ ] Docker runtime smoke
- [ ] install script smoke
- [ ] latest/current packaged lane probe
- [ ] representative older packaged lane probe
- [ ] `git diff --check`

### CL-07: Final Honest Public Gameplay

The final gameplay replay must use public generated API/CLI only. It must not
use creative inventory, `/give`, preloaded inventory, human movement,
hard-coded survival scenario actions, or direct in-process test calls that
bypass public API/CLI.

- [ ] Create or attach a real Craftless-controlled client.
- [ ] Fetch generated per-client OpenAPI.
- [ ] Capture actions/resources projections.
- [ ] Subscribe to SSE or JSON-RPC subscription stream.
- [ ] Write chat.
- [ ] Observe player/world/entity state.
- [ ] Observe inventory state.
- [ ] Collect a resource.
- [ ] Craft and equip an item.
- [ ] Mine or place a block.
- [ ] Interact with or attack an entity.
- [ ] Pick up or drop an item.
- [ ] Record server log.
- [ ] Write final artifact summary under
  `driver-fabric/build/craftless-final-gameplay/artifacts/`.

### CL-08: Publish Completed State

- [ ] Final evidence names every closed CL gate and command.
- [ ] Checklist, phase index, evidence, README/docs, and code are committed.
- [ ] `git status --short --branch` is clean after commit.
- [ ] `git push origin main` succeeds.

## Final Completion Gate

Completion remains blocked until CL-01 through CL-08 are checked with fresh
evidence. The final record must include runnable support evidence for both the
latest/current lane and the representative older lane under the same public
API/CLI gates.

Historical phase sections do not close the product goal. They are indexed in
`docs/superpowers/phase-index.md` and backed by specs, plans, and evidence
files.

## Current Baseline

Craftless currently has a Kotlin/JVM Ktor supervisor, adaptive JVM CLI,
generated per-client OpenAPI, graph-projected actions/resources, generic
invocation, SSE plus JSON-RPC-style query/control, packaged distribution paths,
and staged Fabric gameplay evidence.

CL-03 is closed for latest/current Minecraft `26.2`. CL-04 is closed for
representative older Minecraft `1.20.6`. CL-05 is the active blocker: prove
that external users and agents can install, run, inspect, stream, invoke, and
debug Craftless from docs and packaged artifacts without reading source.

## Closed Evidence Index

- CL-01:
  `docs/superpowers/evidence/2026-06-28-daemon-openapi-graph-only-authority.md`,
  `docs/superpowers/evidence/2026-06-28-remote-driver-action-graph-authority.md`,
  `docs/superpowers/evidence/2026-06-28-public-agent-actions-projection-optional.md`.
- CL-02:
  `docs/superpowers/evidence/2026-06-28-fabric-execution-adapter-naming.md`,
  `docs/superpowers/evidence/2026-06-28-bootstrap-resource-derivation.md`,
  `docs/superpowers/evidence/2026-06-28-bootstrap-adapter-key-separation.md`,
  `docs/superpowers/evidence/2026-06-28-client-state-operation-discovery.md`,
  `docs/superpowers/evidence/2026-06-28-static-gameplay-guard-closure.md`.
- CL-03:
  `docs/superpowers/evidence/2026-06-28-packaged-official-latest-lane.md`,
  `docs/superpowers/evidence/2026-06-28-official-client-state-world-time-operation.md`,
  `docs/superpowers/evidence/2026-06-28-official-world-time-invocation.md`,
  `docs/superpowers/evidence/2026-06-28-packaged-latest-current-attach-artifacts.md`,
  `docs/superpowers/evidence/2026-06-28-latest-current-generated-primitive-smoke.md`.
- CL-04:
  `docs/superpowers/evidence/2026-06-28-representative-older-product-lane.md`.
