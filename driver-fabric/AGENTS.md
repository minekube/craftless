# Fabric Driver Module Instructions

This file is intentionally compact. Keep only Fabric-driver rules that future
agents must not miss; put growing detail in the module contract.

Read root `AGENTS.md`, then read:

- `docs/agent-operating-contract.md`
- `docs/agent-module-contracts.md#driver-fabric`

When driver-fabric-specific instructions need to change, update
`docs/agent-module-contracts.md#driver-fabric`, not this file.

## Do Not Miss

- `driver-fabric/` owns the Fabric/Loom in-client driver lane, client-thread
  gateway, Mixins/accessors, and real Fabric-backed discovery/projection/
  invocation.
- Shared version-agnostic discovery, projection, invocation, event, attach,
  schema, and OpenAPI plumbing is the default.
- Add per-version code only for proven Minecraft, Fabric API, mapping, loader,
  or bytecode-signature divergence; isolate only the diverging adapter,
  accessor, Mixin, or provider behind a lane boundary.
- Do not expose Fabric/Yarn/intermediary/raw Minecraft names publicly.
- Do not add public gameplay by hand-writing a descriptor plus binding or by
  registering placeholder descriptors. Improve generic runtime graph discovery
  and projection first.
- Keep Minecraft calls on the client thread.
