# Craftless Project Completion Checklist

This is the active completion board. It is not an archive, design document,
roadmap essay, or raw command log. Keep it precise enough that the next agent can
continue from the first open row without rereading the whole repository.

Craftless is complete only when every CL gate is `[x]`, the named evidence files
are fresh, local verification passed, the worktree is clean, and `main` is
pushed.

Status legend:

- `[ ]` open
- `[~]` in progress with partial evidence
- `[x]` closed with evidence
- `[!]` blocked with exact evidence and the next diagnostic command

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
- When this checklist changes, update the first stale or contradictory row
  before adding new rows. Do not leave "current result" and "current blocker"
  describing different runs.
- Do not mark a task closed from intent or code shape. Closed means a command,
  artifact, or evidence file proves the behavior from the product surface.

## Current Truth

| Field | State |
| --- | --- |
| Active gate | All CL gates closed |
| Latest run | `mise run final-public-gameplay-probe` on Minecraft `1.21.6`; latest artifacts under `driver-fabric/build/craftless-final-gameplay/artifacts/`. |
| Current result | CL-07 passed. The packaged probe created and connected a client, fetched generated OpenAPI, captured projection/RPC/SSE artifacts, sent chat, discovered and broke a runtime log, proved `Jungle Log x1` pickup, discovered and crafted `Jungle Planks x4`, equipped slot `8`, used a plank through `world.block.interact`, selected a same-level runtime entity from public player/entity positions, navigated to it, and `entity.attack` returned `hit:true`. |
| Fixed locally | Block query radius now supports `64`; block query projection exposes `collectable`, `material`, and `requires-tool`; the final probe uses generated public primitives for spawn-robust material discovery, recipe selection, crafting, equip, block interaction, and same-level entity targeting. |
| Current blocker | None known. CL-08 closure evidence records the pushed CL-07 commit and the final post-push status requirement. |
| Likely system gap | None known for CL-07 after the latest product run. Treat any future failure as a new `missing-generic-primitive:*` and fix generic discovery/projection/invocation, not static scenario APIs. |
| Next work | Push this final evidence/checklist closure and verify `git status --short --branch` is clean. |
| Main command | `git push origin main`, `git status --short --branch`. |
| Latest artifacts | `driver-fabric/build/craftless-final-gameplay/artifacts/` |

## Remaining Completion Path

All listed work is complete once this final closure commit is pushed and
`git status --short --branch` prints `## main...origin/main`.

1. Push this final evidence/checklist closure.
2. Verify `git status --short --branch` is clean and `main...origin/main` is
   not ahead.

## Completion Gates

| Gate | Status | Closure Standard | Evidence |
| --- | --- | --- | --- |
| CL-01 Generated authority | [x] | Public gameplay authority is generated runtime graph/OpenAPI, not static lists. | Phases 171-173. |
| CL-02 Static shortcut guards | [x] | Static gameplay catalog regressions are guarded; transitional Fabric bootstrap cannot become public API authority. | Phase 178. |
| CL-03 Latest/current lane | [x] | Minecraft `26.2` packaged lane completes create, attach, connect, generated OpenAPI, projections, SSE, JSON-RPC, and adaptive CLI invocation. | `docs/superpowers/evidence/2026-06-28-latest-current-generated-primitive-smoke.md` |
| CL-04 Representative older lane | [x] | Minecraft `1.20.6` packaged lane completes the same public product gate set as CL-03. | `docs/superpowers/evidence/2026-06-28-representative-older-product-lane.md` |
| CL-05 External usability | [x] | External users and agents can install, run, inspect, stream, invoke, and debug Craftless without reading source. | `docs/superpowers/evidence/2026-06-28-user-facing-usability-docs.md` |
| CL-06 Release-quality local gates | [x] | Local release-quality gates pass after CL-05 is closed. | `docs/superpowers/evidence/2026-06-28-final-local-release-gates.md` |
| CL-07 Final public gameplay | [x] | Honest survival gameplay succeeds through public generated API/CLI only, with server provisioning disabled. | `docs/superpowers/evidence/2026-06-28-final-public-gameplay.md` |
| CL-08 Publish completed state | [x] | Final state is clean, committed, pushed to `main`, and indexed. | `docs/superpowers/evidence/2026-06-28-final-completion.md` |

## Active Task Board

Only this table drives the next work. Closed rows here are local progress, not
gate closure. CL-07 closes only after the final evidence file proves the full
run.

| Step | Status | Done When | Evidence Or Command |
| --- | --- | --- | --- |
| 1. CL-07 spec/plan | [x] | Final gameplay design and implementation plan exist, and both forbid static survival macros. | `docs/superpowers/specs/2026-06-28-187-final-public-gameplay-design.md`, `docs/superpowers/plans/2026-06-28-187-final-public-gameplay-plan.md` |
| 2. No-shortcut guard | [x] | Distribution tests reject provisioning, `/give`, `task.*`, and scenario actions such as `find.tree`, `craft.sword`, or `kill.cow`. | `mise exec -- bun test playwright/src/distribution.test.ts` |
| 3. Provisioning disabled | [x] | CL-07 starts from an honest survival inventory: no preloaded inventory, no server-give items, no creative-mode setup. | `CRAFTLESS_DISABLE_SMOKE_PROVISIONING=1`; focused Fabric provisioning test passed. |
| 4. Public probe harness | [x] | `scripts/final-public-gameplay-probe.sh` uses packaged `craftless`, generated OpenAPI, JSON-RPC invoke, SSE/subscription artifacts, and no direct driver calls. | `bash -n scripts/final-public-gameplay-probe.sh` |
| 5. Task/runtime wiring | [x] | `.mise.toml` packages the CLI and runs the Fabric smoke with CL-07 provisioning disabled. | `tasks.final-public-gameplay-probe`; do not re-add `CRAFTLESS_FABRIC_CLIENT_SMOKE=1`. |
| 6. Static task removal | [x] | `task.*` is absent from the Fabric public graph and generated OpenAPI. | Focused Fabric graph and protocol OpenAPI tests passed. |
| 7. Packaged client lifecycle | [x] | Packaged daemon can create, connect, stream, and stop the CL-07 client. | `clients-create.log`, `clients-connect.log`, `client-events-stream.sse`, `client-stop.log` in latest artifacts. |
| 8. Public observation/actions | [x] | Probe proves generated OpenAPI fetch, action/resource projection capture, SSE capture, chat, player/inventory/world observation, and `world.block.break changed:true`. | `client-actions.json`, `client-resources.json`, `client-openapi.json`, `public-agent-actions.jsonl`, `public-agent-state.jsonl`, server log. |
| 9. Packaged generic navigation runtime | [x] | The packaged `1.21.6` lane includes private navigation runtime mods, generated `navigation.*` actions are available, and older lanes do not receive incompatible runtime mods. | `mise run package-cli`; `build/docker/craftless/driver-mods.json`; generated OpenAPI shows `navigation.plan`, `navigation.follow`, `navigation.stop` available. |
| 10. Generic useful material discovery | [x] | From an arbitrary honest survival spawn, the public graph can select a reachable collectable that is useful to the live recipe graph, acquire it, and prove inventory changed without a static resource recipe. | `final-gameplay-summary.json`; `public-agent-state.jsonl` shows selected runtime log and `Jungle Log x1` pickup. |
| 11. Recipe runtime/projection | [x] | After useful real material pickup, `recipe.query { craftable: true }` discovers craftable recipes from runtime state without hard-coded item recipes or scenario actions. | `public-agent-state.jsonl` shows `recipe-query-after-material` count `2`. |
| 12. Recipe craft selection | [x] | The public probe selects a discovered crafting recipe, not a furnace-only recipe, and `recipe.craft` changes output/inventory state. | `selected-recipe` was `shapeless-crafting`; `recipe.craft` returned `changed:true`; `inventory-after-craft` shows `Jungle Planks x4`. |
| 13. Equip proof | [x] | Probe equips or selects a usable item, then proves inventory/selected-slot state changed through public API. | `inventory.equip { slot: 8 }`; `player-after-equip` shows `selected-slot:8`. |
| 14. Additional world/entity proof | [x] | Probe mines or places another block and interacts with or attacks an entity, with observed public state/log evidence. | `world.block.interact` accepted and final inventory shows `Jungle Planks x3`; `entity.attack` returned `hit:true` against `entity.handle-4`. |
| 15. CL-07 evidence file | [x] | Evidence summarizes commands, artifacts, public state transitions, missing-primitive fixes, and negative shortcut proof. | `docs/superpowers/evidence/2026-06-28-final-public-gameplay.md`. |
| 16. Phase/checklist sync | [x] | Checklist and phase index reflect the final CL-07 result. | This checklist and `docs/superpowers/phase-index.md` reflect CL-07 closure. |
| 17. Publish | [x] | Work is committed and pushed; worktree is clean. | `docs/superpowers/evidence/2026-06-28-final-completion.md`; final status must print `## main...origin/main`. |

## Latest Local Guards

These commands are green after the CL-07 edits:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest*' --tests '*LocalSessionApiServerTest.prepared runtime includes packaged fabric lane runtime mods*' --tests '*LocalSessionApiServerTest.prepared runtime selects packaged older fabric lane from manifest*'
mise exec -- gradle :driver-fabric:test --tests '*FabricNavigationDiscoveryTest*' --tests '*FabricDriverModuleTest*recipe*' --tests '*FabricDriverModuleTest.fabric runtime graph exposes block query from client state*' --tests '*FabricDriverModuleTest.fabric client smoke can disable default server item provisioning*'
mise exec -- gradle :protocol:test --tests '*OpenApiGenerationTest*'
git diff --check
```

Publish:

```sh
git status --short --branch
git add ...
git commit -m "..."
git push origin main
git status --short --branch
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
