# Fabric Discovery Module Instructions

`driver-fabric-discovery/` owns shared Fabric Loader/runtime discovery code
that is reusable across current, older, latest/current, and future Fabric
driver lanes.

## Scope

- Fabric Loader identity and installed-mod discovery.
- Deterministic runtime metadata fingerprints.
- Shared Fabric runtime metadata snapshot/provider helpers.
- Shared Fabric runtime metadata resource projection for the runtime graph.
- Shared protocol-level Fabric runtime graph fragments and graph composition.
- Shared non-gameplay registry resource and handle projection from
  lane-provided registry fingerprints.
- Shared non-gameplay event resource and event projection from lane-provided
  Fabric event-source evidence.
- Shared non-gameplay client-state resource and handle projection from
  lane-provided client-state snapshots.

## Rules

- Do not add gameplay actions, action descriptors, scenario shortcuts, CLI
  behavior, public route families, or version-specific public APIs here.
- Keep this module free of Yarn, intermediary, official-mapping, and Minecraft
  game-class calls. Lane modules may pass lane-specific mappings fingerprints,
  registry probes, server-feature probes, and execution adapters into shared
  metadata helpers.
- Prefer generic Fabric Loader data and Craftless-owned metadata over
  per-version constants. Per-version divergence belongs in the lane adapter
  that calls this module.
- Shared runtime graph projection may compose protocol-level resource,
  operation, handle, and event nodes passed in by lanes, but this module must
  not discover Minecraft game classes or mint gameplay action catalogs itself.
  Lane modules must provide any lane-specific source evidence and keep
  Minecraft game-class registry/server/action discovery outside this module.
- Registry projection in this module may expose Craftless-owned registry
  resources/handles and availability from lane-provided fingerprints. It must
  not inspect Minecraft registries itself, hard-code mod content, or imply a
  supported gameplay lane.
- Event projection in this module may expose Craftless-owned event resources
  and event nodes from lane-provided source evidence. It must not register
  Fabric API callbacks, define mixins, stream SSE by itself, or imply a
  supported gameplay lane.
- Client-state projection in this module may expose Craftless-owned resources
  and handles such as client, player, inventory, recipe, world, entity, screen,
  inventory slots, recipe handles, world block handles, and entity handles from
  lane-provided booleans. It must not inspect Minecraft client/game classes,
  call Fabric APIs, define accessors, execute actions, or imply a supported
  gameplay lane.
- Keep graph composition version-agnostic. If current, older, latest/current,
  or future Fabric versions diverge, model the difference as lane-provided
  metadata, evidence, availability, or a narrow adapter before adding
  per-version code.
- This module should grow when two lanes need the same system behavior. Prefer
  shared graph fragments, metadata providers, projection helpers, fingerprint
  helpers, and schema helpers here over parallel copies in lane modules. Keep
  actual Minecraft game-class calls and gameplay execution out of this module.
- Do not depend on `driver-fabric`, `driver-fabric-official`, `daemon`, or
  `cli`.

## Verification

```sh
mise exec -- gradle :driver-fabric-discovery:test
```
