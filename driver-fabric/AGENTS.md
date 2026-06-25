# Fabric Driver Module Instructions

`driver-fabric/` owns the current Fabric/Loom driver module. Keep
version-specific Minecraft bindings internal to this module where practical.

## Scope

- Fabric client entrypoint and metadata.
- Mixins/accessors and bytecode-sensitive Minecraft glue.
- Client-thread gateway for connect, chat, stop, and generated action
  invocation.
- First real Fabric backend behavior.

## Rules

- Java is appropriate for Mixins, accessors, and exact bytecode signatures.
  Kotlin is appropriate for driver/runtime logic where Fabric classloading is
  proven safe.
- Keep Minecraft calls on the client thread.
- Do not expose Fabric, Yarn, intermediary, or Minecraft implementation names as
  public action IDs, routes, CLI commands, or docs.
- Register only actions that this runtime can actually support. Per-client
  OpenAPI/action descriptors should reflect support checks.
- Prefer internal version-aware bindings and reflection/mapping probes over new
  public Gradle subprojects per Minecraft version.
- Do not depend on the HMC bridge for final Fabric behavior.

## Verification

```sh
mise exec -- gradle :driver-fabric:test
```
