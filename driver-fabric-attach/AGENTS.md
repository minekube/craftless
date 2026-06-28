# Fabric Attach Module Instructions

`driver-fabric-attach/` owns version-neutral Fabric self-attach and Ktor
loopback transport shared by all Fabric driver lanes.
It is the common attach boundary for current, older, latest/current, and future
Fabric lanes; version differences must arrive as stable session data or be
handled in the lane adapter before the session is passed here.

## Scope

- Attach environment parsing.
- In-client self-attach startup.
- Ktor loopback routes for the stable `DriverSession` contract.
- Daemon attach handoff payloads.

## Rules

- Keep this module free of Minecraft, Fabric API, Yarn, intermediary, and
  official-mapping implementation calls.
- Do not add gameplay bindings, action descriptors, runtime graph operation
  catalogs, per-version route trees, scenario shortcuts, or CLI behavior here.
- Use Ktor Client and Ktor Server only for HTTP transport.
- Keep route shapes tied to the stable driver session contract:
  snapshot, connect, actions, runtime metadata, runtime graph, generic invoke,
  stop, and events.
- If a Fabric lane needs version-specific behavior, keep that behavior in the
  lane adapter and pass a stable `DriverSession` into this module.
- Do not add a new attach server, route family, session implementation, or
  transport DTO just because one Minecraft/Fabric version currently needs a
  workaround. First model the difference as metadata, availability, or a narrow
  lane adapter; this module should remain reusable by every lane.

## Verification

```sh
mise exec -- gradle :driver-fabric-attach:test
```
