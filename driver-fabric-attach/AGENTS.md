# Fabric Attach Module Instructions

This file is intentionally compact. Keep only Fabric-attach rules that future
agents must not miss; put growing detail in the module contract.

Read root `AGENTS.md`, then read:

- `docs/agent-operating-contract.md`
- `docs/agent-module-contracts.md#driver-fabric-attach`

When fabric-attach-specific instructions need to change, update
`docs/agent-module-contracts.md#driver-fabric-attach`, not this file.

## Do Not Miss

- `driver-fabric-attach/` owns version-neutral Fabric self-attach and Ktor
  loopback transport shared by Fabric driver lanes.
- Keep it free of Minecraft, Fabric API, Yarn, intermediary, and
  official-mapping implementation calls.
- Do not add gameplay bindings, action descriptors, runtime graph operation
  catalogs, per-version route trees, scenario shortcuts, or CLI behavior here.
- Route shapes stay tied to the stable driver session contract and generic
  invocation.
