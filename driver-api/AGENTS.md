# Driver API Module Instructions

`driver-api/` owns the stable JVM contract between daemon/runtime code and any
in-client automation implementation.
It is the long-lived boundary for all supported Minecraft/Fabric versions; keep
version selection and divergence as data, metadata, descriptors, availability,
and backend adapter behavior rather than stable API forks.

## Scope

- `DriverSession` and stable driver-facing DTOs.
- Action descriptors and invocation results.
- Runtime metadata and capability graph handoff points.

## Rules

- Keep the public driver contract small and descriptor-driven: runtime metadata,
  action discovery, generic action invocation, events, session state, and
  lifecycle.
- Do not grow one stable Kotlin method per Minecraft action as the public API.
  Internal convenience methods are acceptable only when they do not leak into
  daemon routes, CLI commands, or public docs as the action model.
- Action IDs and DTOs must be Craftless-owned.
- Preserve typed `JsonElement` action args.
- Keep the contract Minecraft-version-neutral. Version-specific divergence
  belongs in driver implementations and compatibility lanes, not in stable API
  methods, enums, or DTO forks.
- Do not add stable API methods, DTO variants, or enums for one Minecraft
  release, loader lane, or Fabric API generation. The stable API should carry
  discovered data, runtime metadata, action descriptors, availability reasons,
  and generic invocation payloads.
- Do not add Craftless-owned enums for foreign concepts that already have
  standard protocol forms or should remain data from the runtime graph.
- Keep lifecycle primitives distinct from gameplay breadth. Connect, stop,
  snapshot, metadata, events, action discovery, graph snapshot, and generic
  invoke may stay stable; chat, raycast, inventory, block, entity, recipe,
  navigation, and similar affordances must arrive through discovered actions,
  resources, handles, and schemas.

## Verification

```sh
mise exec -- gradle :driver-api:test
```
