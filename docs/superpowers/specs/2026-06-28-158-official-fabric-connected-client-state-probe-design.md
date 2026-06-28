# Phase 158: Official Fabric Connected Client State Probe Design

## Goal

Prove that the latest/current official Fabric lane can attach to Craftless,
connect to a real local Minecraft server, and project connected client-state
through the shared Fabric runtime graph without adding static gameplay actions
or copying the Yarn/remap gameplay gateway.

## Product Boundary

This phase is compatibility evidence for the 26.x/latest-current lane. It is
not final latest/current support, and it must not add the official driver jar
to `driver-mods.json`.

Allowed behavior:

- add a narrow official-mapped lifecycle connector adapter when official
  Minecraft classes diverge from the Yarn/remap lane;
- call the stable driver `connect(target)` lifecycle primitive;
- observe connected client state through the existing shared
  `FabricClientStateGraphSnapshot` projection;
- extend the opt-in official attach probe so it can start or reuse a local
  server, call the public daemon connect route, fetch per-client OpenAPI, and
  record machine-readable evidence.

Forbidden behavior:

- copying chat, movement, inventory, block, recipe, navigation, or entity
  gameplay bindings from `driver-fabric`;
- adding static gameplay action descriptors, CLI commands, daemon routes, or
  protocol DTOs for the official lane;
- claiming latest/current support before packaging, generated actions/resources,
  SSE, public API/CLI smoke, and honest gameplay evidence pass.

## Architecture

The official lane should keep lifecycle connection as the only new
official-mapped adapter. The adapter may use official/Mojang names such as
`ConnectScreen`, `TitleScreen`, `ServerData`, and `ServerAddress`, but those
names must remain private implementation inputs. `OfficialFabricDriverBackend`
delegates `connect(target)` to the adapter, emits a Craftless-owned lifecycle
result, and continues to compose runtime metadata, registry evidence, event
evidence, and client-state evidence through `driver-fabric-discovery`.

The probe remains test-only and opt-in. It should start the daemon, launch the
official Fabric client, wait for `client.attached`, start a real local server
or use an existing local test-server helper, call the daemon connect API, wait
until the shared client-state graph reports connected resources, then write a
summary of OpenAPI resource/handle/event counts and connected availability.

## Success Criteria

- Focused unit tests prove `OfficialFabricDriverBackend.connect` delegates to a
  narrow connector and reports observed success/failure without changing public
  action shape.
- Architecture tests prove the official lane still does not depend on
  `driver-fabric`, does not package as a supported manifest lane, and does not
  expose public gameplay actions.
- The enabled official attach probe launches the official 26.x client, attaches
  to Craftless, connects to a real local server through public daemon/client
  lifecycle, and records connected client-state resources from generated
  OpenAPI.
- `mise run ci` and `mise run fabric-lane-check-latest-official` remain green.

## Completion Evidence

Evidence belongs in
`docs/superpowers/evidence/2026-06-28-official-fabric-connected-client-state-probe.md`
and should include:

- exact Minecraft version, Fabric Loader version, Java major, and driver id;
- connect target host/port;
- attach status;
- connected resource/handle/event counts;
- availability for client, player, inventory, world, camera, interaction
  manager, recipes, and screen resources;
- explicit statement that actions remain zero until official-lane gameplay
  discovery/invocation is implemented generically.

## Self-Review

- No placeholders remain.
- The spec is a single implementation slice: connected client-state evidence.
- The spec keeps lifecycle connection separate from gameplay breadth.
- The spec forbids static gameplay catalogs and per-version public APIs.
