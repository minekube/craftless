# Craftless Project Completion Checklist

This is the active completion board. It is not an archive, design document, or
raw command log. Keep it current enough that the next agent can continue from
the first open row without rereading the whole repository.

Craftless is complete only when every CL gate is `[x]`, the named evidence files
are fresh, local verification passed, the worktree is clean, and `main` is
pushed.

Status legend: `[ ]` open, `[~]` in progress, `[x]` closed with evidence,
`[!]` blocked with exact evidence and the next diagnostic command.

## Rules

- Work on the system: discovery, projection, invocation, streaming, packaging,
  version/runtime resolution, CLI, docs, and verification.
- Do not add static gameplay catalogs, static CLI gameplay trees, scenario
  actions, preloaded inventory, `/give`, creative-mode shortcuts, or direct
  driver calls to pass a gate.
- Use generated per-client OpenAPI as the gameplay authority. `/actions` and
  `/resources` are projections only.
- When a primitive is missing, record `missing-generic-primitive:<id>` and fix
  the generic discovery/projection/invocation/runtime path.
- Put specs in `docs/superpowers/specs/`, plans in
  `docs/superpowers/plans/`, evidence in `docs/superpowers/evidence/`, and
  durable rules in `docs/agent-operating-contract.md` or
  `docs/agent-module-contracts.md`.
- Do not grow `AGENTS.md` files with phase history or task lists.

## Current Truth

| Field | State |
| --- | --- |
| Active gate | CL-07 final honest public gameplay |
| Current result | The packaged final probe creates and connects the client, fetches generated OpenAPI, captures action/resource/RPC/SSE artifacts, sends chat, breaks a block, and proves inventory changed. |
| Current blocker | The public agent collected Dirt, then `recipe.query { craftable: true }` returned no recipes and the probe failed with `missing-generic-primitive:recipe.query.craftable`. |
| Likely system gap | Generic navigation/material acquisition is not usable in the packaged lane. `navigation.*` is discovered but unavailable with `pathfinder-unavailable`, so the agent cannot reliably reach craftable resources such as logs. |
| Next work | Make packaged runtime support generic navigation or equivalent generic material acquisition, without exposing Baritone/mod internals or adding survival scenario actions. |
| Main command | `mise run final-public-gameplay-probe` |
| Latest artifacts | `driver-fabric/build/craftless-final-gameplay/artifacts/` |

## Completion Gates

| Gate | Status | Closure Standard | Evidence |
| --- | --- | --- | --- |
| CL-01 Generated authority | [x] | Public gameplay authority is generated runtime graph/OpenAPI, not static lists. | Phases 171-173. |
| CL-02 Static shortcut guards | [x] | Static gameplay catalog regressions are guarded; transitional Fabric bootstrap cannot become public API authority. | Phase 178. |
| CL-03 Latest/current lane | [x] | Minecraft `26.2` packaged lane completes create, attach, connect, generated OpenAPI, projections, SSE, JSON-RPC, and adaptive CLI invocation. | `docs/superpowers/evidence/2026-06-28-latest-current-generated-primitive-smoke.md` |
| CL-04 Representative older lane | [x] | Minecraft `1.20.6` packaged lane completes the same public product gate set as CL-03. | `docs/superpowers/evidence/2026-06-28-representative-older-product-lane.md` |
| CL-05 External usability | [x] | External users and agents can install, run, inspect, stream, invoke, and debug Craftless without reading source. | `docs/superpowers/evidence/2026-06-28-user-facing-usability-docs.md` |
| CL-06 Release-quality local gates | [x] | Local release-quality gates pass after CL-05 is closed. | `docs/superpowers/evidence/2026-06-28-final-local-release-gates.md` |
| CL-07 Final public gameplay | [~] | Honest survival gameplay succeeds through public generated API/CLI only, with server provisioning disabled. | `docs/superpowers/evidence/2026-06-28-final-public-gameplay.md` |
| CL-08 Publish completed state | [ ] | Final state is clean, committed, pushed to `main`, and indexed. | `docs/superpowers/evidence/2026-06-28-final-completion.md` |

## Active Task Board

Only this table drives the next work. Closed rows here are local progress, not
gate closure. CL-07 closes only after the final evidence file proves the full
run.

| Step | Status | Done When | Evidence Or Command |
| --- | --- | --- | --- |
| 1. CL-07 spec/plan | [x] | The final gameplay design and implementation plan exist. | `docs/superpowers/specs/2026-06-28-187-final-public-gameplay-design.md`, `docs/superpowers/plans/2026-06-28-187-final-public-gameplay-plan.md` |
| 2. No-shortcut guard | [x] | Distribution tests reject provisioning, `/give`, `task.*`, and scenario actions such as `find.tree`, `craft.sword`, or `kill.cow`. | `mise exec -- bun test playwright/src/distribution.test.ts` |
| 3. Provisioning disabled | [x] | CL-07 does not preload inventory or server-give items. | `CRAFTLESS_DISABLE_SMOKE_PROVISIONING=1`; focused Fabric provisioning test passed. |
| 4. Public probe harness | [x] | `scripts/final-public-gameplay-probe.sh` uses packaged `craftless`, generated OpenAPI, JSON-RPC invoke, SSE/subscription artifacts, and no direct driver calls. | `bash -n scripts/final-public-gameplay-probe.sh` |
| 5. Task/runtime wiring | [x] | `.mise.toml` packages the CLI and runs the Fabric smoke with CL-07 provisioning disabled. | `tasks.final-public-gameplay-probe`; do not re-add `CRAFTLESS_FABRIC_CLIENT_SMOKE=1`. |
| 6. Static task removal | [x] | `task.*` is absent from the Fabric public graph and generated OpenAPI. | Focused Fabric graph and protocol OpenAPI tests passed. |
| 7. Packaged client lifecycle | [x] | Packaged daemon can create, connect, stream, and stop the CL-07 client. | `clients-create.log`, `clients-connect.log`, `client-events-stream.sse`, `client-stop.log` in latest artifacts. |
| 8. Public state transitions | [x] | Probe proves chat, generated OpenAPI fetch, block break, and inventory delta through public API artifacts. | `public-agent-actions.jsonl`, `public-agent-state.jsonl`, server log. |
| 9. Generic navigation/material acquisition | [!] | Agent can reach or otherwise discover craftable material through generic public primitives, not a scenario shortcut. | Current blocker: `navigation.*` is `pathfinder-unavailable`; `recipe.query` returned `count:0` after Dirt. Next: fix packaged runtime support for generic navigation/material discovery. |
| 10. Craft/equip proof | [ ] | Probe crafts and equips an item, then proves inventory/selected-slot state changed through public API. | Rerun `mise run final-public-gameplay-probe` after Step 9. |
| 11. World/entity proof | [ ] | Probe mines or places a block and interacts with or attacks an entity, with observed public state/log evidence. | Final probe artifacts. |
| 12. CL-07 evidence file | [ ] | Evidence summarizes commands, artifacts, public state transitions, missing-primitive fixes, and negative shortcut proof. | Write `docs/superpowers/evidence/2026-06-28-final-public-gameplay.md`. |
| 13. Phase/checklist sync | [ ] | Checklist and phase index reflect the final CL-07 result. | Update this file and `docs/superpowers/phase-index.md`. |
| 14. Publish | [ ] | Work is committed and pushed; worktree is clean. | `git status --short --branch`, `git commit`, `git push origin main`. |

## Next Diagnostic Commands

Inspect the current CL-07 failure:

```sh
cat driver-fabric/build/craftless-final-gameplay/artifacts/smoke-action.log
tail -20 driver-fabric/build/craftless-final-gameplay/artifacts/public-agent-actions.jsonl
mise exec -- bun --eval 'const fs=await import("node:fs/promises"); const a=JSON.parse(await fs.readFile("driver-fabric/build/craftless-final-gameplay/artifacts/client-actions.json","utf8")); console.log(a.filter(x=>x.id.startsWith("navigation.")).map(x=>({id:x.id,availability:x.availability,reason:x.availabilityReason})));'
```

Check whether the packaged lane carries the private navigation runtime needed
by the discovered `navigation.*` actions:

```sh
jar tf build/docker/craftless/mods/fabric-1.20.6/craftless-driver-fabric.jar | rg 'baritone|pathfinder|nether|META-INF/jars' || true
cat build/docker/craftless/driver-mods.json
```

After a system fix, rerun the focused guard set:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
mise exec -- gradle :driver-fabric:test --tests '*FabricNavigationDiscoveryTest*' --tests '*FabricDriverModuleTest.fabric client smoke can disable default server item provisioning*'
mise exec -- gradle :protocol:test --tests '*OpenApiGenerationTest*'
git diff --check
```

Then rerun the final public gameplay probe:

```sh
mise run final-public-gameplay-probe
```

## CL-07 Acceptance Contract

The final replay must use public generated API/CLI only. It must not use
creative inventory, `/give`, preloaded inventory, human movement, hard-coded
survival scenario actions, or direct in-process test calls.

Required positive proof:

- Create or attach a real Craftless-controlled client.
- Fetch generated per-client OpenAPI and use it as authority.
- Capture action/resource projections.
- Capture SSE or JSON-RPC subscription artifacts.
- Send chat.
- Observe player, world, entity, and inventory state.
- Collect a resource and prove the inventory change.
- Craft and equip an item and prove inventory/selected-slot state changed.
- Mine or place a block and prove world state changed.
- Interact with or attack an entity and prove public state or server log.
- Record server log.
- Write final artifacts under
  `driver-fabric/build/craftless-final-gameplay/artifacts/`.

Required negative proof:

- No `/give`, creative inventory, preloaded inventory, direct driver calls,
  human movement, server provisioning, `task.*`, `task.survival`, `kill.cow`,
  `find.tree`, `craft.sword`, or other scenario shortcut appears in the final
  probe path.
- `/clients/{id}/actions` and `/clients/{id}/resources` remain projections.
  The authority for gameplay selection is `GET /clients/{id}/openapi.json`.

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
- CL-05:
  `docs/superpowers/evidence/2026-06-28-user-facing-usability-docs.md`.
- CL-06:
  `docs/superpowers/evidence/2026-06-28-final-local-release-gates.md`.
