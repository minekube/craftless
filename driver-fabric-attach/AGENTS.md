# Fabric Attach Module Instructions

`driver-fabric-attach/` owns version-neutral Fabric self-attach and Ktor
loopback transport shared by all Fabric driver lanes.

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

## Verification

```sh
mise exec -- gradle :driver-fabric-attach:test
```
