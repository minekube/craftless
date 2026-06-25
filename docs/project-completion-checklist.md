# Craftless Project Checklist

This checklist is the active project red line. Keep it short, current, and
honest. Update it whenever implementation or product status changes.

Legend:

- `[ ]` not started
- `[~]` in progress
- `[x]` done with evidence
- `[!]` blocked

## Current Baseline

- [x] Repository is renamed to Craftless and uses `com.minekube.craftless`.
- [x] Tooling is pinned through `mise`.
- [x] JVM HTTP surfaces use Ktor Server/Client.
- [x] Go implementation and removed TypeScript SDK are not active product
  surfaces.
- [x] Stable supervisor OpenAPI exists at `GET /openapi.json`.
- [x] Per-client OpenAPI route exists at `GET /clients/{id}/openapi.json`.
- [x] Generic action invocation exists at `POST /clients/{id}:run`.
- [x] CLI binary is `craftless` and uses adaptive action metadata.
- [x] Fabric smoke has proven real client launch, server join, generated chat,
  generated movement invocation, disconnect, and artifact capture.
- [~] Current Fabric driver has real chat and movement bindings. Broader
  gameplay discovery is not implemented yet and must not be represented as a
  static placeholder catalog.
- [ ] Craftless is complete.

Baseline evidence:

- Latest real-smoke evidence path:
  `driver-fabric/build/craftless-local-server-smoke/artifacts/`
- Latest static-placeholder cleanup smoke: `client-actions.json` contained
  only `player.chat` and `player.move`; `server-evidence.jsonl` contained
  join, chat, and disconnect for the same real client.
- Key commands:
  - `mise run lint`
  - `mise run ci`
  - `CRAFTLESS_FABRIC_CLIENT_SMOKE=1 mise exec -- gradle :driver-fabric:fabricClientSmoke`
- Current known local-only file: `.vscode/` is untracked and unrelated.

## 1. Product Positioning And README

- [ ] Restore the richer README comparison structure from the earlier approved
  direction, updated to current truth.
- [ ] Keep the README comparison focused on Craftless, Mineflayer, and
  Baritone unless another project is useful as clearly labelled evidence.
- [ ] Do not advertise HeadlessMC/HMC-Specifics, Prism Launcher, removed SDKs,
  or bridge internals as active product surfaces.
- [ ] README must clearly separate implemented features from roadmap.
- [ ] README must describe Craftless as live generated OpenAPI over real
  Minecraft Java clients, not a static action SDK.

Verification:

- `git diff --check`
- `rg -n "minekube\\.dev|dev\\.minekube|player/sendChat|/player/sendChat" README.md docs --glob '!docs/superpowers/**' -S`

## 2. Runtime Discovery Architecture

- [x] Remove static placeholder action descriptors from product code and tests.
- [ ] Design the Fabric runtime discovery/projection layer.
- [ ] Define how internal Fabric/Minecraft/mod/registry/server data becomes
  Craftless-owned actions, resources, handles, schemas, availability, and
  events.
- [ ] Define the rule for unavailable-but-detected operations: they may appear
  in OpenAPI only when a runtime probe discovered them and produced a
  machine-readable availability reason.
- [ ] Ensure generated aliases are derived only from the running client's
  OpenAPI/action descriptors.
- [ ] Ensure public OpenAPI does not expose Fabric/Yarn/intermediary names,
  raw Minecraft implementation names, mod package names, commands, or launcher
  internals.
- [ ] Add tests that fail if public descriptors leak implementation names.

Verification:

- `mise exec -- gradle :protocol:test :driver-fabric:test`
- `mise exec -- gradle :protocol:test --tests com.minekube.craftless.protocol.NamespacePolicyTest`
- `mise exec -- gradle :driver-fabric:test --tests com.minekube.craftless.driver.fabric.v1_21_6.FabricDriverModuleTest`

## 3. Fabric Driver Action Bindings

- [x] `player.chat` has a real Fabric binding.
- [x] `player.move` has a real Fabric binding and driver-side event evidence.
- [ ] Real look/perception/block/inventory/screen capabilities are discovered
  from the running client before they are advertised.
- [ ] Each advertised gameplay action has either a real Fabric execution
  binding or probe-backed unavailable metadata.
- [ ] No future gameplay action is added as a hand-written placeholder
  descriptor.
- [ ] `FabricClientGateway` stays generic and does not grow one method per
  gameplay action.

Verification:

- `mise exec -- gradle :driver-fabric:test`
- `CRAFTLESS_FABRIC_CLIENT_SMOKE=1 mise exec -- gradle :driver-fabric:fabricClientSmoke`

## 4. Real Gameplay Vertical Slice

- [ ] Define the first useful end-to-end gameplay slice.
- [ ] Recommended target: obtain/equip an iron sword using real client actions,
  without Minecraft console commands as the public API.
- [ ] The slice uses generated OpenAPI/action metadata as the client contract.
- [ ] The slice discovers the needed actions/resources from the running client;
  it does not call hard-coded Kotlin methods or static CLI commands.
- [ ] The slice runs against a real Fabric client and local server fixture.
- [ ] Evidence proves observable game effects through server logs, client
  telemetry, or both.

Verification:

- `CRAFTLESS_FABRIC_CLIENT_SMOKE=1 mise exec -- gradle :driver-fabric:fabricClientSmoke`
- Add a narrower smoke command when the slice exists.

## 5. Client Runtime And Files

- [x] Craftless-owned instance file layout is modeled.
- [x] Prism Launcher source was cloned under `/tmp/prismlauncher-source` for
  research, outside Minekube repos.
- [x] Prism findings are captured as design input, not a core dependency.
- [ ] Client runtime/file management is strong enough for repeated local and CI
  runs.
- [ ] Public APIs expose Craftless-owned file handles only.

Verification:

- `mise exec -- gradle :protocol:test :daemon:test`

## 6. Quality Gates

- [x] `mise run lint` exists.
- [x] `mise run ci` exists.
- [x] Kotlin lint includes ktlint, detekt, and compiler warnings as errors.
- [x] Bun helper tests run through `mise`.
- [ ] CI and local checks cover the real discovery/action architecture once it
  replaces placeholder descriptors.

Verification:

- `mise run lint`
- `mise run ci`
- `git diff --check`

## Completion Gate

Craftless is complete only when all are true:

- [ ] README and docs match the restored product direction.
- [ ] Per-client OpenAPI is generated from runtime discovery and real bindings,
  not a static placeholder action list.
- [ ] Public API names are Craftless-owned and policy tests enforce that.
- [ ] Advertised player/world/inventory/screen actions have real Fabric
  execution bindings or probe-backed unavailable metadata.
- [ ] A real gameplay vertical slice passes through API/CLI the way a user or
  agent would use it.
- [ ] `mise run lint` passes.
- [ ] `mise run ci` passes.
- [ ] Opt-in Fabric real-client smoke passes and writes evidence artifacts.
- [ ] The completed work is pushed to `main` when requested.
